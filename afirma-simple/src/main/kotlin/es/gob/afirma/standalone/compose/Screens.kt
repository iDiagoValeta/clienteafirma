package es.gob.afirma.standalone.compose

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.awt.Desktop
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val DATE = SimpleDateFormat("d MMM yyyy", Locale.forLanguageTag("es-ES"))
private val DATE_TIME = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.forLanguageTag("es-ES"))

private fun humanSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.0f KB".format(bytes / 1024.0)
    else -> "%.1f MB".format(bytes / (1024.0 * 1024.0))
}

/* ================================ Root ================================ */

@Composable
fun AppRoot(state: AppState, pickFile: () -> File?, pickSave: (File) -> File?, dropHover: Boolean) {
    Box(Modifier.fillMaxSize().background(g.canvas)) {
        Column(Modifier.fillMaxSize()) {
            TopBar(state)
            Box(Modifier.weight(1f).fillMaxWidth()) {
                AnimatedContent(
                    state.step,
                    transitionSpec = {
                        (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 24 }) togetherWith fadeOut(tween(120))
                    },
                ) { step ->
                    when (step) {
                        Step.Home -> HomeScreen(state, pickFile, dropHover)
                        Step.Document -> DocumentScreen(state, pickSave)
                        Step.Placement -> PlacementScreen(state)
                        Step.Certificate -> CertificateScreen(state)
                        Step.Signing -> SigningScreen(state)
                        Step.Done -> DoneScreen(state)
                    }
                }
            }
        }
        state.error?.let { (title, detail) -> ErrorToast(title, detail) { state.error = null } }
        state.busy?.let { BusyOverlay(it) }
        DialogLayer()
    }
}

@Composable
private fun TopBar(state: AppState) {
    Row(
        Modifier.fillMaxWidth().height(64.dp).padding(horizontal = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Wordmark: a pen nib in a rounded square + name
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(g.primary), contentAlignment = Alignment.Center) {
            Ico(Icon.Pen, g.onPrimary, 17.dp, 2f)
        }
        HSpace(10.dp)
        Text("Autofirma", style = MaterialTheme.typography.titleMedium, color = g.ink)
        HSpace(28.dp)
        if (state.step != Step.Home) Stepper(state)
        Box(Modifier.weight(1f))
        if (state.step != Step.Home && state.step != Step.Signing) {
            Btn("Nuevo documento", { state.reset() }, BtnKind.Ghost, icon = Icon.Plus)
        }
    }
    Divider()
}

@Composable
private fun Stepper(state: AppState) {
    val steps = state.steps
    val currentIndex = steps.indexOfFirst { it.first == state.step }.let { if (state.step == Step.Signing) steps.size - 1 else it }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        steps.forEachIndexed { i, (_, label) ->
            val done = i < currentIndex
            val active = i == currentIndex
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier.size(22.dp).clip(CircleShape)
                        .background(if (done || active) g.primary else Color.Transparent)
                        .border(1.dp, if (done || active) g.primary else g.lineStrong, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    if (done) Ico(Icon.Check, g.onPrimary, 13.dp, 2.4f)
                    else Text("${i + 1}", style = MaterialTheme.typography.labelSmall, color = if (active) g.onPrimary else g.inkFaint)
                }
                HSpace(8.dp)
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (active) g.ink else if (done) g.inkMuted else g.inkFaint)
            }
            if (i < steps.size - 1) Box(Modifier.padding(horizontal = 6.dp).width(22.dp).height(1.dp).background(g.lineStrong))
        }
    }
}

/* ================================ Home ================================ */

