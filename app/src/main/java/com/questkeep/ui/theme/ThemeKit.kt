package com.questkeep.ui.theme

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * ThemeKit — каждая тема предоставляет свои структурные компоненты
 * с общим интерфейсом, но радикально разным визуалом.
 */
interface ThemeKit {
    /** Кубик-кнопка для выбора типа кости */
    @Composable fun Die(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier)
    /** Плашка характеристики (СИЛ 12, ЛОВ 15...) */
    @Composable fun StatPlate(label: String, value: Int, onClick: () -> Unit = {}, modifier: Modifier = Modifier)
    /** Шкала ресурса (HP, Эфир, Броня) */
    @Composable fun Gauge(label: String, value: Int, max: Int, color: Color, modifier: Modifier = Modifier)
    /** Пузырь сообщения в чате */
    @Composable fun ChatBubble(author: String, text: String, mine: Boolean, modifier: Modifier = Modifier)
}

// ═══════════════════════════════════════════════════════════════════════════════
// 1. ФАЛЛОРАН — Клёпаные гайки, сегментные шкалы, телеграмма
// ═══════════════════════════════════════════════════════════════════════════════

class FalloranKit : ThemeKit {

    @Composable
    override fun Die(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val c = FalloranColors
        val measurer = rememberTextMeasurer()
        Box(
            modifier = modifier
                .size(52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Бросить d$n" }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val hex = Path().apply {
                    moveTo(w * 0.5f, h * 0.04f); lineTo(w * 0.93f, h * 0.27f)
                    lineTo(w * 0.93f, h * 0.73f); lineTo(w * 0.5f, h * 0.96f)
                    lineTo(w * 0.07f, h * 0.73f); lineTo(w * 0.07f, h * 0.27f); close()
                }
                drawPath(hex, if (active) c.accent else c.panel)
                drawPath(hex, c.accent, style = Stroke(width = 2.5.dp.toPx()))
                // Inner screw hole ring (industrial nut feel)
                drawCircle(c.line, radius = w * 0.22f, style = Stroke(width = 1.dp.toPx()))
                val label = measurer.measure(
                    "d$n",
                    TextStyle(
                        fontFamily = RussoOneFamily, fontSize = 13.sp,
                        color = if (active) c.bg else c.text
                    )
                )
                drawText(label, topLeft = Offset((w - label.size.width) / 2f, (h - label.size.height) / 2f))
            }
        }
    }

    @Composable
    override fun StatPlate(label: String, value: Int, onClick: () -> Unit, modifier: Modifier) {
        val c = FalloranColors
        Box(
            modifier = modifier
                .widthIn(min = 92.dp)
                .height(72.dp)
                .clickable(onClick = onClick)
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                // Plate background
                drawRoundRect(c.panel, size = size, cornerRadius = CornerRadius(2.dp.toPx()))
                // Border
                drawRoundRect(c.line, size = size, cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
                // 4 corner rivets
                val rv = c.accent.copy(alpha = 0.8f)
                val rr = 2.5.dp.toPx(); val off = 6.dp.toPx()
                drawCircle(rv, rr, Offset(off, off))
                drawCircle(rv, rr, Offset(w - off, off))
                drawCircle(rv, rr, Offset(off, h - off))
                drawCircle(rv, rr, Offset(w - off, h - off))
            }
            Column(
                Modifier.fillMaxSize().padding(4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(label.uppercase(), color = c.muted, fontSize = 11.sp,
                    fontFamily = SpecialEliteFamily, fontWeight = FontWeight.SemiBold)
                Text("$value", color = c.text, fontSize = 22.sp,
                    fontFamily = RussoOneFamily, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    override fun Gauge(label: String, value: Int, max: Int, color: Color, modifier: Modifier) {
        val c = FalloranColors
        val safeMax = max.coerceAtLeast(1)
        Column(modifier = modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label.uppercase(), color = c.muted, fontSize = 12.sp,
                    fontFamily = SpecialEliteFamily, fontWeight = FontWeight.SemiBold)
                Text("$value/$max", color = c.text, fontSize = 13.sp,
                    fontFamily = RussoOneFamily, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            // Segmented bar (industrial gauges)
            Canvas(Modifier.fillMaxWidth().height(14.dp)) {
                val w = size.width; val h = size.height
                val segments = safeMax.coerceAtMost(30)
                val gap = 2.dp.toPx()
                val segW = (w - gap * (segments - 1)) / segments
                for (i in 0 until segments) {
                    val x = i * (segW + gap)
                    val filled = i < (value.toFloat() / safeMax * segments).toInt()
                    drawRoundRect(
                        color = if (filled) color else c.panel,
                        topLeft = Offset(x, 0f),
                        size = Size(segW, h),
                        cornerRadius = CornerRadius(1.dp.toPx())
                    )
                }
                // Outer rim
                drawRoundRect(c.line, size = size, cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(1.dp.toPx()))
            }
        }
    }

    @Composable
    override fun ChatBubble(author: String, text: String, mine: Boolean, modifier: Modifier) {
        val c = FalloranColors
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = c.panel,
                shape = TornBottom,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("ТЕЛЕГРАММА", color = c.muted, fontSize = 10.sp,
                        fontFamily = RussoOneFamily, letterSpacing = 2.sp)
                    Spacer(Modifier.height(4.dp))
                    Text("$author:", color = c.accent, fontSize = 13.sp,
                        fontFamily = RussoOneFamily, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(text, color = c.text, fontSize = 14.sp, fontFamily = SpecialEliteFamily)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 2. ЦИНГЕЙТ — Латунный компас, судовой журнал, карты
// ═══════════════════════════════════════════════════════════════════════════════

class TsingateKit : ThemeKit {

    @Composable
    override fun Die(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val c = TsingateColors
        val measurer = rememberTextMeasurer()
        Box(
            modifier = modifier
                .size(52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Бросить d$n" }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2; val cy = h / 2; val r = w * 0.42f
                // Compass dial circle
                drawCircle(if (active) c.accent else c.panel, radius = r, center = Offset(cx, cy))
                drawCircle(c.accent, radius = r, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
                // Tick marks around the edge
                for (i in 0 until 12) {
                    val angle = (i * 30.0 * PI / 180.0).toFloat()
                    val inner = r * 0.78f; val outer = r * 0.95f
                    drawLine(
                        c.accent.copy(alpha = 0.5f),
                        Offset(cx + cos(angle) * inner, cy + sin(angle) * inner),
                        Offset(cx + cos(angle) * outer, cy + sin(angle) * outer),
                        strokeWidth = 1.dp.toPx()
                    )
                }
                val label = measurer.measure(
                    "d$n",
                    TextStyle(
                        fontFamily = PtSerifFamily, fontSize = 13.sp,
                        color = if (active) c.bg else c.text
                    )
                )
                drawText(label, topLeft = Offset((w - label.size.width) / 2f, (h - label.size.height) / 2f))
            }
        }
    }

    @Composable
    override fun StatPlate(label: String, value: Int, onClick: () -> Unit, modifier: Modifier) {
        val c = TsingateColors
        Surface(
            modifier = modifier
                .widthIn(min = 92.dp)
                .height(72.dp)
                .clickable(onClick = onClick),
            color = c.panel,
            shape = CutCornerShape
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(label.uppercase(), color = c.muted, fontSize = 11.sp,
                        fontFamily = PtSerifFamily)
                    Text("$value", color = c.text, fontSize = 22.sp,
                        fontFamily = RuslanDisplayFamily, fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    @Composable
    override fun Gauge(label: String, value: Int, max: Int, color: Color, modifier: Modifier) {
        val c = TsingateColors
        val safeMax = max.coerceAtLeast(1)
        val fraction = (value.toFloat() / safeMax).coerceIn(0f, 1f)
        Column(modifier = modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = c.muted, fontSize = 12.sp, fontFamily = PtSerifFamily)
                Text("$value/$max", color = c.text, fontSize = 13.sp,
                    fontFamily = RuslanDisplayFamily, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val w = size.width; val h = size.height
                drawRoundRect(c.panel, size = size, cornerRadius = CornerRadius(h / 2))
                if (fraction > 0f) {
                    drawRoundRect(color, size = Size(w * fraction, h), cornerRadius = CornerRadius(h / 2))
                }
                drawRoundRect(c.line, size = size, cornerRadius = CornerRadius(h / 2), style = Stroke(1.dp.toPx()))
            }
        }
    }

    @Composable
    override fun ChatBubble(author: String, text: String, mine: Boolean, modifier: Modifier) {
        val c = TsingateColors
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = c.panel,
                shape = RoundedCornerShape(6.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(Modifier.padding(12.dp)) {
                    Text("Судовой журнал", color = c.muted, fontSize = 10.sp,
                        fontFamily = PtSerifFamily, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(4.dp))
                    Text("$author:", color = c.accent, fontSize = 13.sp,
                        fontFamily = RuslanDisplayFamily, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(text, color = c.text, fontSize = 14.sp, fontFamily = PtSerifFamily)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 3. НИРВЕНТАЛЬ — Годовые кольца, руны, листья, асимметричные панели
// ═══════════════════════════════════════════════════════════════════════════════

class NirventhalKit : ThemeKit {

    @Composable
    override fun Die(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val c = NirventhalColors
        val measurer = rememberTextMeasurer()
        Box(
            modifier = modifier
                .size(52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Бросить d$n" }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2; val cy = h / 2; val r = w * 0.42f
                // Tree ring circles (annual rings)
                drawCircle(if (active) c.accent else c.panel, radius = r, center = Offset(cx, cy))
                drawCircle(c.accent, radius = r, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
                drawCircle(c.accent.copy(alpha = 0.3f), radius = r * 0.7f, center = Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
                drawCircle(c.accent.copy(alpha = 0.2f), radius = r * 0.45f, center = Offset(cx, cy), style = Stroke(0.8.dp.toPx()))
                val label = measurer.measure(
                    "d$n",
                    TextStyle(
                        fontFamily = PhilosopherFamily, fontSize = 13.sp,
                        color = if (active) c.bg else c.text
                    )
                )
                drawText(label, topLeft = Offset((w - label.size.width) / 2f, (h - label.size.height) / 2f))
            }
        }
    }

    @Composable
    override fun StatPlate(label: String, value: Int, onClick: () -> Unit, modifier: Modifier) {
        val c = NirventhalColors
        Surface(
            modifier = modifier
                .widthIn(min = 92.dp)
                .height(72.dp)
                .clickable(onClick = onClick),
            color = c.panel,
            shape = OrganicPanel
        ) {
            Row(
                Modifier.fillMaxSize().padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(label, color = c.muted, fontSize = 13.sp,
                    fontFamily = PhilosopherFamily)
                Text("$value", color = c.text, fontSize = 22.sp,
                    fontFamily = CormorantGaramondFamily, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    override fun Gauge(label: String, value: Int, max: Int, color: Color, modifier: Modifier) {
        val c = NirventhalColors
        val safeMax = max.coerceAtLeast(1)
        val fraction = (value.toFloat() / safeMax).coerceIn(0f, 1f)
        Column(modifier = modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = c.muted, fontSize = 12.sp, fontFamily = PhilosopherFamily)
                Text("$value/$max", color = c.text, fontSize = 13.sp,
                    fontFamily = CormorantGaramondFamily, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxWidth().height(8.dp)) {
                val w = size.width; val h = size.height
                // Organic asymmetric track
                drawRoundRect(c.panel, size = size, cornerRadius = CornerRadius(h))
                if (fraction > 0f) {
                    drawRoundRect(color, size = Size(w * fraction, h), cornerRadius = CornerRadius(h))
                }
                drawRoundRect(c.line.copy(alpha = 0.5f), size = size, cornerRadius = CornerRadius(h), style = Stroke(0.8.dp.toPx()))
            }
        }
    }

    @Composable
    override fun ChatBubble(author: String, text: String, mine: Boolean, modifier: Modifier) {
        val c = NirventhalColors
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = c.panel,
                shape = OrganicPanel,
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                    Text("Шёпот корней", color = c.accent, fontSize = 11.sp,
                        fontFamily = PhilosopherFamily, fontStyle = FontStyle.Italic)
                    Spacer(Modifier.height(4.dp))
                    Text("$author:", color = c.hot, fontSize = 13.sp,
                        fontFamily = PhilosopherFamily, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(2.dp))
                    Text(text, color = c.text, fontSize = 14.sp, fontFamily = CormorantGaramondFamily)
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
// 4. КСАНТЕЯ ОБЩАЯ — Астролябия, эфир, кристалл, бирюза
// ═══════════════════════════════════════════════════════════════════════════════

class XanteyaGeneralKit : ThemeKit {

    @Composable
    override fun Die(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier) {
        val c = GeneralXanteyaColors
        val measurer = rememberTextMeasurer()
        Box(
            modifier = modifier
                .size(52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = "Бросить d$n" }
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val w = size.width; val h = size.height
                val cx = w / 2; val cy = h / 2; val r = w * 0.42f
                // Astrolabe ring
                drawCircle(if (active) c.accent else c.panel, radius = r, center = Offset(cx, cy))
                drawCircle(c.accent, radius = r, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
                // Dotted inner orbit
                drawCircle(c.accent.copy(alpha = 0.25f), radius = r * 0.65f, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))
                val label = measurer.measure(
                    "d$n",
                    TextStyle(
                        fontFamily = JuraFamily, fontSize = 13.sp,
                        color = if (active) c.bg else c.text
                    )
                )
                drawText(label, topLeft = Offset((w - label.size.width) / 2f, (h - label.size.height) / 2f))
            }
        }
    }

    @Composable
    override fun StatPlate(label: String, value: Int, onClick: () -> Unit, modifier: Modifier) {
        val c = GeneralXanteyaColors
        Surface(
            modifier = modifier
                .widthIn(min = 92.dp)
                .height(72.dp)
                .clickable(onClick = onClick),
            color = c.panel,
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier.fillMaxSize().padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(label.uppercase(), color = c.muted, fontSize = 11.sp,
                    fontFamily = JuraFamily, letterSpacing = 1.sp)
                Text("$value", color = c.text, fontSize = 22.sp,
                    fontFamily = ForumFamily, fontWeight = FontWeight.Bold)
            }
        }
    }

    @Composable
    override fun Gauge(label: String, value: Int, max: Int, color: Color, modifier: Modifier) {
        val c = GeneralXanteyaColors
        val safeMax = max.coerceAtLeast(1)
        val fraction = (value.toFloat() / safeMax).coerceIn(0f, 1f)
        Column(modifier = modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(label, color = c.muted, fontSize = 12.sp, fontFamily = JuraFamily)
                Text("$value/$max", color = c.text, fontSize = 13.sp,
                    fontFamily = ForumFamily, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Canvas(Modifier.fillMaxWidth().height(10.dp)) {
                val w = size.width; val h = size.height
                drawRoundRect(c.panel, size = size, cornerRadius = CornerRadius(h / 2))
                if (fraction > 0f) {
                    drawRoundRect(color, size = Size(w * fraction, h), cornerRadius = CornerRadius(h / 2))
                }
                drawRoundRect(c.line, size = size, cornerRadius = CornerRadius(h / 2), style = Stroke(1.dp.toPx()))
            }
        }
    }

    @Composable
    override fun ChatBubble(author: String, text: String, mine: Boolean, modifier: Modifier) {
        val c = GeneralXanteyaColors
        Column(
            modifier = modifier.fillMaxWidth(),
            horizontalAlignment = if (mine) Alignment.End else Alignment.Start
        ) {
            Surface(
                color = c.panel,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth(0.85f)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Text("$author:", color = c.accent, fontSize = 13.sp,
                        fontFamily = ForumFamily, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(text, color = c.text, fontSize = 14.sp, fontFamily = JuraFamily)
                }
            }
        }
    }
}

// ─── Top-level Convenience Delegates ──────────────────────────────────────────

@Composable
fun HexDie(
    n: Int,
    active: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    LocalXanteya.current.kit.Die(n = n, active = active, onClick = onClick, modifier = modifier)
}

@Composable
fun StatPlate(
    label: String,
    value: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LocalXanteya.current.kit.StatPlate(label = label, value = value, onClick = onClick, modifier = modifier)
}

@Composable
fun SegmentGauge(
    label: String,
    value: Int,
    max: Int,
    color: Color,
    modifier: Modifier = Modifier
) {
    LocalXanteya.current.kit.Gauge(label = label, value = value, max = max, color = color, modifier = modifier)
}

@Composable
fun AtmosphericChatBubble(
    author: String,
    text: String,
    mine: Boolean,
    modifier: Modifier = Modifier
) {
    LocalXanteya.current.kit.ChatBubble(author = author, text = text, mine = mine, modifier = modifier)
}
