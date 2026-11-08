package es.gob.afirma.standalone.compose

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val g: Grafito @Composable get() = LocalGrafito.current

/* ---------- Icons: drawn strokes, 24-unit grid, so they follow the theme ink ---------- */

enum class Icon { Upload, Doc, Check, Chevron, Back, Pen, Key, Shield, Folder, External, Alert, Close, Refresh, Plus, Minus }

@Composable
fun Ico(icon: Icon, color: Color = g.ink, size: Dp = 20.dp, stroke: Float = 1.7f) {
    Canvas(Modifier.size(size)) { drawIcon(icon, color, stroke) }
}

private fun DrawScope.drawIcon(icon: Icon, color: Color, strokeW: Float) {
    val u = this.size.minDimension / 24f
    val st = Stroke(width = strokeW * u * 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    fun p(vararg pts: Pair<Float, Float>, close: Boolean = false) = Path().apply {
        pts.forEachIndexed { i, (x, y) -> if (i == 0) moveTo(x * u, y * u) else lineTo(x * u, y * u) }
        if (close) close()
    }
    fun line(vararg pts: Pair<Float, Float>, close: Boolean = false) = drawPath(p(*pts, close = close), color, style = st)
    when (icon) {
        Icon.Upload -> { line(12f to 15f, 12f to 4f); line(7.5f to 8.5f, 12f to 4f, 16.5f to 8.5f); line(4f to 15f, 4f to 19f, 20f to 19f, 20f to 15f) }
        Icon.Doc -> { line(6f to 3f, 14f to 3f, 19f to 8f, 19f to 21f, 6f to 21f, close = true); line(14f to 3f, 14f to 8f, 19f to 8f); line(9f to 13f, 16f to 13f); line(9f to 17f, 14f to 17f) }
        Icon.Check -> line(5f to 12.5f, 10f to 17.5f, 19f to 7f)
        Icon.Chevron -> line(9f to 6f, 15f to 12f, 9f to 18f)
        Icon.Back -> line(15f to 6f, 9f to 12f, 15f to 18f)
        Icon.Pen -> { line(4f to 20f, 4f to 16f, 15f to 5f, 19f to 9f, 8f to 20f, close = true); line(13f to 7f, 17f to 11f) }
        Icon.Key -> { drawCircle(color, 4f * u, Offset(8f * u, 15f * u), style = st); line(11f to 12f, 19f to 4f); line(16f to 7f, 18f to 9f); line(14f to 9f, 16f to 11f) }
        Icon.Shield -> { line(12f to 3f, 19f to 6f, 19f to 11f); drawPath(Path().apply {
                moveTo(19f * u, 11f * u); cubicTo(19f * u, 16f * u, 15.5f * u, 19.5f * u, 12f * u, 21f * u)
                cubicTo(8.5f * u, 19.5f * u, 5f * u, 16f * u, 5f * u, 11f * u); lineTo(5f * u, 6f * u); lineTo(12f * u, 3f * u) }, color, style = st)
            line(9f to 12f, 11.2f to 14.2f, 15f to 10f) }
        Icon.Folder -> line(3f to 6f, 9f to 6f, 11f to 8f, 21f to 8f, 21f to 19f, 3f to 19f, close = true)
        Icon.External -> { line(14f to 4f, 20f to 4f, 20f to 10f); line(20f to 4f, 11f to 13f); line(18f to 14f, 18f to 20f, 4f to 20f, 4f to 6f, 10f to 6f) }
        Icon.Alert -> { line(12f to 4f, 21f to 20f, 3f to 20f, close = true); line(12f to 10f, 12f to 14f); drawCircle(color, 0.9f * u, Offset(12f * u, 17f * u)) }
        Icon.Close -> { line(6f to 6f, 18f to 18f); line(18f to 6f, 6f to 18f) }
        Icon.Refresh -> { drawArc(color, -60f, 300f, false, Offset(5f * u, 5f * u), Size(14f * u, 14f * u), style = st); line(16f to 3.5f, 18.8f to 6f, 15.8f to 8f) }
        Icon.Plus -> { line(12f to 5f, 12f to 19f); line(5f to 12f, 19f to 12f) }
        Icon.Minus -> line(5f to 12f, 19f to 12f)
    }
}

/* ---------- Buttons ---------- */

enum class BtnKind { Primary, Secondary, Ghost }

@Composable
fun Btn(
    text: String,
    onClick: () -> Unit,
    kind: BtnKind = BtnKind.Primary,
    enabled: Boolean = true,
    icon: Icon? = null,
    trailing: Icon? = null,
    large: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val bg = when (kind) {
        BtnKind.Primary -> if (hovered) g.primary.copy(alpha = 0.86f) else g.primary
        BtnKind.Secondary -> if (hovered) g.surfaceAlt else g.surface
        BtnKind.Ghost -> if (hovered) g.surfaceAlt else Color.Transparent
    }
    val fg = if (kind == BtnKind.Primary) g.onPrimary else g.ink
    val animBg by animateColorAsState(bg)
    Row(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) animBg else g.surfaceAlt)
            .then(if (kind == BtnKind.Secondary) Modifier.border(1.dp, g.lineStrong, RoundedCornerShape(10.dp)) else Modifier)
            .hoverable(src, enabled)
            .clickable(src, null, enabled = enabled, onClick = onClick)
            .pointerHoverIcon(if (enabled) PointerIcon.Hand else PointerIcon.Default)
            .padding(
                when {
                    text.isEmpty() -> PaddingValues(10.dp)
                    large -> PaddingValues(horizontal = 26.dp, vertical = 14.dp)
                    else -> PaddingValues(horizontal = 18.dp, vertical = 10.dp)
                }
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val c = if (enabled) fg else g.inkFaint
        icon?.let { Ico(it, c, 18.dp) }
        if (text.isNotEmpty()) Text(text, style = if (large) MaterialTheme.typography.titleMedium else MaterialTheme.typography.labelLarge, color = c)
        trailing?.let { Ico(it, c, 16.dp) }
    }
}

