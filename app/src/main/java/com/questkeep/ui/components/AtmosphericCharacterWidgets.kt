package com.questkeep.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questkeep.domain.entities.Character
import com.questkeep.ui.theme.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Renders the top atmospheric hero header customized for each campaign theme:
 * - Falloran: Industrial hazard stripes + ЦМК Pass title + Bold name in Russo One
 * - Nirventhal: Asymmetric OrganicPanel + Great Tree spiral glyph + Withering leaves
 * - Xanteya: Celestial Sky Arc with 2 suns & 2 moons + Astrolabe aesthetic
 * - Tsingate: Nautical header with compass accent
 */
@Composable
fun AtmosphericHeader(
    character: Character,
    isGm: Boolean,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current

    when (t.id) {
        "falloran" -> {
            Column(modifier = modifier.fillMaxWidth()) {
                HazardStripes(modifier = Modifier.fillMaxWidth().height(14.dp))
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "ПРОПУСК ЦМК № 372-04 · ЦЕХ 4",
                    fontFamily = t.body,
                    fontSize = 11.sp,
                    color = t.colors.accent,
                    letterSpacing = 1.sp
                )
                Text(
                    text = character.name.uppercase(),
                    fontFamily = t.display,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.colors.text
                )
            }
        }
        "nirventhal" -> {
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = OrganicPanel,
                color = t.colors.panel,
                border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    TreeSpiralIcon(modifier = Modifier.size(54.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "НИРВЕНТАЛЬ · 372 г. ПВС",
                            fontFamily = t.display,
                            fontSize = 11.sp,
                            color = t.colors.muted,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = character.name,
                            fontFamily = t.display,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = t.colors.text
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = "Увядание II/V",
                                fontFamily = t.body,
                                fontSize = 12.sp,
                                color = t.colors.muted
                            )
                            WitheringLeaves(stage = 2, max = 5)
                        }
                    }
                }
            }
        }
        "xanteya" -> {
            Column(
                modifier = modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "К С А Н Т Е Я",
                    fontFamily = t.display,
                    fontSize = 22.sp,
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.colors.hot
                )
                Spacer(Modifier.height(4.dp))
                CelestialSkyArc(modifier = Modifier.fillMaxWidth().height(36.dp))
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "СЕЗОН КАСПА · ДЕНЬ 12 ИЗ 55",
                    fontFamily = t.body,
                    fontSize = 10.sp,
                    letterSpacing = 1.5.sp,
                    color = t.colors.muted
                )
            }
        }
        else -> {
            // Tsingate or Default
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = CutCornerShape,
                color = t.colors.panel,
                border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "СУДОВОЙ РЕЕСТР · ПОРТ ЦИНГЕЙТ",
                        fontFamily = t.display,
                        fontSize = 11.sp,
                        color = t.colors.accent,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = character.name,
                        fontFamily = t.display,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.text
                    )
                }
            }
        }
    }
}

/**
 * Diagonal hazard caution stripes for Falloran industrial style
 */
@Composable
fun HazardStripes(modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val stripeW = 12.dp.toPx()
        val numStripes = (w / stripeW).toInt() + 4

        // Background
        drawRect(t.colors.bg)

        // Diagonal hazard stripes
        for (i in -2..numStripes) {
            val startX = i * stripeW * 2
            val path = Path().apply {
                moveTo(startX, 0f)
                lineTo(startX + stripeW, 0f)
                lineTo(startX + stripeW - h, h)
                lineTo(startX - h, h)
                close()
            }
            drawPath(path, t.colors.accent.copy(alpha = 0.85f))
        }
    }
}

/**
 * Concentric tree cross-section logarithmic spiral for Nirventhal
 */
@Composable
fun TreeSpiralIcon(modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Canvas(modifier = modifier) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val maxR = size.minDimension / 2f * 0.9f

        // Draw annual rings
        for (r in listOf(maxR, maxR * 0.75f, maxR * 0.5f, maxR * 0.28f)) {
            drawCircle(
                color = t.colors.accent.copy(alpha = 0.4f),
                radius = r,
                center = Offset(cx, cy),
                style = Stroke(width = 1.2.dp.toPx())
            )
        }

        // Draw inner spiral in hot violet/chaos color
        val spiralPath = Path()
        val turns = 2.5
        val points = 80
        for (i in 0..points) {
            val theta = (i.toDouble() / points) * turns * 2 * PI
            val r = (i.toDouble() / points) * (maxR * 0.6f)
            val x = cx + (r * cos(theta)).toFloat()
            val y = cy + (r * sin(theta)).toFloat()
            if (i == 0) spiralPath.moveTo(x, y) else spiralPath.lineTo(x, y)
        }
        drawPath(spiralPath, t.colors.hot, style = Stroke(width = 1.8.dp.toPx()))
    }
}

