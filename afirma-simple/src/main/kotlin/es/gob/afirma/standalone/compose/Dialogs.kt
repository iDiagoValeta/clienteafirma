package es.gob.afirma.standalone.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import es.gob.afirma.core.ui.AOUIManager
import es.gob.afirma.ui.core.jse.JSEUIManager
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import java.awt.EventQueue
import javax.swing.JOptionPane

/** A modal question rendered inside the Compose window. */
sealed class DialogRequest(val title: String, val text: String) {
    class Confirm(title: String, text: String, val danger: Boolean, val answer: CompletableDeferred<Boolean>) : DialogRequest(title, text)
    class Password(title: String, text: String, val answer: CompletableDeferred<CharArray?>) : DialogRequest(title, text)
    class Message(title: String, text: String, val error: Boolean, val answer: CompletableDeferred<Unit>) : DialogRequest(title, text)
}

/** Single slot for the dialog currently on screen. Background threads wait on it; the UI answers it. */
object DialogHost : Prompts {
    var current by mutableStateOf<DialogRequest?>(null)
        private set

    private suspend fun <T> ask(req: DialogRequest, answer: CompletableDeferred<T>): T {
        current = req
        try {
            return answer.await()
        } finally {
            if (current === req) current = null
        }
    }

    override suspend fun confirm(title: String, text: String): Boolean {
        val d = CompletableDeferred<Boolean>()
        return ask(DialogRequest.Confirm(title, text, true, d), d)
    }

    override suspend fun password(title: String, text: String): CharArray? {
        val d = CompletableDeferred<CharArray?>()
        return ask(DialogRequest.Password(title, text, d), d)
    }

    suspend fun message(title: String, text: String, error: Boolean) {
        val d = CompletableDeferred<Unit>()
        ask(DialogRequest.Message(title, text, error, d), d)
    }
}

/**
 * Routes the core library prompts (keystore and PDF passwords, confirmations, messages) to
 * Compose dialogs. Calls made on the event thread, or with rich Swing content, keep the
 * Swing implementation, since blocking the UI thread would deadlock the Compose window.
 */
class ComposeUIManager(private val fallback: JSEUIManager = JSEUIManager()) : AOUIManager by fallback {

    private fun usable(message: Any?) = !EventQueue.isDispatchThread() && (message == null || message is String)

    private fun plain(text: Any?): String =
        (text as? String).orEmpty().replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    override fun getPassword(text: String?, c: Any?): CharArray? =
        if (!usable(text)) fallback.getPassword(text, c)
        else runBlocking { DialogHost.password("Contraseña", plain(text)) }
            ?: throw es.gob.afirma.core.AOCancelledOperationException("Cancelado por el usuario")

    override fun getPassword(text: String?, icon: Any?, charSet: String?, beep: Boolean, c: Any?): CharArray? =
        if (!usable(text)) fallback.getPassword(text, icon, charSet, beep, c)
        else getPassword(text, c)

    override fun showConfirmDialog(parentComponent: Any?, message: Any?, title: String?, optionType: Int, messageType: Int): Int {
        if (!usable(message)) return fallback.showConfirmDialog(parentComponent, message, title, optionType, messageType)
        val yes = runBlocking { DialogHost.confirm(title.orEmpty(), plain(message)) }
        return when {
            yes -> JOptionPane.YES_OPTION
            optionType == JOptionPane.OK_CANCEL_OPTION -> JOptionPane.CANCEL_OPTION
            else -> JOptionPane.NO_OPTION
        }
    }

    override fun showMessageDialog(parentComponent: Any?, message: Any?, title: String?, messageType: Int) {
        if (!usable(message)) return fallback.showMessageDialog(parentComponent, message, title, messageType)
        runBlocking { DialogHost.message(title.orEmpty(), plain(message), messageType == JOptionPane.ERROR_MESSAGE) }
    }

    override fun showMessageDialog(parentComponent: Any?, message: Any?, title: String?, messageType: Int, icon: Any?) =
        showMessageDialog(parentComponent, message, title, messageType)

    override fun showErrorMessage(message: Any?, title: String?, messageType: Int, t: Throwable?) {
        if (!usable(message)) return fallback.showErrorMessage(message, title, messageType, t)
        runBlocking { DialogHost.message(title.orEmpty(), plain(message), true) }
    }

    override fun showErrorMessage(parentComponent: Any?, message: Any?, title: String?, messageType: Int, t: Throwable?) =
        showErrorMessage(message, title, messageType, t)
}
