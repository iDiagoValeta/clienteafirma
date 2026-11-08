package es.gob.afirma.standalone.compose

import es.gob.afirma.core.CustomRuntimeConfigNeededException
import es.gob.afirma.core.RuntimeConfigNeededException
import es.gob.afirma.core.RuntimePasswordNeededException
import es.gob.afirma.core.keystores.NameCertificateBean
import es.gob.afirma.core.signers.AOSignConstants
import es.gob.afirma.keystores.AOKeyStoreDialog
import es.gob.afirma.keystores.AOKeyStoreManager
import es.gob.afirma.signers.pades.common.PdfExtraParams
import es.gob.afirma.signvalidation.SignValidity
import es.gob.afirma.signvalidation.SignValiderFactory
import es.gob.afirma.signvalidation.ValidatePdfSignature
import es.gob.afirma.standalone.SimpleAfirmaMessages
import es.gob.afirma.standalone.SimpleKeyStoreManager
import es.gob.afirma.standalone.configurator.common.PreferencesManager
import es.gob.afirma.standalone.crypto.CertAnalyzer
import es.gob.afirma.standalone.ui.ComposeBridge
import es.gob.afirma.standalone.ui.SignOperationConfig
import es.gob.afirma.standalone.ui.SignOperationConfig.CryptoOperation
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.image.BufferedImage
import java.io.File
import java.security.KeyStore
import java.security.cert.X509Certificate
import java.util.Properties
import java.util.logging.Logger

private val LOGGER: Logger = Logger.getLogger("es.gob.afirma")

/** User interaction the signing engine may need halfway through (PDF passwords, risk confirmations). */
interface Prompts {
    suspend fun confirm(title: String, text: String): Boolean
    suspend fun password(title: String, text: String): CharArray?
}

/** A loaded document ready to be signed. */
class LoadedDocument(
    val file: File,
    val config: SignOperationConfig,
    val pageCount: Int,
    /** Page sizes in PDF points (width, height). */
    val pageSizes: List<Pair<Float, Float>>,
    val thumbnail: BufferedImage?,
) {
    val isPdf get() = ComposeBridge.isPdf(config)
    val alreadySigned get() = config.cryptoOperation == CryptoOperation.COSIGN
    val sizeBytes get() = file.length()
    val lastModified get() = file.lastModified()
    val formatName: String get() = config.signatureFormatName ?: config.signer?.javaClass?.simpleName ?: ""
}

class CertOption(val bean: NameCertificateBean) {
    val cert: X509Certificate get() = bean.certificate
    val holder: String by lazy {
        runCatching { CertAnalyzer.getCertInformation(cert).holderName }.getOrNull() ?: bean.name
    }
    val issuer: String by lazy {
        runCatching { CertAnalyzer.getCertInformation(cert).issuerName }.getOrNull()
            ?: cnOf(cert.issuerX500Principal.name)
    }
    val notAfter get() = cert.notAfter
    val expired get() = cert.notAfter.before(java.util.Date())
    val notYetValid get() = cert.notBefore.after(java.util.Date())
    val canSign: Boolean get() = cert.keyUsage?.let { it[0] || it[1] } ?: true
}

/** Visible signature as the user drew it: page index (0-based) and rectangle in PDF points. */
data class VisibleSignature(
    val page: Int,
    val llx: Float, val lly: Float, val urx: Float, val ury: Float,
    val text: String,
    val fontSize: Int,
    val fontColor: String,
)

data class SignResult(
    val output: File,
    val signer: X509Certificate,
    val validity: List<SignValidity>,
    val signatureBytes: Int,
)

fun cnOf(dn: String): String =
    dn.split(',').map { it.trim() }.firstOrNull { it.startsWith("CN=", true) }?.substring(3) ?: dn

/** Thin layer over the existing AutoFirma signing logic. No Swing here. */
object Engine {

    @Volatile
    private var ksm: AOKeyStoreManager? = null

    /** Reuses the NSS store opened at startup when it is the configured one: NSS cannot be
     * initialised again later in the process (see SimpleAfirma.main). */
    fun preload(store: AOKeyStoreManager?) {
        if (store != null && PreferencesManager.VALUE_KEYSTORE_DEFAULT ==
            PreferencesManager.get(PreferencesManager.PREFERENCE_KEYSTORE_DEFAULT_STORE)) ksm = store
    }

    fun load(file: File): LoadedDocument {
        val config = ComposeBridge.prepareSignConfig(file)
        var pages = 0
        val sizes = mutableListOf<Pair<Float, Float>>()
        var thumb: BufferedImage? = null
        if (ComposeBridge.isPdf(config)) {
            runCatching {
                PDDocument.load(file).use { doc ->
                    pages = doc.numberOfPages
                    for (p in doc.pages) {
                        val box = p.cropBox
                        sizes += if (p.rotation % 180 == 0) box.width to box.height else box.height to box.width
                    }
                    thumb = PDFRenderer(doc).renderImageWithDPI(0, 60f)
                }
            }.onFailure { LOGGER.warning("No se pudo previsualizar el PDF: $it") }
        }
        return LoadedDocument(file, config, pages, sizes, thumb)
    }

    fun renderPage(file: File, page: Int, dpi: Float): BufferedImage =
        PDDocument.load(file).use { PDFRenderer(it).renderImageWithDPI(page, dpi) }

    /** Opens the keystore configured in the preferences (may prompt for its password). */
    fun keyStore(forceReload: Boolean = false): AOKeyStoreManager {
        if (!forceReload) ksm?.let { return it }
        val k = SimpleKeyStoreManager.getKeyStore(false, false, null)
        ksm = k
        return k
    }

    fun keyStoreName(): String = runCatching {
        SimpleKeyStoreManager.getDefaultKeyStoreType().toString()
    }.getOrDefault("")