/**
 * 5 leaf silhouettes showing Withering stage (e.g. 2 active, 3 outline)
 */
@Composable
fun WitheringLeaves(
    stage: Int,
    max: Int = 5,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        for (i in 1..max) {
            val filled = i <= stage
            Canvas(modifier = Modifier.size(14.dp, 18.dp)) {
                val w = size.width
                val h = size.height
                val leaf = Path().apply {
                    moveTo(w * 0.5f, 0f)
                    cubicTo(w * 1.1f, h * 0.35f, w * 0.9f, h * 0.8f, w * 0.5f, h)
                    cubicTo(w * 0.1f, h * 0.8f, -w * 0.1f, h * 0.35f, w * 0.5f, 0f)
                    close()
                }
                if (filled) {
                    drawPath(leaf, if (i == 1) t.colors.accent else t.colors.line)
                } else {
                    drawPath(leaf, t.colors.panel)
                    drawPath(leaf, t.colors.line.copy(alpha = 0.6f), style = Stroke(1.dp.toPx()))
                }
            }
        }
    }
}

/**
 * Curved celestial sky arc with 2 suns and 2 moons
 */
@Composable
fun CelestialSkyArc(modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Curved arc path
        val arcPath = Path().apply {
            moveTo(w * 0.05f, h * 0.85f)
            quadraticTo(w * 0.5f, h * 0.05f, w * 0.95f, h * 0.85f)
        }
        drawPath(arcPath, t.colors.line.copy(alpha = 0.5f), style = Stroke(1.dp.toPx()))

        // 2 Suns: Amber primary sun & Teal secondary sun
        drawCircle(Color(0xFFF7DE8B), radius = 6.dp.toPx(), center = Offset(w * 0.32f, h * 0.35f))
        drawCircle(Color(0xFF5FD1B0), radius = 4.5.dp.toPx(), center = Offset(w * 0.42f, h * 0.22f))

        // 2 Moons: Lavender moon & Small slate-blue moon
        drawCircle(Color(0xFFD6D1F5), radius = 3.5.dp.toPx(), center = Offset(w * 0.68f, h * 0.38f))
        drawCircle(Color(0xFF8D9EC6), radius = 2.5.dp.toPx(), center = Offset(w * 0.77f, h * 0.56f))
    }
}

/**
 * Astrolabe centerpiece dial showing the prominent roll in the center
 */
@Composable
fun AstrolabeDialHero(
    centerValue: Int = 17,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Box(
        modifier = modifier.size(190.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rOuter = size.minDimension / 2f * 0.95f
            val rMiddle = rOuter * 0.8f
            val rInner = rOuter * 0.55f

            // Outer dial rim
            drawCircle(t.colors.hot.copy(alpha = 0.8f), radius = rOuter, center = Offset(cx, cy), style = Stroke(1.8.dp.toPx()))

            // Radial tick marks
            val ticks = 36
            for (i in 0 until ticks) {
                val angle = (i * 360f / ticks) * (PI / 180f).toFloat()
                val isMajor = i % 3 == 0
                val tickLen = if (isMajor) 7.dp.toPx() else 4.dp.toPx()
                val x1 = cx + (rOuter - tickLen) * cos(angle)
                val y1 = cy + (rOuter - tickLen) * sin(angle)
                val x2 = cx + rOuter * cos(angle)
                val y2 = cy + rOuter * sin(angle)
                drawLine(
                    color = if (isMajor) t.colors.hot else t.colors.line,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Middle orbit
            drawCircle(t.colors.line.copy(alpha = 0.4f), radius = rMiddle, center = Offset(cx, cy), style = Stroke(1.dp.toPx()))

            // Inner glowing circle
            drawCircle(t.colors.accent, radius = rInner, center = Offset(cx, cy), style = Stroke(2.2.dp.toPx()))
        }

        Text(
            text = "$centerValue",
            fontFamily = t.display,
            fontSize = 42.sp,
            fontWeight = FontWeight.Bold,
            color = t.colors.hot
        )
    }
}

/**
 * Atmospheric Hero Roll Result (e.g. big 17 + formula)
 */
@Composable
fun AtmosphericRollHero(
    rollValue: Int,
    formula: String,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "$rollValue",
            fontFamily = t.display,
            fontSize = 54.sp,
            fontWeight = FontWeight.Bold,
            color = t.colors.hot
        )
        Column {
            Text(
                text = formula,
                fontFamily = t.body,
                fontSize = 13.sp,
                color = t.colors.muted
            )
        }
    }
}