@Composable
private fun HomeScreen(state: AppState, pickFile: () -> File?, dropHover: Boolean) {
    var legalOpen by remember { mutableStateOf(false) }
    Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 44.dp), horizontalArrangement = Arrangement.spacedBy(48.dp)) {
        Column(Modifier.weight(0.9f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Eyebrow("Firma electrónica")
            VSpace(14.dp)
            Text("Firma tus documentos\ncon tu certificado.", style = MaterialTheme.typography.displaySmall, color = g.ink)
            VSpace(16.dp)
            Text(
                "PDF, facturas electrónicas, documentos de Office o cualquier otro fichero. La firma se hace en tu equipo: el documento no sale de él.",
                style = MaterialTheme.typography.bodyLarge, color = g.inkMuted,
            )
            VSpace(32.dp)
            Feature(Icon.Shield, "Validez legal", "Firmas avanzadas PAdES, CAdES y XAdES, reconocidas por la Administración.")
            VSpace(16.dp)
            Feature(Icon.Pen, "Firma visible en PDF", "Elige la página y dibuja el recuadro donde aparecerá tu firma.")
            VSpace(16.dp)
            Feature(Icon.Key, "Tus certificados", "DNIe, tarjetas, certificados de Firefox o un fichero .p12.")
            VSpace(28.dp)
            Row(
                Modifier.clip(RoundedCornerShape(6.dp)).clickable { legalOpen = !legalOpen }.pointerHoverIcon(PointerIcon.Hand).padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(if (legalOpen) "Ocultar aviso sobre datos personales" else "Aviso sobre datos personales", style = MaterialTheme.typography.labelMedium, color = g.inkMuted)
                HSpace(4.dp)
                Ico(Icon.Chevron, g.inkMuted, 14.dp)
            }
            AnimatedVisibility(legalOpen) {
                Text(
                    "Al firmar, el fichero puede incorporar datos personales como tu número de DNI, nombre y apellidos o, con un certificado profesional, " +
                        "información sobre tu situación laboral. Consulta las políticas de protección de datos de los destinatarios antes de enviarlo. " +
                        "Si insertas o extraes una tarjeta o dispositivo criptográfico, recarga el almacén desde la pantalla de certificados.",
                    style = MaterialTheme.typography.bodySmall, color = g.inkFaint, modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        DropZone(Modifier.weight(1.1f).fillMaxHeight(), dropHover) { pickFile()?.let(state::openFile) }
    }
}

@Composable
private fun Feature(icon: Icon, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(g.surfaceAlt), contentAlignment = Alignment.Center) { Ico(icon, g.ink, 18.dp) }
        HSpace(14.dp)
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = g.ink)
            Text(text, style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
        }
    }
}

@Composable
private fun DropZone(modifier: Modifier, hover: Boolean, onPick: () -> Unit) {
    val line = if (hover) g.ink else g.lineStrong
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(if (hover) g.surfaceAlt else g.surface)
            .clickable(onClick = onPick).pointerHoverIcon(PointerIcon.Hand),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) { dashedRoundRect(line, 22.dp.toPx(), 1.5.dp.toPx(), 7.dp.toPx()) }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
            Box(Modifier.size(64.dp).clip(CircleShape).background(g.surfaceAlt).border(1.dp, g.line, CircleShape), contentAlignment = Alignment.Center) {
                Ico(Icon.Upload, g.ink, 28.dp)
            }
            VSpace(22.dp)
            Text(if (hover) "Suelta para abrir" else "Arrastra aquí un documento", style = MaterialTheme.typography.headlineSmall, color = g.ink)
            VSpace(6.dp)
            Text("o elige uno de tu equipo", style = MaterialTheme.typography.bodyMedium, color = g.inkMuted)
            VSpace(24.dp)
            Btn("Seleccionar documento", onPick, icon = Icon.Folder, large = true)
        }
    }
}

/* ================================ Document ================================ */