/* ---------- Surfaces ---------- */

@Composable
fun Card(modifier: Modifier = Modifier, padding: Dp = 24.dp, selected: Boolean = false, onClick: (() -> Unit)? = null, content: @Composable () -> Unit) {
    val src = remember { MutableInteractionSource() }
    val hovered by src.collectIsHoveredAsState()
    val border by animateColorAsState(
        when {
            selected -> g.ink
            hovered && onClick != null -> g.lineStrong
            else -> g.line
        }
    )
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(g.surface)
            .border(BorderStroke(if (selected) 2.dp else 1.dp, border), RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) Modifier.hoverable(src).clickable(src, null, onClick = onClick).pointerHoverIcon(PointerIcon.Hand)
                else Modifier
            )
            .padding(padding)
    ) { content() }
}

@Composable
fun Badge(text: String, fg: Color, bg: Color, icon: Icon? = null) {
    Row(
        Modifier.clip(RoundedCornerShape(999.dp)).background(bg).padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        icon?.let { Ico(it, fg, 13.dp, 2f) }
        Text(text, style = MaterialTheme.typography.labelMedium, color = fg)
    }
}

@Composable
fun Eyebrow(text: String) =
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = g.inkFaint)

@Composable
fun KeyValue(key: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(key, style = MaterialTheme.typography.bodySmall, color = g.inkFaint)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = g.ink)
    }
}

@Composable
fun Toggle(checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    val track by animateColorAsState(if (checked) g.primary else g.lineStrong)
    val x by androidx.compose.animation.core.animateDpAsState(if (checked) 18.dp else 2.dp)
    Box(
        Modifier.size(40.dp, 24.dp).clip(RoundedCornerShape(12.dp)).background(if (enabled) track else g.line)
            .clickable(enabled = enabled) { onChange(!checked) }.pointerHoverIcon(PointerIcon.Hand)
    ) {
        Box(Modifier.padding(start = x, top = 2.dp).size(20.dp).clip(CircleShape).background(if (checked) g.onPrimary else g.surface))
    }
}

/** Option row with title, description and a switch: the building block of the settings cards. */
@Composable
fun OptionRow(title: String, description: String, checked: Boolean, onChange: (Boolean) -> Unit, enabled: Boolean = true) {
    Row(Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f).padding(end = 16.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = if (enabled) g.ink else g.inkFaint)
            Text(description, style = MaterialTheme.typography.bodySmall, color = g.inkMuted)
        }
        Toggle(checked, onChange, enabled)
    }
}

/** Segmented control (monochrome). */
@Composable
fun <T> Segmented(options: List<Pair<T, String>>, selected: T, onSelect: (T) -> Unit) {
    Row(
        Modifier.clip(RoundedCornerShape(10.dp)).background(g.surfaceAlt).padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        options.forEach { (v, label) ->
            val sel = v == selected
            Box(
                Modifier.clip(RoundedCornerShape(8.dp)).background(if (sel) g.surface else Color.Transparent)
                    .clickable { onSelect(v) }.pointerHoverIcon(PointerIcon.Hand)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, color = if (sel) g.ink else g.inkMuted)
            }
        }
    }
}

@Composable
fun Divider() = Box(Modifier.fillMaxWidth().height(1.dp).background(g.line))

/** Circular spinner drawn as a rotating arc. */
@Composable
fun Spinner(size: Dp = 44.dp, color: Color = g.ink) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition()
    val angle by t.animateFloat(
        0f, 360f,
        androidx.compose.animation.core.infiniteRepeatable(androidx.compose.animation.core.tween(900, easing = androidx.compose.animation.core.LinearEasing))
    )
    val track = g.line
    Canvas(Modifier.size(size)) {
        val w = this.size.minDimension * 0.09f
        drawArc(track, 0f, 360f, false, Offset(w / 2, w / 2), Size(this.size.width - w, this.size.height - w), style = Stroke(w))
        drawArc(color, angle, 90f, false, Offset(w / 2, w / 2), Size(this.size.width - w, this.size.height - w), style = Stroke(w, cap = StrokeCap.Round))
    }
}

/** Dashed rounded rectangle, used by the drop zone. */
fun DrawScope.dashedRoundRect(color: Color, radius: Float, width: Float, dash: Float) {
    drawRoundRect(
        color, Offset(width / 2, width / 2), Size(size.width - width, size.height - width), CornerRadius(radius),
        style = Stroke(width, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash, dash * 0.8f)))
    )
}

@Composable
fun Avatar(text: String, size: Dp = 44.dp) {
    val initials = text.split(' ', '-').filter { it.isNotBlank() && it[0].isLetter() }.take(2).joinToString("") { it.take(1).uppercase() }
    Box(Modifier.size(size).clip(CircleShape).background(g.surfaceAlt), contentAlignment = Alignment.Center) {
        Text(initials, style = MaterialTheme.typography.titleSmall, color = g.ink, textAlign = TextAlign.Center)
    }
}

@Composable
fun HSpace(w: Dp) = Spacer(Modifier.width(w))

@Composable
fun VSpace(h: Dp) = Spacer(Modifier.height(h))