/**
 * Themed row of standard dice (d4, d6, d8, d10, d12, d20)
 * Uses LocalXanteya.current.kit.Die procedure!
 */
@Composable
fun AtmosphericDiceRow(
    selectedDie: Int = 20,
    onDieClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dice = listOf(4, 6, 8, 10, 12, 20)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dice.forEach { sides ->
            HexDie(
                n = sides,
                active = selectedDie == sides,
                onClick = { onDieClick(sides) }
            )
        }
    }
}

/**
 * Industrial Recent Action Slip with stamped [ОДОБРЕНО] badge
 */
@Composable
fun RecentActionStampCard(
    title: String = "ПОСЛЕДНЯЯ ЗАЯВКА",
    actionText: String = "Кристальный разряд · 2d6+ВОЛ",
    stampText: String = "ОДОБРЕНО",
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title.uppercase(),
                    fontFamily = t.display,
                    fontSize = 10.sp,
                    color = t.colors.muted,
                    letterSpacing = 1.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = actionText,
                    fontFamily = t.body,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = t.colors.text
                )
            }

            // Angled Stamp
            Box(
                modifier = Modifier
                    .rotate(-4f)
                    .border(1.5.dp, t.colors.hot, RoundedCornerShape(3.dp))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = stampText.uppercase(),
                    color = t.colors.hot,
                    fontFamily = t.display,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

/**
 * Telegram note card with torn bottom edge
 */
@Composable
fun TelegramCard(
    author: String = "ГМ",
    text: String = "Стража на мосту. Видишь фонарь?",
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFE2D6C0), // parchment paper background
        shape = TornBottom
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "ТЕЛЕГРАММА",
                fontFamily = t.display,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF221F1B),
                letterSpacing = 1.5.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "$author: $text",
                fontFamily = t.body,
                fontSize = 14.sp,
                color = Color(0xFF18181A)
            )
        }
    }
}

/**
 * Nirventhal "Шёпот корней" whisper card
 */
@Composable
fun WhisperRootsCard(
    text: String = "Корни шепчут. Кора под рукой тёплая.",
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OrganicPanel,
        color = t.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Шёпот корней",
                fontFamily = t.display,
                fontStyle = FontStyle.Italic,
                fontSize = 12.sp,
                color = t.colors.accent
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = text,
                fontFamily = t.body,
                fontSize = 14.sp,
                color = t.colors.text
            )
        }
    }
}

/**
 * Nirventhal runic stat list with dotted leader lines (ᚦ САМОЧУВСТВИЕ ...... 12)
 */
@Composable
fun NirventhalRunicStatsPanel(
    character: Character,
    onStatClick: (name: String, value: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val runes = listOf(
        Triple("ᚦ", "ТЕЛОСЛОЖЕНИЕ", character.constitution),
        Triple("ᚱ", "ДВИЖЕНИЕ", character.movement),
        Triple("ᛗ", "ВНИМАТЕЛЬНОСТЬ", character.perception),
        Triple("ᛁ", "ИНТЕЛЛЕКТ", character.intelligence),
        Triple("ᚲ", "ВЛИЯНИЕ", character.influence),
        Triple("ᛏ", "ВОЛЯ", character.willpower)
    )
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OrganicPanel,
        color = t.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "АТРИБУТЫ ЛЕСА",
                fontFamily = t.display,
                fontSize = 12.sp,
                color = t.colors.accent,
                letterSpacing = 1.sp,
                fontWeight = FontWeight.Bold
            )
            runes.forEach { (rune, name, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onStatClick(name, value) }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rune,
                        fontFamily = t.display,
                        fontSize = 18.sp,
                        color = t.colors.hot,
                        modifier = Modifier.width(28.dp)
                    )
                    Text(
                        text = name,
                        fontFamily = t.body,
                        fontSize = 13.sp,
                        color = t.colors.text,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = " ................................................................",
                        color = t.colors.line.copy(alpha = 0.7f),
                        maxLines = 1,
                        modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                    )
                    Text(
                        text = if (value >= 0) "+$value" else "$value",
                        fontFamily = t.display,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.accent
                    )
                }
            }
        }
    }
}