@Composable
private fun DocumentScreen(state: AppState, pickSave: (File) -> File?) {
    val d = state.doc ?: return
    Row(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp), horizontalArrangement = Arrangement.spacedBy(40.dp)) {
        // Page preview
        Box(Modifier.weight(0.8f).fillMaxHeight(), contentAlignment = Alignment.TopCenter) {
            val img = d.thumbnail
            if (img != null) {
                Image(
                    img.toComposeImageBitmap(), null,
                    Modifier.shadow(18.dp, RoundedCornerShape(4.dp)).clip(RoundedCornerShape(4.dp)).background(Color.White).fillMaxHeight(0.92f),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Box(Modifier.size(220.dp, 290.dp).clip(RoundedCornerShape(14.dp)).background(g.surface).border(1.dp, g.line, RoundedCornerShape(14.dp)), contentAlignment = Alignment.Center) {
                    Ico(Icon.Doc, g.inkFaint, 56.dp, 1.3f)
                }
            }
        }
        Column(Modifier.weight(1.2f).fillMaxHeight().verticalScroll(rememberScrollState())) {
            Eyebrow(if (d.alreadySigned) "Documento ya firmado: se añadirá tu firma" else "Documento a firmar")
            VSpace(10.dp)
            Text(d.file.name, style = MaterialTheme.typography.headlineMedium, color = g.ink, maxLines = 2, overflow = TextOverflow.Ellipsis)
            VSpace(6.dp)
            Text(d.file.parent ?: "", style = MaterialTheme.typography.bodySmall, color = g.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
            VSpace(20.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(32.dp)) {
                KeyValue("Formato de firma", d.formatName.ifBlank { "Automático" })
                if (d.isPdf && d.pageCount > 0) KeyValue("Páginas", d.pageCount.toString())
                KeyValue("Tamaño", humanSize(d.sizeBytes))
                KeyValue("Modificado", DATE.format(Date(d.lastModified)))
            }
            d.config.signValidity?.firstOrNull()?.let { v ->
                VSpace(20.dp)
                val ok = v.validity == es.gob.afirma.signvalidation.SignValidity.SIGN_DETAIL_TYPE.OK
                Badge(
                    if (ok) "Las firmas existentes son válidas" else "Revisa las firmas existentes: ${v.validityTypeToString()}",
                    if (ok) g.success else g.warning, if (ok) g.successBg else g.warningBg,
                    if (ok) Icon.Check else Icon.Alert,
                )
            }
            VSpace(28.dp)
            if (d.isPdf) {
                Card(Modifier.fillMaxWidth(), padding = 20.dp) {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        OptionRow("Firma visible", "Muestra un recuadro con tus datos en la página que elijas.", state.visibleEnabled, { state.visibleEnabled = it })
                        Divider()
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("Certificar el PDF", style = MaterialTheme.typography.titleSmall, color = g.ink)
                                Text("Limita los cambios permitidos después de firmar.", style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
                            }
                            Segmented(listOf(0 to "No", 1 to "Sin cambios", 2 to "Formularios", 3 to "Anotaciones"), state.certificationLevel) { state.certificationLevel = it }
                        }
                    }
                }
                VSpace(16.dp)
            }
            Card(Modifier.fillMaxWidth(), padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Ico(Icon.Folder, g.inkMuted, 18.dp)
                    HSpace(12.dp)
                    Column(Modifier.weight(1f)) {
                        Text("Se guardará como", style = MaterialTheme.typography.bodySmall, color = g.inkFaint)
                        Text(state.output?.name ?: "", style = MaterialTheme.typography.bodyMedium, color = g.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Btn("Cambiar", { state.output?.let(pickSave)?.let { state.output = it } }, BtnKind.Ghost)
                }
            }
            VSpace(32.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Btn("Atrás", { state.back() }, BtnKind.Secondary, icon = Icon.Back)
                Box(Modifier.weight(1f))
                Btn(if (d.isPdf && state.visibleEnabled) "Colocar la firma" else "Elegir certificado", { state.continueFromDocument() }, trailing = Icon.Chevron, large = true)
            }
        }
    }
}

/* ================================ Placement ================================ */

private val SAMPLE_VALUES = mapOf(
    "\$\$SUBJECTCN\$\$" to "NOMBRE APELLIDOS - 00000000T",
    "\$\$ISSUERCN\$\$" to "AC FNMT Usuarios",
    "\$\$CERTSERIAL\$\$" to "1A2B3C",
    "\$\$SUBJECTDN\$\$" to "CN=NOMBRE APELLIDOS",
)

private fun previewText(template: String): String {
    var s = template
    SAMPLE_VALUES.forEach { (k, v) -> s = s.replace(k, v) }
    s = Regex("\\$\\\$SIGNDATE=([^$]+)\\$\\$").replace(s) { m -> runCatching { SimpleDateFormat(m.groupValues[1]).format(Date()) }.getOrDefault(m.value) }
    return s
}

