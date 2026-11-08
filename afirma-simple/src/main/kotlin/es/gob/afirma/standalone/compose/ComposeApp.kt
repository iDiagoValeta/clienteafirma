@file:JvmName("ComposeLauncher")

package es.gob.afirma.standalone.compose

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import es.gob.afirma.core.ui.AOUIFactory
import es.gob.afirma.keystores.AOKeyStoreManager
import es.gob.afirma.standalone.LookAndFeelManager
import java.awt.Dimension
import java.awt.FileDialog
import java.awt.datatransfer.DataFlavor
import java.awt.dnd.DnDConstants
import java.awt.dnd.DropTarget
import java.awt.dnd.DropTargetAdapter
import java.awt.dnd.DropTargetDragEvent
import java.awt.dnd.DropTargetDropEvent
import java.awt.dnd.DropTargetEvent
import java.io.File
import kotlin.system.exitProcess

/** True unless the classic Swing interface is requested with {@code -Dafirma.ui=swing}. */
fun isEnabled(): Boolean = !"swing".equals(System.getProperty("afirma.ui"), ignoreCase = true)

/**
 * Starts the Compose interface. Blocks until the window is closed and then exits.
 * @param preloadedKeyStore Keystore already opened at startup (the NSS early preload), or null.
 * @param initialFile File to open straight away, or null.
 */
fun launch(preloadedKeyStore: AOKeyStoreManager?, initialFile: File?) {
    // Skia's OpenGL backend fails under XWayland with some drivers; software rendering is
    // fast enough for this UI and works everywhere
    if (System.getProperty("skiko.renderApi") == null) System.setProperty("skiko.renderApi", "SOFTWARE_FAST")
    Engine.preload(preloadedKeyStore)
    AOUIFactory.setUIManager(ComposeUIManager())

    application {
        val windowState = rememberWindowState(size = DpSize(1120.dp, 760.dp), position = WindowPosition.PlatformDefault)
        Window(
            onCloseRequest = { exitApplication(); exitProcess(0) },
            state = windowState,
            title = "Autofirma",
        ) {
            window.minimumSize = Dimension(940, 640)
            val scope = rememberCoroutineScope()
            // -Dafirma.ui.open=<file> opens a document at startup (any program argument would
            // switch AutoFirma to its command line mode instead)
            val toOpen = initialFile ?: System.getProperty("afirma.ui.open")?.let(::File)?.takeIf { it.isFile }
            val state = remember { AppState(scope).also { s -> toOpen?.let { s.openFile(it) } } }
            var dropHover by remember { mutableStateOf(false) }

            remember {
                window.contentPane.dropTarget = DropTarget(window.contentPane, DnDConstants.ACTION_COPY, object : DropTargetAdapter() {
                    override fun dragEnter(e: DropTargetDragEvent) { dropHover = true }
                    override fun dragExit(e: DropTargetEvent) { dropHover = false }
                    override fun drop(e: DropTargetDropEvent) {
                        dropHover = false
                        e.acceptDrop(DnDConstants.ACTION_COPY)
                        @Suppress("UNCHECKED_CAST")
                        val files = runCatching { e.transferable.getTransferData(DataFlavor.javaFileListFlavor) as List<File> }.getOrNull()
                        files?.firstOrNull { it.isFile }?.let { state.openFile(it) }
                        e.dropComplete(files != null)
                    }
                }, true)
                Unit
            }

            // -Dafirma.ui.theme=light|dark overrides the system theme
            val dark = when (System.getProperty("afirma.ui.theme")?.lowercase()) {
                "light" -> false
                "dark" -> true
                else -> LookAndFeelManager.GNOME_DARK_MODE
            }
            GrafitoTheme(dark = dark) {
                AppRoot(
                    state,
                    pickFile = {
                        val fd = FileDialog(window, "Selecciona el documento a firmar", FileDialog.LOAD)
                        fd.isVisible = true
                        fd.file?.let { File(fd.directory, it) }
                    },
                    pickSave = { current ->
                        val fd = FileDialog(window, "Guardar el documento firmado como", FileDialog.SAVE)
                        fd.directory = current.parent
                        fd.file = current.name
                        fd.isVisible = true
                        fd.file?.let { File(fd.directory, it) }
                    },
                    dropHover = dropHover,
                )
            }
        }
    }
}
