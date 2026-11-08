package es.gob.afirma.standalone.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import es.gob.afirma.core.AOCancelledOperationException
import es.gob.afirma.standalone.configurator.common.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

enum class Step { Home, Document, Placement, Certificate, Signing, Done }

/** Whole UI state. Screens read it; actions run the slow parts off the UI thread. */
class AppState(private val scope: CoroutineScope) {

    var step by mutableStateOf(Step.Home)
    var busy by mutableStateOf<String?>(null)
    var error by mutableStateOf<Pair<String, String>?>(null)

    var doc by mutableStateOf<LoadedDocument?>(null)
    var visibleEnabled by mutableStateOf(PreferencesManager.getBoolean(PreferencesManager.PREFERENCE_PADES_VISIBLE))
    var certificationLevel by mutableStateOf(0)
    var visible by mutableStateOf<VisibleSignature?>(null)

    var certs by mutableStateOf<List<CertOption>?>(null)
    var certsError by mutableStateOf<String?>(null)
    var selectedCert by mutableStateOf<CertOption?>(null)

    var output by mutableStateOf<File?>(null)
    var progress by mutableStateOf("")
    var result by mutableStateOf<SignResult?>(null)

    val steps: List<Pair<Step, String>> get() = buildList {
        add(Step.Document to "Documento")
        if (doc?.isPdf == true && visibleEnabled) add(Step.Placement to "Firma visible")
        add(Step.Certificate to "Certificado")
        add(Step.Done to "Resultado")
    }

    fun openFile(file: File) {
        error = null
        busy = "Analizando ${file.name}"
        scope.launch {
            try {
                val d = withContext(Dispatchers.IO) { Engine.load(file) }
                doc = d
                visible = null
                result = null
                output = Engine.defaultOutput(d)
                step = Step.Document
            } catch (e: Exception) {
                LOGGER.log(Level.WARNING, "No se pudo cargar el fichero", e)
                error = "No se ha podido abrir el fichero" to (e.message ?: e.toString())
            } finally {
                busy = null
            }
        }
    }

    fun continueFromDocument() {
        step = if (doc?.isPdf == true && visibleEnabled) Step.Placement else Step.Certificate
        if (step == Step.Certificate) loadCerts()
    }

    fun confirmPlacement(v: VisibleSignature) {
        visible = v
        step = Step.Certificate
        loadCerts()
    }

    fun loadCerts(force: Boolean = false) {
        if (certs != null && !force) return
        certs = null
        certsError = null
        scope.launch {
            try {
                val list = withContext(Dispatchers.IO) { Engine.certificates(force) }
                certs = list
                if (selectedCert == null || list.none { it.bean.alias == selectedCert?.bean?.alias }) {
                    selectedCert = list.firstOrNull { !it.expired && it.canSign }
                }
            } catch (e: AOCancelledOperationException) {
                certsError = "Se canceló el acceso al almacén de certificados."
            } catch (e: Exception) {
                LOGGER.log(Level.WARNING, "No se pudo cargar el almacen", e)
                certsError = e.message ?: e.toString()
            }
        }
    }

    fun sign() {
        val d = doc ?: return
        val c = selectedCert ?: return
        val out = output ?: Engine.defaultOutput(d)
        step = Step.Signing
        progress = "Preparando la firma"
        scope.launch {
            try {
                // Reload the configuration so a previous attempt never leaks parameters into this one
                val fresh = withContext(Dispatchers.IO) { Engine.load(d.file) }
                doc = fresh
                result = withContext(Dispatchers.IO) {
                    Engine.sign(fresh, c, if (visibleEnabled && fresh.isPdf) visible else null, certificationLevel, out, DialogHost) {
                        progress = it
                    }
                }
                step = Step.Done
            } catch (e: AOCancelledOperationException) {
                step = Step.Certificate
            } catch (e: Exception) {
                LOGGER.log(Level.SEVERE, "Error al firmar", e)
                step = Step.Certificate
                error = "No se ha podido firmar el documento" to (e.message ?: e.toString())
            }
        }
    }

    fun back() {
        step = when (step) {
            Step.Placement -> Step.Document
            Step.Certificate -> if (doc?.isPdf == true && visibleEnabled) Step.Placement else Step.Document
            Step.Document -> Step.Home
            else -> Step.Home
        }
    }

    fun reset() {
        doc = null
        visible = null
        result = null
        error = null
        step = Step.Home
    }

    companion object {
        private val LOGGER: Logger = Logger.getLogger("es.gob.afirma")
    }
}