@Composable
private fun PlacementScreen(state: AppState) {
    val d = state.doc ?: return
    val prev = state.visible
    var page by remember { mutableStateOf(prev?.page ?: (d.pageCount - 1).coerceAtLeast(0)) }
    // Rectangle in fractions of the page (0..1, origin top-left), so it survives page and zoom changes
    var rect by remember {
        mutableStateOf(
            prev?.let { v ->
                val (w, h) = d.pageSizes.getOrElse(v.page) { 595f to 842f }
                floatArrayOf(v.llx / w, 1 - v.ury / h, v.urx / w, 1 - v.lly / h)
            } ?: floatArrayOf(0.52f, 0.80f, 0.92f, 0.90f)
        )
    }
    var text by remember { mutableStateOf(prev?.text ?: Engine.defaultSignatureText()) }
    var fontSize by remember { mutableStateOf(prev?.fontSize ?: Engine.defaultFontSize()) }
    var color by remember { mutableStateOf(prev?.fontColor ?: "black") }

    val image by produceState<androidx.compose.ui.graphics.ImageBitmap?>(null, page) {
        value = withContext(Dispatchers.IO) { runCatching { Engine.renderPage(d.file, page, 110f).toComposeImageBitmap() }.getOrNull() }
    }
    val (pw, ph) = d.pageSizes.getOrElse(page) { 595f to 842f }
    val rectWpt = (rect[2] - rect[0]) * pw
    val rectHpt = (rect[3] - rect[1]) * ph
    val tooSmall = rectWpt < 60f || rectHpt < 25f

    Row(Modifier.fillMaxSize()) {
        // Page canvas
        Column(Modifier.weight(1f).fillMaxHeight().background(g.surfaceAlt).padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("", { page = (page - 1).coerceAtLeast(0) }, BtnKind.Secondary, enabled = page > 0, icon = Icon.Back)
                Text("Página ${page + 1} de ${d.pageCount}", style = MaterialTheme.typography.labelLarge, color = g.ink, modifier = Modifier.widthIn(min = 120.dp))
                Btn("", { page = (page + 1).coerceAtMost(d.pageCount - 1) }, BtnKind.Secondary, enabled = page < d.pageCount - 1, icon = Icon.Chevron)
            }
            VSpace(8.dp)
            Text("Arrastra sobre la página para dibujar el recuadro de la firma", style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
            VSpace(16.dp)
            BoxWithConstraints(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                val aspect = pw / ph
                val boxW = minOf(maxWidth.value, maxHeight.value * aspect)
                val boxH = boxW / aspect
                val density = LocalDensity.current
                Box(Modifier.size(boxW.dp, boxH.dp).shadow(16.dp, RoundedCornerShape(3.dp)).background(Color.White)) {
                    image?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.FillBounds) }
                    var dragStart by remember { mutableStateOf(Offset.Zero) }
                    Box(
                        Modifier.fillMaxSize().pointerHoverIcon(PointerIcon.Crosshair).pointerInput(page) {
                            detectDragGestures(
                                onDragStart = { o -> dragStart = o; rect = floatArrayOf(o.x / size.width, o.y / size.height, o.x / size.width, o.y / size.height) },
                                onDrag = { change, _ ->
                                    val p = change.position
                                    val x0 = (dragStart.x / size.width).coerceIn(0f, 1f); val y0 = (dragStart.y / size.height).coerceIn(0f, 1f)
                                    val x1 = (p.x / size.width).coerceIn(0f, 1f); val y1 = (p.y / size.height).coerceIn(0f, 1f)
                                    rect = floatArrayOf(minOf(x0, x1), minOf(y0, y1), maxOf(x0, x1), maxOf(y0, y1))
                                },
                            )
                        }
                    )
                    // Signature box with live preview of the text
                    val l = rect[0] * boxW; val t = rect[1] * boxH
                    val w = (rect[2] - rect[0]) * boxW; val h = (rect[3] - rect[1]) * boxH
                    if (w > 2 && h > 2) {
                        Box(
                            Modifier.offset(l.dp, t.dp).size(w.dp, h.dp)
                                .background(Color(0x14000000)).border(1.5.dp, if (tooSmall) g.error else Color(0xFF1C1C1B))
                                .padding(3.dp)
                        ) {
                            val scale = boxW / pw // dp per PDF point
                            val fontPx = with(density) { (fontSize * scale).dp.toSp() }
                            Text(
                                previewText(text), color = pdfColor(color),
                                style = TextStyle(fontSize = fontPx, lineHeight = fontPx * 1.15f, fontFamily = Engine.previewFontFamily()),
                                overflow = TextOverflow.Clip,
                            )
                        }
                    }
                }
            }
        }
        // Appearance panel
        Column(Modifier.width(380.dp).fillMaxHeight().background(g.canvas).padding(28.dp).verticalScroll(rememberScrollState())) {
            Eyebrow("Firma visible")
            VSpace(8.dp)
            Text("Apariencia", style = MaterialTheme.typography.headlineSmall, color = g.ink)
            VSpace(6.dp)
            Text("Los campos entre \$\$ se sustituyen por los datos de tu certificado al firmar.", style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
            VSpace(18.dp)
            Text("Texto", style = MaterialTheme.typography.labelMedium, color = g.inkMuted)
            VSpace(6.dp)
            BasicTextField(
                text, { text = it },
                textStyle = MaterialTheme.typography.bodyMedium.copy(color = g.ink),
                cursorBrush = SolidColor(g.ink),
                modifier = Modifier.fillMaxWidth().height(120.dp).clip(RoundedCornerShape(10.dp)).background(g.surface)
                    .border(1.dp, g.lineStrong, RoundedCornerShape(10.dp)).padding(12.dp),
            )
            VSpace(10.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Nombre" to "\$\$SUBJECTCN\$\$", "Emisor" to "\$\$ISSUERCN\$\$", "Fecha" to "\$\$SIGNDATE=dd/MM/yyyy\$\$").forEach { (label, token) ->
                    Box(
                        Modifier.clip(RoundedCornerShape(999.dp)).border(1.dp, g.lineStrong, RoundedCornerShape(999.dp))
                            .clickable { text = "$text $token".trim() }.pointerHoverIcon(PointerIcon.Hand).padding(horizontal = 10.dp, vertical = 5.dp)
                    ) { Text("+ $label", style = MaterialTheme.typography.labelMedium, color = g.inkMuted) }
                }
            }
            VSpace(22.dp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Tamaño de letra", style = MaterialTheme.typography.labelMedium, color = g.inkMuted, modifier = Modifier.weight(1f))
                Btn("", { fontSize = (fontSize - 1).coerceAtLeast(6) }, BtnKind.Secondary, enabled = fontSize > 6, icon = Icon.Minus)
                Text("$fontSize pt", style = MaterialTheme.typography.labelLarge, color = g.ink, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.width(64.dp))
                Btn("", { fontSize = (fontSize + 1).coerceAtMost(24) }, BtnKind.Secondary, enabled = fontSize < 24, icon = Icon.Plus)
            }
            VSpace(18.dp)
            Text("Color", style = MaterialTheme.typography.labelMedium, color = g.inkMuted)
            VSpace(8.dp)
            Segmented(listOf("black" to "Negro", "darkGray" to "Gris oscuro", "gray" to "Gris", "blue" to "Azul"), color) { color = it }
            VSpace(22.dp)
            Card(Modifier.fillMaxWidth(), padding = 14.dp) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    KeyValue("Página", "${page + 1}")
                    KeyValue("Recuadro", "${rectWpt.toInt()} × ${rectHpt.toInt()} pt")
                }
            }
            if (tooSmall) {
                VSpace(10.dp)
                Badge("El recuadro es demasiado pequeño", g.error, g.errorBg, Icon.Alert)
            }
            VSpace(28.dp)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Btn("Atrás", { state.back() }, BtnKind.Secondary, icon = Icon.Back)
                Box(Modifier.weight(1f))
                Btn("Continuar", {
                    state.confirmPlacement(
                        VisibleSignature(page, rect[0] * pw, (1 - rect[3]) * ph, rect[2] * pw, (1 - rect[1]) * ph, text, fontSize, color)
                    )
                }, enabled = !tooSmall, trailing = Icon.Chevron)
            }
        }
    }
}