/**
 * 1-Tap Quick World Theme Switcher Pill
 */
@Composable
fun ThemeSwitcherPill(
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val context = androidx.compose.ui.platform.LocalContext.current
    var showMenu by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Surface(
            onClick = { showMenu = true },
            color = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.accent.copy(alpha = 0.6f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (t.id) {
                        "falloran" -> "🔥 Фаллоран"
                        "nirventhal" -> "🌲 Нирвенталь"
                        "tsingate" -> "⚓ Цингейт"
                        else -> "🌌 Ксантея"
                    },
                    fontFamily = t.display,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.colors.accent
                )
                Text(
                    text = "▾",
                    color = t.colors.accent,
                    fontSize = 10.sp
                )
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            modifier = Modifier.background(t.colors.panel)
        ) {
            AllXanteyaThemes.forEach { theme ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(
                                text = "${theme.name} — ${theme.subtitle}",
                                fontFamily = theme.display,
                                color = if (theme.id == t.id) theme.colors.accent else theme.colors.text,
                                fontWeight = if (theme.id == t.id) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 13.sp
                            )
                        }
                    },
                    onClick = {
                        showMenu = false
                        com.questkeep.data.local.SessionManager.setGlobalTheme(context, theme.id)
                    }
                )
            }
        }
    }
}

/**
 * Ether Crystal with Chaos fracture for Xanteya
 */
@Composable
fun EtherCrystalIcon(modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Crystal facet path
        val crystalPath = Path().apply {
            moveTo(w * 0.5f, 0f)
            lineTo(w * 0.85f, h * 0.25f)
            lineTo(w * 0.85f, h * 0.95f)
            lineTo(w * 0.5f, h)
            lineTo(w * 0.15f, h * 0.95f)
            lineTo(w * 0.15f, h * 0.25f)
            close()
        }
        drawPath(crystalPath, Color(0xFF16323E))
        drawPath(crystalPath, t.colors.accent, style = Stroke(1.8.dp.toPx()))

        // Internal facets
        drawLine(t.colors.accent.copy(alpha = 0.5f), Offset(w * 0.5f, 0f), Offset(w * 0.5f, h), strokeWidth = 1.dp.toPx())
        drawLine(t.colors.accent.copy(alpha = 0.4f), Offset(w * 0.15f, h * 0.25f), Offset(w * 0.5f, h * 0.45f), strokeWidth = 1.dp.toPx())
        drawLine(t.colors.accent.copy(alpha = 0.4f), Offset(w * 0.85f, h * 0.25f), Offset(w * 0.5f, h * 0.45f), strokeWidth = 1.dp.toPx())

        // Violet Chaos fracture
        val fracture = Path().apply {
            moveTo(w * 0.5f, h * 0.08f)
            lineTo(w * 0.44f, h * 0.32f)
            lineTo(w * 0.58f, h * 0.52f)
            lineTo(w * 0.46f, h * 0.82f)
        }
        drawPath(fracture, t.colors.chaos, style = Stroke(2.dp.toPx()))
    }
}

/**
 * Glowing horizontal attribute progress bars for Xanteya
 */