    fun certificates(forceReload: Boolean = false): List<CertOption> {
        val store = keyStore(forceReload)
        val showExpired = PreferencesManager.getBoolean(PreferencesManager.PREFERENCE_KEYSTORE_SHOWEXPIREDCERTS)
        val dialog = AOKeyStoreDialog(store, null, true, showExpired, true, ComposeBridge.getCertFilters(), false, null, false)
        return dialog.nameCertificates.orEmpty().map { CertOption(it) }
    }

    fun defaultSignatureText(): String =
        PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2TEXT).orEmpty()

    fun defaultFontSize(): Int =
        PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSIZE)?.toIntOrNull() ?: 12

    /** Font used by the PDF appearance (0 Courier, 1 Helvetica, 2 Times), for an accurate preview. */
    fun previewFontFamily(): androidx.compose.ui.text.font.FontFamily =
        when (PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTFAMILY)) {
            "1" -> androidx.compose.ui.text.font.FontFamily.SansSerif
            "2" -> androidx.compose.ui.text.font.FontFamily.Serif
            else -> androidx.compose.ui.text.font.FontFamily.Monospace
        }

    fun defaultOutput(doc: LoadedDocument): File {
        val name = doc.config.signer.getSignedName(doc.file.name, "_signed")
        return File(doc.file.parentFile, name)
    }

    suspend fun sign(
        doc: LoadedDocument,
        cert: CertOption,
        visible: VisibleSignature?,
        certificationLevel: Int,
        output: File,
        prompts: Prompts,
        progress: (String) -> Unit,
    ): SignResult {
        val config = doc.config
        val params: Properties = config.extraParams
        if (doc.isPdf) {
            if (certificationLevel > 0) params.setProperty(PdfExtraParams.CERTIFICATION_LEVEL, certificationLevel.toString())
            visible?.let { v ->
                params.setProperty(PdfExtraParams.SIGNATURE_PAGES, (v.page + 1).toString())
                params.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTX, v.llx.toInt().toString())
                params.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_LOWER_LEFTY, v.lly.toInt().toString())
                params.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTX, v.urx.toInt().toString())
                params.setProperty(PdfExtraParams.SIGNATURE_POSITION_ON_PAGE_UPPER_RIGHTY, v.ury.toInt().toString())
                params.setProperty(PdfExtraParams.LAYER2_TEXT, v.text)
                params.setProperty(PdfExtraParams.LAYER2_FONTSIZE, v.fontSize.toString())
                params.setProperty(PdfExtraParams.LAYER2_FONTCOLOR, v.fontColor)
                params.setProperty(PdfExtraParams.LAYER2_FONTFAMILY,
                    PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTFAMILY) ?: "0")
                params.setProperty(PdfExtraParams.LAYER2_FONTSTYLE,
                    PreferencesManager.get(PreferencesManager.PREFERENCE_PDF_SIGN_LAYER2FONTSTYLE) ?: "0")
            }
        }
        // Same behaviour as the Swing task: system properties can carry extra parameters
        System.getProperties().forEach { (k, v) -> if (!params.containsKey(k)) params[k] = v }

        progress("Accediendo a la clave privada")
        val pke: KeyStore.PrivateKeyEntry = keyStore().getKeyEntry(cert.bean.alias)
        val digest = config.digestAlgorithm
            ?: PreferencesManager.get(PreferencesManager.PREFERENCE_GENERAL_SIGNATURE_ALGORITHM)
        val algorithm = AOSignConstants.composeSignatureAlgorithmName(digest, pke.privateKey.algorithm)
        val data = doc.file.readBytes()

        var signature: ByteArray? = null
        var attempts = 0
        while (signature == null) {
            progress("Firmando el documento")
            try {
                signature = when (config.cryptoOperation) {
                    CryptoOperation.COSIGN -> config.signer.cosign(data, algorithm, pke.privateKey, pke.certificateChain, params)
                    else -> config.signer.sign(data, algorithm, pke.privateKey, pke.certificateChain, params)
                }
            } catch (e: RuntimeConfigNeededException) {
                if (++attempts > 6) throw e
                val text = runCatching { SimpleAfirmaMessages.getString(e.requestorText) }.getOrDefault(e.requestorText)
                when (e.requestType) {
                    RuntimeConfigNeededException.RequestType.CONFIRM -> {
                        if (!prompts.confirm("Confirmación necesaria", text)) throw es.gob.afirma.core.AOCancelledOperationException()
                        if (e is CustomRuntimeConfigNeededException) e.prepareOperationWithConfirmation(params)
                        else params.setProperty(e.param, "true")
                    }
                    RuntimeConfigNeededException.RequestType.PASSWORD -> {
                        val pwd = prompts.password("Documento protegido", text)
                            ?: throw es.gob.afirma.core.AOCancelledOperationException()
                        if (e is RuntimePasswordNeededException) e.configure(params, pwd)
                    }
                    else -> throw e
                }
            }
        }

        progress("Guardando el documento firmado")
        output.writeBytes(signature)
        config.signatureFile = output

        progress("Validando la firma")
        return SignResult(output, pke.certificate as X509Certificate, validate(signature, params), signature.size)
    }

    private fun validate(signature: ByteArray, params: Properties): List<SignValidity> = runCatching {
        val sv = SignValiderFactory.getSignValider(signature) ?: return emptyList()
        if (sv is ValidatePdfSignature) {
            val p = Properties().apply { putAll(params); setProperty(PdfExtraParams.CHECK_CERTIFICATES, "true") }
            sv.validate(signature, p)
        } else sv.validate(signature)
    }.onFailure { LOGGER.warning("No se pudo validar la firma generada: $it") }.getOrDefault(emptyList())
}