private fun pdfColor(name: String) = when (name) {
    "darkGray" -> Color(0xFF404040)
    "gray" -> Color(0xFF808080)
    "blue" -> Color(0xFF1F3F9F)
    "red" -> Color(0xFFB00000)
    else -> Color.Black
}

/* ================================ Certificate ================================ */

@Composable
private fun CertificateScreen(state: AppState) {
    LaunchedEffect(Unit) { state.loadCerts() }
    Column(Modifier.fillMaxSize().padding(horizontal = 56.dp, vertical = 36.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                Eyebrow("Almacén: ${Engine.keyStoreName()}")
                VSpace(8.dp)
                Text("¿Con qué certificado firmas?", style = MaterialTheme.typography.headlineMedium, color = g.ink)
            }
            Btn("Recargar", { state.loadCerts(force = true) }, BtnKind.Ghost, icon = Icon.Refresh)
        }
        VSpace(22.dp)
        Box(Modifier.weight(1f).fillMaxWidth()) {
            val list = state.certs
            when {
                state.certsError != null -> EmptyState(Icon.Alert, "No se pudieron leer los certificados", state.certsError!!) {
                    Btn("Reintentar", { state.loadCerts(force = true) }, BtnKind.Secondary, icon = Icon.Refresh)
                }
                list == null -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Spinner(); VSpace(16.dp)
                    Text("Leyendo el almacén de certificados", style = MaterialTheme.typography.bodyMedium, color = g.inkMuted)
                }
                list.isEmpty() -> EmptyState(Icon.Key, "No hay certificados de firma", "Inserta tu tarjeta o DNIe y pulsa Recargar, o cambia el almacén en Preferencias.") {
                    Btn("Recargar", { state.loadCerts(force = true) }, BtnKind.Secondary, icon = Icon.Refresh)
                }
                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    list.forEach { c -> CertCard(c, state.selectedCert?.bean?.alias == c.bean.alias) { if (!c.expired) state.selectedCert = c } }
                }
            }
        }
        VSpace(20.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Btn("Atrás", { state.back() }, BtnKind.Secondary, icon = Icon.Back)
            Box(Modifier.weight(1f))
            state.doc?.let { Text(it.file.name, style = MaterialTheme.typography.bodySmall, color = g.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 280.dp)) }
            HSpace(8.dp)
            Btn("Firmar documento", { state.sign() }, enabled = state.selectedCert != null, icon = Icon.Pen, large = true)
        }
    }
}