@Composable
fun XanteyaStatsBarPanel(
    character: Character,
    onStatClick: (name: String, value: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val stats = listOf(
        Triple("ТЕЛ", "Телосложение", character.constitution),
        Triple("ДВИ", "Движение", character.movement),
        Triple("ВНИ", "Внимательность", character.perception),
        Triple("ИНТ", "Интеллект", character.intelligence),
        Triple("ВЛИ", "Влияние", character.influence),
        Triple("ВОЛ", "Воля", character.willpower)
    )
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        stats.forEach { (label, fullName, value) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .clickable { onStatClick(fullName, value) }
                    .padding(vertical = 3.dp, horizontal = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = label,
                    fontFamily = t.display,
                    fontSize = 12.sp,
                    color = t.colors.muted,
                    modifier = Modifier.width(36.dp)
                )
                // Glowing cyan horizontal bar
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(t.colors.panel)
                        .border(1.dp, t.colors.line, RoundedCornerShape(5.dp))
                ) {
                    val safeVal = value.coerceAtLeast(0)
                    val fraction = (safeVal / 20f).coerceIn(0.08f, 1f)
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(fraction)
                            .clip(RoundedCornerShape(5.dp))
                            .background(t.colors.accent)
                    )
                }
                Text(
                    text = if (value >= 0) "+$value" else "$value",
                    fontFamily = t.display,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = t.colors.text,
                    modifier = Modifier.width(28.dp),
                    textAlign = TextAlign.End
                )
            }
        }
    }
}

/**
 * Chronicle card for Xanteya
 */
@Composable
fun ChronicleCard(
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Хроника",
                fontFamily = t.display,
                fontSize = 12.sp,
                color = t.colors.accent,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ГМ Слой реальности дрожит. Эфир гудит в стенах.",
                fontFamily = t.body,
                fontSize = 13.sp,
                color = t.colors.text
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = "Рейн Скрытность: 17",
                fontFamily = t.body,
                fontSize = 12.sp,
                color = t.colors.accent
            )
        }
    }
}

/**
 * Falloran single riveted plate
 */
