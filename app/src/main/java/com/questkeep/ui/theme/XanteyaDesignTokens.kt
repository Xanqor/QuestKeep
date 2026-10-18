package com.questkeep.ui.theme

import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.questkeep.R

// ─── Font Families (bundled TTFs for offline play at the table) ────────────────

val RussoOneFamily = FontFamily(Font(R.font.russo_one, FontWeight.Normal))
val SpecialEliteFamily = FontFamily(Font(R.font.special_elite, FontWeight.Normal))
val RuslanDisplayFamily = FontFamily(Font(R.font.ruslan_display, FontWeight.Normal))
val PtSerifFamily = FontFamily(Font(R.font.pt_serif, FontWeight.Normal))
val PhilosopherFamily = FontFamily(Font(R.font.philosopher, FontWeight.Normal))
val CormorantGaramondFamily = FontFamily(Font(R.font.cormorant_garamond, FontWeight.Normal))
val ForumFamily = FontFamily(Font(R.font.forum, FontWeight.Normal))
val JuraFamily = FontFamily(Font(R.font.jura, FontWeight.Normal))

// ─── Theme Data Classes ───────────────────────────────────────────────────────

data class XanteyaColors(
    val bg: Color,
    val panel: Color,
    val line: Color,
    val text: Color,
    val muted: Color,
    val accent: Color,
    val hot: Color,
    val chaos: Color = Color(0xFF9B6BFF)
)

data class XanteyaTheme(
    val id: String,
    val name: String,
    val subtitle: String,
    val colors: XanteyaColors,
    val corner: Dp,
    val display: FontFamily,
    val body: FontFamily,
    val kit: ThemeKit
)

// ─── 4 Lore Campaign Themes ───────────────────────────────────────────────────

// 1. Фаллоран («Пламя Революции»): индустриальный гримдарк, медь, клёпаные пластины
val FalloranColors = XanteyaColors(
    bg = Color(0xFF18181A),
    panel = Color(0xFF25231F),
    line = Color(0xFF4A443B),
    text = Color(0xFFE8E2D8),
    muted = Color(0xFF9A9288),
    accent = Color(0xFFC0703A),
    hot = Color(0xFFD9342B)
)

// 2. Цингейт («Штормовой Прилив»): морской нуар, петроль, латунь
val TsingateColors = XanteyaColors(
    bg = Color(0xFF0A1A23),
    panel = Color(0xFF112F39),
    line = Color(0xFFC9A14A),
    text = Color(0xFFE6EFE9),
    muted = Color(0xFF7FA3AF),
    accent = Color(0xFFC9A14A),
    hot = Color(0xFFE8B04A)
)

// 3. Нирвенталь («Шёпот Увядания»): фолк-хоррор, Великое Древо, хвоя, увядающее золото
val NirventhalColors = XanteyaColors(
    bg = Color(0xFF0E1C16),
    panel = Color(0xFF16291F),
    line = Color(0xFFC2A85A),
    text = Color(0xFFE4ECE6),
    muted = Color(0xFF8AA597),
    accent = Color(0xFFC2A85A),
    hot = Color(0xFFA87BE0)
)

// 4. Ксантея (Общая магопанк-тема): космический вакуум, эфир, бирюза, трещина Хаоса
val GeneralXanteyaColors = XanteyaColors(
    bg = Color(0xFF0B0D14),
    panel = Color(0xFF0F1424),
    line = Color(0xFF2A3150),
    text = Color(0xFFE8ECF5),
    muted = Color(0xFF8D97B8),
    accent = Color(0xFF5FD1B0),
    hot = Color(0xFFF3E6B8),
    chaos = Color(0xFF9B6BFF)
)

// Theme instances are created lazily via function to avoid circular init with ThemeKit
fun createFalloranTheme() = XanteyaTheme(
    id = "falloran",
    name = "Фаллоран",
    subtitle = "Пламя Революции",
    colors = FalloranColors,
    corner = 2.dp,
    display = RussoOneFamily,
    body = SpecialEliteFamily,
    kit = FalloranKit()
)

fun createTsingateTheme() = XanteyaTheme(
    id = "tsingate",
    name = "Цингейт",
    subtitle = "Штормовой Прилив",
    colors = TsingateColors,
    corner = 6.dp,
    display = RuslanDisplayFamily,
    body = PtSerifFamily,
    kit = TsingateKit()
)

fun createNirventhalTheme() = XanteyaTheme(
    id = "nirventhal",
    name = "Нирвенталь",
    subtitle = "Шёпот Увядания",
    colors = NirventhalColors,
    corner = 14.dp,
    display = PhilosopherFamily,
    body = CormorantGaramondFamily,
    kit = NirventhalKit()
)

fun createGeneralXanteyaTheme() = XanteyaTheme(
    id = "xanteya",
    name = "Ксантея",
    subtitle = "Планарный Эфир",
    colors = GeneralXanteyaColors,
    corner = 16.dp,
    display = ForumFamily,
    body = JuraFamily,
    kit = XanteyaGeneralKit()
)

val AllXanteyaThemes: List<XanteyaTheme> by lazy {
    listOf(createGeneralXanteyaTheme(), createFalloranTheme(), createTsingateTheme(), createNirventhalTheme())
}

fun getThemeByKey(key: String): XanteyaTheme {
    return AllXanteyaThemes.find { it.id == key } ?: AllXanteyaThemes.first()
}

val LocalXanteya = staticCompositionLocalOf<XanteyaTheme> { createGeneralXanteyaTheme() }

// ─── Custom Atmospheric Shapes ────────────────────────────────────────────────

// Асимметричные панели Нирвенталя
val OrganicPanel = RoundedCornerShape(
    topStart = 48.dp,
    topEnd = 12.dp,
    bottomEnd = 48.dp,
    bottomStart = 12.dp
)

// Рваный край телеграммы Фаллорана
val TornBottom = GenericShape { size, _ ->
    val steps = 24
    val dx = size.width / steps
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width, size.height * 0.95f)
    for (i in steps downTo 1) {
        lineTo(dx * (i - 0.5f), size.height)
        lineTo(dx * (i - 1), size.height * 0.95f)
    }
    close()
}

// Срезанный угол Цингейта (45° cut corners)
val CutCornerShape = GenericShape { size, _ ->
    val cut = size.minDimension * 0.12f
    moveTo(cut, 0f)
    lineTo(size.width - cut, 0f)
    lineTo(size.width, cut)
    lineTo(size.width, size.height - cut)
    lineTo(size.width - cut, size.height)
    lineTo(cut, size.height)
    lineTo(0f, size.height - cut)
    lineTo(0f, cut)
    close()
}