@Composable
private fun CertCard(c: CertOption, selected: Boolean, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth(), padding = 18.dp, selected = selected, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Avatar(c.holder)
            HSpace(16.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(c.holder, style = MaterialTheme.typography.titleMedium, color = if (c.expired) g.inkFaint else g.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Emitido por ${c.issuer}", style = MaterialTheme.typography.bodySmall, color = g.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            HSpace(16.dp)
            when {
                c.expired -> Badge("Caducado", g.error, g.errorBg, Icon.Alert)
                c.notYetValid -> Badge("Aún no válido", g.warning, g.warningBg, Icon.Alert)
                else -> Text("Válido hasta ${DATE.format(c.notAfter)}", style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
            }
            HSpace(16.dp)
            Box(
                Modifier.size(22.dp).clip(CircleShape).background(if (selected) g.primary else Color.Transparent).border(1.5.dp, if (selected) g.primary else g.lineStrong, CircleShape),
                contentAlignment = Alignment.Center,
            ) { if (selected) Ico(Icon.Check, g.onPrimary, 13.dp, 2.6f) }
        }
    }
}

@Composable
private fun EmptyState(icon: Icon, title: String, text: String, action: @Composable () -> Unit) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Box(Modifier.size(56.dp).clip(CircleShape).background(g.surfaceAlt), contentAlignment = Alignment.Center) { Ico(icon, g.inkMuted, 24.dp) }
        VSpace(16.dp)
        Text(title, style = MaterialTheme.typography.titleLarge, color = g.ink)
        VSpace(6.dp)
        Text(text, style = MaterialTheme.typography.bodyMedium, color = g.inkMuted, modifier = Modifier.widthIn(max = 460.dp), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        VSpace(18.dp)
        action()
    }
}