@Composable
fun FalloranStatPlate(
    label: String,
    value: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val c = FalloranColors
    Box(
        modifier = modifier
            .widthIn(min = 92.dp)
            .height(72.dp)
            .clickable(onClick = onClick)
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            drawRoundRect(c.panel, size = size, cornerRadius = CornerRadius(2.dp.toPx()))
            drawRoundRect(c.line, size = size, cornerRadius = CornerRadius(2.dp.toPx()), style = Stroke(1.5.dp.toPx()))
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
            Text(if (value >= 0) "+$value" else "$value", color = c.text, fontSize = 22.sp,
                fontFamily = RussoOneFamily, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Falloran Die Button
 */
@Composable
fun FalloranDieButton(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    HexDie(n = n, active = active, onClick = onClick, modifier = modifier)
}

/**
 * Falloran 3x2 riveted iron plates grid
 */
@Composable
fun FalloranRivetedStatsGrid(
    character: Character,
    onStatClick: (name: String, value: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val stats = listOf(
        Triple("ТЕЛ", "Телосложение", character.constitution),
        Triple("ДВИ", "Движение", character.movement),
        Triple("ВНИ", "Внимательность", character.perception),
        Triple("ИНТ", "Интеллект", character.intelligence),
        Triple("ВЛИ", "Влияние", character.influence),
        Triple("ВОЛ", "Воля", character.willpower)
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.take(3).forEach { (shortLabel, fullLabel, value) ->
                FalloranStatPlate(
                    label = shortLabel,
                    value = value,
                    onClick = { onStatClick(fullLabel, value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.drop(3).forEach { (shortLabel, fullLabel, value) ->
                FalloranStatPlate(
                    label = shortLabel,
                    value = value,
                    onClick = { onStatClick(fullLabel, value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Falloran discrete 12-segment bar gauge for Health & Aether
 */
@Composable
fun FalloranSegmentedGauge(
    label: String,
    current: Int,
    max: Int,
    barColor: Color,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val safeMax = max.coerceAtLeast(1)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label.uppercase(),
                color = t.colors.muted,
                fontSize = 11.sp,
                fontFamily = SpecialEliteFamily,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "$current/$max",
                color = t.colors.text,
                fontSize = 13.sp,
                fontFamily = RussoOneFamily,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth().height(16.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            val totalSegments = 12
            val filledSegments = (current.toFloat() / safeMax * totalSegments).toInt().coerceIn(0, totalSegments)
            for (i in 0 until totalSegments) {
                val isFilled = i < filledSegments
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(
                            color = if (isFilled) barColor else t.colors.panel,
                            shape = RoundedCornerShape(1.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isFilled) barColor else t.colors.line.copy(alpha = 0.5f),
                            shape = RoundedCornerShape(1.dp)
                        )
                )
            }
        }
    }
}

/**
 * Nirventhal smooth progress bar with rounded cap
 */
@Composable
fun NirventhalLeafGauge(
    label: String,
    current: Int,
    max: Int,
    barColor: Color,
    onTap: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val safeMax = max.coerceAtLeast(1)
    val fraction = (current.toFloat() / safeMax).coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onTap)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "$label $current/$max",
                color = t.colors.text,
                fontSize = 12.sp,
                fontFamily = PhilosopherFamily
            )
        }
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(t.colors.panel)
                .border(1.dp, t.colors.line.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(fraction)
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}

/**
 * Nirventhal Tree Ring Die Button
 */
@Composable
fun NirventhalDieButton(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = NirventhalColors
    Box(
        modifier = modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val cx = w / 2; val cy = h / 2
            for (r in listOf(w * 0.44f, w * 0.34f, w * 0.24f, w * 0.14f)) {
                drawCircle(if (active) c.accent else c.line.copy(alpha = 0.4f), radius = r, center = Offset(cx, cy), style = Stroke(1.2.dp.toPx()))
            }
        }
        Text(
            "d$n",
            fontFamily = CormorantGaramondFamily,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) c.accent else c.text
        )
    }
}

/**
 * Concentric Tree Ring dice row for Nirventhal
 */
@Composable
fun TreeRingDieRow(
    selectedDie: Int = 20,
    onDieClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val dice = listOf(4, 6, 8, 10, 12, 20)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = OrganicPanel,
        color = t.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dice.forEach { sides ->
                NirventhalDieButton(
                    n = sides,
                    active = sides == selectedDie,
                    onClick = { onDieClick(sides) }
                )
            }
        }
    }
}

/**
 * Tsingate Card Stat Plate
 */
@Composable
fun TsingateStatPlateView(
    label: String,
    value: Int,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
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
                Text(label.uppercase(), color = c.muted, fontSize = 11.sp, fontFamily = PtSerifFamily)
                Text(if (value >= 0) "+$value" else "$value", color = c.text, fontSize = 22.sp, fontFamily = RuslanDisplayFamily, fontWeight = FontWeight.Bold)
            }
        }
    }
}

/**
 * Tsingate CutCornerShape stats grid
 */
@Composable
fun TsingateStatsGrid(
    character: Character,
    onStatClick: (name: String, value: Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val stats = listOf(
        Triple("ТЕЛ", "Телосложение", character.constitution),
        Triple("ДВИ", "Движение", character.movement),
        Triple("ВНИ", "Внимательность", character.perception),
        Triple("ИНТ", "Интеллект", character.intelligence),
        Triple("ВЛИ", "Влияние", character.influence),
        Triple("ВОЛ", "Воля", character.willpower)
    )
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.take(3).forEach { (shortLabel, fullLabel, value) ->
                TsingateStatPlateView(
                    label = shortLabel,
                    value = value,
                    onClick = { onStatClick(fullLabel, value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            stats.drop(3).forEach { (shortLabel, fullLabel, value) ->
                TsingateStatPlateView(
                    label = shortLabel,
                    value = value,
                    onClick = { onStatClick(fullLabel, value) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * Tsingate Compass Die Button
 */
@Composable
fun TsingateDieButton(n: Int, active: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = TsingateColors
    Box(
        modifier = modifier
            .size(52.dp)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            val cx = w / 2; val cy = h / 2; val r = w * 0.42f
            drawCircle(if (active) c.accent else c.panel, radius = r, center = Offset(cx, cy))
            drawCircle(c.accent, radius = r, center = Offset(cx, cy), style = Stroke(2.dp.toPx()))
        }
        Text(
            "d$n",
            fontFamily = PtSerifFamily,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = if (active) c.bg else c.text
        )
    }
}

/**
 * Tsingate compass clock dice row
 */
@Composable
fun TsingateCompassDieRow(
    selectedDie: Int = 20,
    onDieClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val dice = listOf(4, 6, 8, 10, 12, 20)
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = CutCornerShape,
        color = LocalXanteya.current.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, LocalXanteya.current.colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dice.forEach { sides ->
                TsingateDieButton(
                    n = sides,
                    active = sides == selectedDie,
                    onClick = { onDieClick(sides) }
                )
            }
        }
    }
}