/* ================================ Signing ================================ */

@Composable
private fun SigningScreen(state: AppState) {
    Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Spinner(64.dp)
        VSpace(28.dp)
        Text("Firmando", style = MaterialTheme.typography.headlineMedium, color = g.ink)
        VSpace(8.dp)
        AnimatedContent(state.progress) { Text(it, style = MaterialTheme.typography.bodyLarge, color = g.inkMuted) }
        VSpace(6.dp)
        Text("Si usas una tarjeta, no la retires hasta terminar.", style = MaterialTheme.typography.bodySmall, color = g.inkFaint)
    }
}

/* ================================ Done ================================ */

@Composable
private fun DoneScreen(state: AppState) {
    val r = state.result ?: return
    val v = r.validity.firstOrNull()
    val problem = v != null && (v.validity == es.gob.afirma.signvalidation.SignValidity.SIGN_DETAIL_TYPE.KO || v.validity == es.gob.afirma.signvalidation.SignValidity.SIGN_DETAIL_TYPE.UNKNOWN)
    val holder = runCatching { es.gob.afirma.standalone.crypto.CertAnalyzer.getCertInformation(r.signer).holderName }.getOrNull() ?: cnOf(r.signer.subjectX500Principal.name)
    val issuer = cnOf(r.signer.issuerX500Principal.name)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 56.dp, vertical = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(76.dp).clip(CircleShape).background(if (problem) g.warningBg else g.successBg),
            contentAlignment = Alignment.Center,
        ) { Ico(if (problem) Icon.Alert else Icon.Check, if (problem) g.warning else g.success, 36.dp, 2.4f) }
        VSpace(22.dp)
        Text(if (problem) "Documento firmado, con avisos" else "Documento firmado", style = MaterialTheme.typography.headlineMedium, color = g.ink)
        VSpace(8.dp)
        Text(
            if (problem) "La firma se ha generado, pero su validación devolvió: ${v!!.validityTypeToString()}." else "La firma se ha generado y validado correctamente.",
            style = MaterialTheme.typography.bodyLarge, color = g.inkMuted,
        )
        VSpace(32.dp)
        Column(Modifier.widthIn(max = 640.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(Modifier.fillMaxWidth(), padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(44.dp).clip(RoundedCornerShape(10.dp)).background(g.surfaceAlt), contentAlignment = Alignment.Center) { Ico(Icon.Doc, g.ink, 22.dp) }
                    HSpace(16.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(r.output.name, style = MaterialTheme.typography.titleMedium, color = g.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(r.output.parent ?: "", style = MaterialTheme.typography.bodySmall, color = g.inkFaint, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(humanSize(r.output.length()), style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
                }
            }
            Card(Modifier.fillMaxWidth(), padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Avatar(holder)
                    HSpace(16.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Firmado por", style = MaterialTheme.typography.bodySmall, color = g.inkFaint)
                        Text(holder, style = MaterialTheme.typography.titleMedium, color = g.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("Certificado emitido por $issuer", style = MaterialTheme.typography.bodySmall, color = g.inkMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Text(DATE_TIME.format(Date()), style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
                }
            }
        }
        VSpace(32.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Btn("Abrir documento", { openDesktop(r.output) }, icon = Icon.External, large = true)
            Btn("Mostrar en la carpeta", { openDesktop(r.output.parentFile) }, BtnKind.Secondary, icon = Icon.Folder, large = true)
            Btn("Firmar otro", { state.reset() }, BtnKind.Ghost, icon = Icon.Plus, large = true)
        }
    }
}

private fun openDesktop(f: File?) {
    if (f == null) return
    Thread { runCatching { Desktop.getDesktop().open(f) } }.start()
}

/* ================================ Overlays ================================ */

@Composable
private fun BusyOverlay(text: String) {
    Box(Modifier.fillMaxSize().background(g.canvas.copy(alpha = 0.82f)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spinner()
            VSpace(14.dp)
            Text(text, style = MaterialTheme.typography.bodyMedium, color = g.inkMuted)
        }
    }
}

@Composable
private fun ErrorToast(title: String, detail: String, onClose: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.BottomCenter) {
        Row(
            Modifier.widthIn(max = 620.dp).shadow(12.dp, RoundedCornerShape(14.dp)).clip(RoundedCornerShape(14.dp)).background(g.surface)
                .border(1.dp, g.line, RoundedCornerShape(14.dp)).padding(16.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(Modifier.size(32.dp).clip(CircleShape).background(g.errorBg), contentAlignment = Alignment.Center) { Ico(Icon.Alert, g.error, 17.dp) }
            HSpace(12.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.titleSmall, color = g.ink)
                Text(detail, style = MaterialTheme.typography.bodySmall, color = g.inkMuted, maxLines = 4, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.clip(CircleShape).clickable(onClick = onClose).padding(4.dp)) { Ico(Icon.Close, g.inkMuted, 16.dp) }
        }
    }
}

@Composable
private fun DialogLayer() {
    val req = DialogHost.current ?: return
    var pwd by remember(req) { mutableStateOf("") }
    Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.38f)).clickable(enabled = true, onClick = {}), contentAlignment = Alignment.Center) {
        Column(
            Modifier.width(460.dp).shadow(24.dp, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(g.surface).padding(26.dp)
        ) {
            val (icon, fg, bg) = when (req) {
                is DialogRequest.Password -> Triple(Icon.Key, g.ink, g.surfaceAlt)
                is DialogRequest.Message -> if (req.error) Triple(Icon.Alert, g.error, g.errorBg) else Triple(Icon.Check, g.ink, g.surfaceAlt)
                is DialogRequest.Confirm -> Triple(Icon.Alert, g.warning, g.warningBg)
            }
            Box(Modifier.size(40.dp).clip(CircleShape).background(bg), contentAlignment = Alignment.Center) { Ico(icon, fg, 20.dp) }
            VSpace(16.dp)
            Text(req.title.ifBlank { "Autofirma" }, style = MaterialTheme.typography.titleLarge, color = g.ink)
            VSpace(8.dp)
            Text(req.text, style = MaterialTheme.typography.bodyMedium, color = g.inkMuted)
            if (req is DialogRequest.Password) {
                VSpace(16.dp)
                val focus = remember { androidx.compose.ui.focus.FocusRequester() }
                BasicTextField(
                    pwd, { pwd = it }, singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = g.ink, fontWeight = FontWeight.Normal, fontSize = 16.sp),
                    cursorBrush = SolidColor(g.ink),
                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(g.canvas)
                        .border(1.dp, g.lineStrong, RoundedCornerShape(10.dp)).padding(horizontal = 14.dp, vertical = 12.dp)
                        .focusRequester(focus)
                        .onKeyEnter { req.answer.complete(pwd.toCharArray()) },
                )
                LaunchedEffect(req) { runCatching { focus.requestFocus() } }
            }
            VSpace(24.dp)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End)) {
                when (req) {
                    is DialogRequest.Password -> {
                        Btn("Cancelar", { req.answer.complete(null) }, BtnKind.Secondary)
                        Btn("Aceptar", { req.answer.complete(pwd.toCharArray()) })
                    }
                    is DialogRequest.Confirm -> {
                        Btn("No", { req.answer.complete(false) }, BtnKind.Secondary)
                        Btn("Sí, continuar", { req.answer.complete(true) })
                    }
                    is DialogRequest.Message -> Btn("Entendido", { req.answer.complete(Unit) })
                }
            }
        }
    }
}

private fun Modifier.onKeyEnter(action: () -> Unit) = this.onPreviewKeyEvent { e ->
    if (e.key == Key.Enter && e.type == KeyEventType.KeyDown) { action(); true } else false
}
