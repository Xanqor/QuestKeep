package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.Spell
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.CharacterSheetViewModel
import com.questkeep.viewmodel.SpellCastResult
import com.questkeep.viewmodel.SpellViewModel
import kotlinx.coroutines.flow.collectLatest
import java.util.UUID

// ═══════════════════════════════════════════════════════════════════════════
//  XANTHEIA MAGIC SYSTEM — Internal UI Models
// ═══════════════════════════════════════════════════════════════════════════

private data class CoreOption(
    val id: String, val name: String,
    val costP: Float, val costE: Float
)

private data class ModOption(
    val id: String, val name: String,
    val costP: Float, val costE: Float,
    val requiresCoreId: String? = null
)

private data class ExpressionOption(val id: String, val name: String, val slots: Int)

private enum class AccentType { MULTIPLIER, COST, ADDITION }

private data class AccentLevel(
    val value: String,
    val description: String,
    val multiplier: Int = 0,
    val addition: Float = 0f,
    val costP: Float = 0f,
    val costE: Float = 0f
)

private data class AccentOption(
    val id: String,
    val name: String,
    val type: AccentType,
    val levels: List<AccentLevel>
)

private data class SpellCalcResult(
    val posP: Float = 0f,
    val negP: Float = 0f,
    val posE: Float = 0f,
    val negE: Float = 0f,
    val multiplier: Int = 1
)

// ═══════════════════════════════════════════════════════════════════════════
//  STATIC DATA  (mirrors xantheia_calculator.html exactly)
// ═══════════════════════════════════════════════════════════════════════════

private val CORES = listOf(
    CoreOption("water",         "Вода",                  0.5f,  0f   ),
    CoreOption("fire",          "Огонь",                 -0.5f, 1f   ),
    CoreOption("ice",           "Лёд",                   0.5f,  -1f  ),
    CoreOption("air",           "Воздух",                -0.5f, 0f   ),
    CoreOption("ambrosia",      "Поток амброза",         0f,    0f   ),
    CoreOption("illusion",      "Иллюзия",               -1f,   0f   ),
    CoreOption("darkness",      "Тьма",                  -1f,   -2f  ),
    CoreOption("electricity",   "Электричество",         1f,    2f   ),
    CoreOption("earth",         "Земля/камень",          1f,    0f   ),
    CoreOption("crystals",      "Кристаллы",             2f,    0f   ),
    CoreOption("transmutation", "Трансмутация",          3f,    0f   ),
    CoreOption("radiation",     "Излучение",             0f,    3f   ),
    CoreOption("gravity",       "Гравитация",            0f,    -3f  ),
    CoreOption("summon",        "Призыв",                -2f,   0f   ),
    CoreOption("teleport",      "Телепорт",              -3f,   0f   ),
)

private val MODIFICATIONS = listOf(
    ModOption("none",       "Без модификации",              0f,    0f,    null        ),
    ModOption("healing",    "Лечение",                      0.5f,  0.5f,  "water"     ),
    ModOption("poison",     "Яд",                           0f,    -0.5f, "water"     ),
    ModOption("acid",       "Кислота",                      -0.5f, -0.5f, "water"     ),
    ModOption("vegetation", "Растительность",               0.5f,  0f,    "water"     ),
    ModOption("lava",       "Лава",                         0f,    0.5f,  "earth"     ),
    ModOption("laser",      "Лазер",                        1f,    0f,    "electricity"),
    ModOption("sound",      "Звук",                         0f,    0.5f,  "air"       ),
    ModOption("metall",     "Металл",                       0.5f,  0f,    "earth"     ),
    ModOption("plasm",      "Плазма",                       0.5f,  0.5f,  "fire"      ),
    ModOption("smoke",      "Дым",                          0.5f,  0f,    "air"       ),
    ModOption("psycho",     "Психол. воздействие",          0f,    -1f,   "illusion"  ),
)

private val EXPRESSIONS = listOf(
    ExpressionOption("touch",       "Касание",         2),
    ExpressionOption("release",     "Высвобождение",   3),
    ExpressionOption("filling",     "Наполнение",      3),
    ExpressionOption("projectile4", "Снаряд (4 яч.)", 4),
    ExpressionOption("projectile5", "Снаряд (5 яч.)", 5),
    ExpressionOption("area",        "Область",         6),
)

private val ACCENTS_BY_EXPRESSION: Map<String, List<AccentOption>> = mapOf(
    "touch" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("duration", "Время действия", AccentType.MULTIPLIER, listOf(
            AccentLevel("0",  "Мгновенное применение", multiplier = 0),
            AccentLevel("x1", "3 хода применения",     multiplier = 1),
        )),
    ),
    "release" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("range", "Дальность", AccentType.COST, listOf(
            AccentLevel("0",    "3 клетки", costP = 0f, costE = 0f),
            AccentLevel("+1 п", "4 клетки", costP = 1f, costE = 0f),
            AccentLevel("+2 п", "5 клеток", costP = 2f, costE = 0f),
        )),
        AccentOption("angle", "Угол (схема распыления)", AccentType.ADDITION, listOf(
            AccentLevel("0",    "Равномерно по конусу (сила -1)",       addition = 0f ),
            AccentLevel("-1 э", "Центр без потери сил (луч + область)", addition = -1f),
            AccentLevel("+1 п", "Край без потери сил",                  addition = 1f ),
        )),
    ),
    "filling" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("castSpeed", "Скорость наложения", AccentType.COST, listOf(
            AccentLevel("0",    "6 ОД на выполнение", costP = 0f, costE = 0f),
            AccentLevel("+1 э", "4 ОД на выполнение", costP = 0f, costE = 1f),
            AccentLevel("+2 э", "2 ОД на выполнение", costP = 0f, costE = 2f),
        )),
        AccentOption("volume", "Объём", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "Небольшой предмет / часть тела",  multiplier = 0),
            AccentLevel("x1", "Большой предмет / половина тела", multiplier = 1),
            AccentLevel("x2", "Крупный предмет / всё тело",      multiplier = 2),
        )),
    ),
    "projectile4" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("speed", "Скорость", AccentType.ADDITION, listOf(
            AccentLevel("-0.5 п", "6 клеток в ход",  addition = -0.5f),
            AccentLevel("-1 п",   "12 клеток в ход", addition = -1f  ),
            AccentLevel("-2 п",   "24 клетки в ход", addition = -2f  ),
        )),
        AccentOption("range", "Дальность", AccentType.ADDITION, listOf(
            AccentLevel("0",    "6 клеток",  addition = 0f),
            AccentLevel("+1 п", "12 клеток", addition = 1f),
            AccentLevel("+2 п", "24 клетки", addition = 2f),
        )),
        AccentOption("size", "Размер снаряда", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "1 клетка",                multiplier = 0),
            AccentLevel("x1", "3x3 (по краям -1 сила)",  multiplier = 1),
            AccentLevel("x2", "5x5 (по краям -1 сила)",  multiplier = 2),
        )),
    ),
    "projectile5" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("speed", "Скорость", AccentType.ADDITION, listOf(
            AccentLevel("-0.5 п", "6 клеток в ход",  addition = -0.5f),
            AccentLevel("-1 п",   "12 клеток в ход", addition = -1f  ),
            AccentLevel("-2 п",   "24 клетки в ход", addition = -2f  ),
        )),
        AccentOption("range", "Дальность", AccentType.ADDITION, listOf(
            AccentLevel("0",    "6 клеток",  addition = 0f),
            AccentLevel("+1 п", "12 клеток", addition = 1f),
            AccentLevel("+2 п", "24 клетки", addition = 2f),
        )),
        AccentOption("size", "Размер снаряда", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "1 клетка",               multiplier = 0),
            AccentLevel("x1", "3x3 (по краям -1 сила)", multiplier = 1),
            AccentLevel("x2", "5x5 (по краям -1 сила)", multiplier = 2),
        )),
        AccentOption("count", "Количество снарядов", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "1 снаряд",   multiplier = 0),
            AccentLevel("x1", "2 снаряда",  multiplier = 1),
            AccentLevel("x2", "3 снаряда",  multiplier = 2),
            AccentLevel("x3", "4 снаряда",  multiplier = 3),
            AccentLevel("x4", "5 снарядов", multiplier = 4),
        )),
    ),
    "area" to listOf(
        AccentOption("power", "Сила", AccentType.MULTIPLIER, listOf(
            AccentLevel("x1", "Базовые эффекты",      multiplier = 1),
            AccentLevel("x2", "Усиленные эффекты",    multiplier = 2),
            AccentLevel("x3", "Максимальные эффекты", multiplier = 3),
        )),
        AccentOption("size", "Размер", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "3x3", multiplier = 0),
            AccentLevel("x1", "5x5", multiplier = 1),
            AccentLevel("x2", "7x7", multiplier = 2),
        )),
        AccentOption("range", "Дальность", AccentType.COST, listOf(
            AccentLevel("0",    "На себя",  costP = 0f, costE = 0f),
            AccentLevel("+1 п", "3 клетки", costP = 1f, costE = 0f),
            AccentLevel("+2 п", "6 клеток", costP = 2f, costE = 0f),
        )),
        AccentOption("duration", "Время действия", AccentType.MULTIPLIER, listOf(
            AccentLevel("x0", "Мгновенное применение",        multiplier = 0),
            AccentLevel("x1", "3 хода применения (сила -1)",  multiplier = 1),
        )),
        AccentOption("castSpeed", "Скорость", AccentType.ADDITION, listOf(
            AccentLevel("0",    "9 ОД на выполнение", addition = 0f),
            AccentLevel("+1 э", "6 ОД на выполнение", addition = 1f),
            AccentLevel("+2 э", "3 ОД на выполнение", addition = 2f),
        )),
        AccentOption("distribution", "Распределение", AccentType.ADDITION, listOf(
            AccentLevel("0",    "Равномерно (-1 сила)",  addition = 0f ),
            AccentLevel("-1 э", "Центр без потери силы", addition = -1f),
            AccentLevel("+1 п", "Края без потери силы",  addition = 1f ),
        )),
    ),
)

private val CORE_EFFECTS: Map<String, List<Pair<Int, String>>> = mapOf(
    "fire"          to listOf(0 to "Небольшой нагрев (без урона)", 1 to "Сильный жар (2 урона)", 2 to "Сильный жар (4 урона)", 3 to "Сильный жар (6 урона)"),
    "ice"           to listOf(0 to "Небольшое охлаждение (без урона)", 1 to "Сильный холод (2 урона)", 2 to "Сильный холод (4 урона)", 3 to "Сильный холод (6 урона)"),
    "electricity"   to listOf(0 to "2 урона", 1 to "5 урона", 2 to "10 урона, помеха на действие 2 хода", 3 to "15 урона, оглушение на 1 ход"),
    "darkness"      to listOf(0 to "Поглощение звука", 1 to "Ослепление на 3 хода", 2 to "100% оглушение на 2 хода", 3 to "10 урона, высасывание 3 каждого аспекта"),
    "water"         to listOf(0 to "Водяной пар / поток воздуха", 1 to "Отталкивание на 1 клетку", 2 to "Отталкивание на 3 клетки", 3 to "Отталкивание на 5 клеток"),
    "air"           to listOf(0 to "Поток воздуха", 1 to "Ускорение на 2 кл/ход", 2 to "Ускорение на 4 кл/ход", 3 to "Ускорение на 5 кл/ход"),
    "earth"         to listOf(0 to "Песок", 1 to "Хрупкий камень (2 урона/2 хп)", 2 to "Крепкий камень (4 урона/8 хп)", 3 to "Твёрдый камень (6 урона/20 хп)"),
    "illusion"      to listOf(0 to "Рассеивающаяся иллюзия", 1 to "Стабильная иллюзия (1 хп)", 2 to "Устойчивая иллюзия", 3 to "Осязаемая иллюзия"),
    "ambrosia"      to listOf(0 to "Скорость передачи x0.5", 1 to "Скорость передачи x1", 2 to "Скорость передачи x2", 3 to "Скорость передачи x3"),
    "crystals"      to listOf(0 to "Мелкие кристаллические осколки", 1 to "Формирование кристаллов", 2 to "Крупные кристаллы", 3 to "Массивные кристаллы"),
    "transmutation" to listOf(0 to "Неудача", 1 to "Базовая трансмутация", 2 to "Средняя трансмутация", 3 to "Полная трансмутация"),
    "radiation"     to listOf(0 to "Слабое излучение", 1 to "Базовое излучение", 2 to "Среднее излучение", 3 to "Сильное излучение"),
    "gravity"       to listOf(0 to "Слабое воздействие", 1 to "Базовое изменение гравитации", 2 to "Среднее изменение гравитации", 3 to "Сильное изменение гравитации"),
    "summon"        to listOf(0 to "Неудача", 1 to "Слабый призыв", 2 to "Средний призыв", 3 to "Сильный призыв"),
    "teleport"      to listOf(0 to "Неудача", 1 to "Короткий телепорт", 2 to "Средний телепорт", 3 to "Дальний телепорт"),
)

// ═══════════════════════════════════════════════════════════════════════════
//  CALCULATION FUNCTIONS
// ═══════════════════════════════════════════════════════════════════════════

private fun calculateCost(
    core: CoreOption,
    mod: ModOption,
    expression: ExpressionOption,
    accents: Map<String, Int>
): SpellCalcResult {
    // 1. Base: core + mod (split positive/negative)
    var basePosP = 0f; var baseNegP = 0f
    var basePosE = 0f; var baseNegE = 0f
    for ((cp, ce) in listOf(core.costP to core.costE, 0f to 0f, mod.costP to mod.costE)) {
        if (cp > 0f) basePosP += cp else baseNegP += cp
        if (ce > 0f) basePosE += ce else baseNegE += ce
    }

    // 2. Accents: multiplier / cost / addition
    var totalMultiplier = 0
    var costPosP = 0f; var costNegP = 0f; var costPosE = 0f; var costNegE = 0f
    var addPosP = 0f; var addNegP = 0f; var addPosE = 0f; var addNegE = 0f

    for (accent in ACCENTS_BY_EXPRESSION[expression.id] ?: emptyList()) {
        val idx = accents[accent.id] ?: 0
        val level = accent.levels.getOrNull(idx) ?: continue
        when (accent.type) {
            AccentType.MULTIPLIER -> totalMultiplier += level.multiplier
            AccentType.COST -> {
                if (level.costP > 0f) costPosP += level.costP else costNegP += level.costP
                if (level.costE > 0f) costPosE += level.costE else costNegE += level.costE
            }
            AccentType.ADDITION -> {
                val isEnergy = level.value.contains('э')
                if (isEnergy) {
                    if (level.addition > 0f) addPosE += level.addition else addNegE += level.addition
                } else {
                    if (level.addition > 0f) addPosP += level.addition else addNegP += level.addition
                }
            }
        }
    }

    val finalMult = if (totalMultiplier == 0) 1 else totalMultiplier
    return SpellCalcResult(
        posP = (basePosP + costPosP) * finalMult + addPosP,
        negP = (baseNegP + costNegP) * finalMult + addNegP,
        posE = (basePosE + costPosE) * finalMult + addPosE,
        negE = (baseNegE + costNegE) * finalMult + addNegE,
        multiplier = finalMult
    )
}

private fun getPowerLevel(expressionId: String, accents: Map<String, Int>): Int {
    val accent = ACCENTS_BY_EXPRESSION[expressionId]?.find { it.id == "power" } ?: return 1
    val idx = accents["power"] ?: 0
    return accent.levels.getOrNull(idx)?.multiplier ?: 1
}

private fun getEffect(coreId: String, powerLevel: Int): String {
    val effects = CORE_EFFECTS[coreId] ?: return "Эффект определяется ядром"
    return effects.find { it.first == powerLevel }?.second
        ?: effects.lastOrNull()?.second
        ?: "Эффект не определён"
}

/** "power=0;range=1;size=2" */
private fun Map<String, Int>.toAccentsString(): String =
    entries.joinToString(";") { "${it.key}=${it.value}" }

/** Format a float without trailing .0 */
private fun fmtF(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)

/** Short cost string for a core card (+0.5п / -1э) */
private fun coreCostStr(c: CoreOption): String {
    val parts = mutableListOf<String>()
    if (c.costP != 0f) parts += "${if (c.costP > 0f) "+" else ""}${fmtF(c.costP)}п"
    if (c.costE != 0f) parts += "${if (c.costE > 0f) "+" else ""}${fmtF(c.costE)}э"
    return if (parts.isEmpty()) "0" else parts.joinToString(" / ")
}

/** Short cost string for a mod card */
private fun modCostStr(m: ModOption): String {
    val parts = mutableListOf<String>()
    if (m.costP != 0f) parts += "${if (m.costP > 0f) "+" else ""}${fmtF(m.costP)}п"
    if (m.costE != 0f) parts += "${if (m.costE > 0f) "+" else ""}${fmtF(m.costE)}э"
    return if (parts.isEmpty()) "0" else parts.joinToString(" / ")
}

// ═══════════════════════════════════════════════════════════════════════════
//  MAIN COMPOSABLE
// ═══════════════════════════════════════════════════════════════════════════

/** Magic tab shown inside CharacterSheetScreen as the second tab. */
@Composable
fun MagicTab(
    character: Character,
    spellViewModel: SpellViewModel = hiltViewModel(),
    characterViewModel: CharacterSheetViewModel = hiltViewModel()
) {
    LaunchedEffect(character.id) { spellViewModel.loadSpells(character.id) }

    val savedSpells by spellViewModel.savedSpells.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedTab by remember { mutableIntStateOf(0) }

    val context = LocalContext.current
    val spellExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = spellViewModel.exportSpellsJson()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    val spellImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
        spellViewModel.importSpellsFromJson(json)
    }

    LaunchedEffect(Unit) {
        spellViewModel.castResult.collectLatest { result ->
            val msg = when (result) {
                is SpellCastResult.Success              -> "✨ Заклинание применено!"
                is SpellCastResult.NotEnoughResources   -> "❌ Не хватает: ${result.missing}"
            }
            snackbarHostState.showSnackbar(msg)
        }
    }

    val t = LocalXanteya.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    var isResourcesExpanded by rememberSaveable { mutableStateOf(!isLandscape) }

    Scaffold(
        containerColor = t.colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {

            // ── Magic resource bars (collapsible, compact in landscape) ───
            MagicResourcesSection(
                character = character,
                onDeltaChange = { name, delta -> characterViewModel.updateResource(name, delta) },
                isExpanded = isResourcesExpanded,
                onToggleExpand = { isResourcesExpanded = !isResourcesExpanded }
            )

            HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))

            // ── Tab row: Constructor / Saved spells ──────────────────────
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = t.colors.panel,
                contentColor = t.colors.accent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("⚗ Конструктор", fontFamily = t.display, fontSize = 13.sp, color = if (selectedTab == 0) t.colors.accent else t.colors.muted) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("📖 Заклинания (${savedSpells.size})", fontFamily = t.display, fontSize = 13.sp, color = if (selectedTab == 1) t.colors.accent else t.colors.muted) })
            }

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (selectedTab) {
                    0 -> XantheiaConstructorTab(
                        character = character,
                        onCast = { spell -> spellViewModel.castSpell(spell, character) },
                        onSave = { spell -> spellViewModel.saveSpell(spell.copy(characterId = character.id)) }
                    )
                    1 -> SavedSpellsTab(
                        spells = savedSpells,
                        character = character,
                        onCast = { spell -> spellViewModel.castSpell(spell, character) },
                        onDelete = { spell -> spellViewModel.deleteSpell(spell) },
                        onExport = { spellExportLauncher.launch("spells.json") },
                        onImport = { spellImportLauncher.launch(arrayOf("application/json")) }
                    )
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  MAGIC RESOURCES SECTION
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun MagicResourcesSection(
    character: Character,
    onDeltaChange: (String, Float) -> Unit,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .clickable { onToggleExpand() }
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    "МАГИЧЕСКИЕ РЕСУРСЫ",
                    style = MaterialTheme.typography.labelMedium,
                    color = t.colors.accent,
                    fontFamily = t.display,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp
                )
                if (!isExpanded) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactResourceBadge("К", character.quarResource, ResourceQuarColor)
                        CompactResourceBadge("О", character.ombraResource, ResourceOmbraColor)
                        CompactResourceBadge("В", character.verunaResource, ResourceVerunaColor)
                        CompactResourceBadge("С", character.sumeniResource, ResourceSumeniColor)
                    }
                }
            }
            Icon(
                imageVector = if (isExpanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                contentDescription = if (isExpanded) "Свернуть" else "Развернуть",
                tint = t.colors.muted,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MagicResourceRow("+п", "Куар",   "quar",   character.quarResource,   character.maxQuarResource,   ResourceQuarColor,   onDeltaChange)
                MagicResourceRow("-п", "Омбра",  "ombra",  character.ombraResource,  character.maxOmbraResource,  ResourceOmbraColor,  onDeltaChange)
                MagicResourceRow("+э", "Веруна", "veruna", character.verunaResource, character.maxVerunaResource, ResourceVerunaColor, onDeltaChange)
                MagicResourceRow("-э", "Сумени", "sumeni", character.sumeniResource, character.maxSumeniResource, ResourceSumeniColor, onDeltaChange)
            }
        }
    }
}

@Composable
private fun CompactResourceBadge(label: String, value: Float, color: Color) {
    val fmt: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(label, color = color, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            Text(fmt(value), color = color, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MagicResourceRow(
    symbol: String, name: String, key: String,
    current: Float, max: Float, color: Color,
    onDeltaChange: (String, Float) -> Unit
) {
    val t = LocalXanteya.current
    val fmt: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    var showEdit by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(symbol, color = color, fontFamily = t.display, fontSize = 10.sp, fontWeight = FontWeight.Bold)
        }
        Text(name, color = t.colors.text, modifier = Modifier.width(50.dp),
            fontFamily = t.body,
            style = MaterialTheme.typography.bodySmall)

        Box(
            modifier = Modifier.weight(1f).height(12.dp)
                .background(t.colors.panel, RoundedCornerShape(6.dp))
                .border(1.dp, t.colors.line, RoundedCornerShape(6.dp))
        ) {
            val frac = if (max > 0f) (current / max).coerceIn(0f, 1f) else 0f
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).background(color, RoundedCornerShape(6.dp)))
        }

        Text(
            text = "${fmt(current)}/${fmt(max)}",
            color = color, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
            modifier = Modifier
                .width(56.dp)
                .combinedClickable(onClick = {}, onLongClick = { showEdit = true })
        )

        IconButton(onClick = { onDeltaChange(key, -0.5f) }, modifier = Modifier.size(28.dp)) {
            Text("−", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
        IconButton(onClick = { onDeltaChange(key, +0.5f) }, modifier = Modifier.size(28.dp)) {
            Text("+", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }
    }

    if (showEdit) {
        ResourceEditDialog(
            name = name, current = current, max = max, color = color, fmt = fmt,
            onSetValue = { v -> onDeltaChange(key, v - current); showEdit = false },
            onDismiss = { showEdit = false }
        )
    }
}

@Composable
private fun ResourceEditDialog(
    name: String, current: Float, max: Float, color: Color,
    fmt: (Float) -> String,
    onSetValue: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val t = LocalXanteya.current
    var text by remember { mutableStateOf(fmt(current)) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        title = { Text("Изменить: $name", color = color, fontFamily = t.display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Текущее: ${fmt(current)} / ${fmt(max)}", color = t.colors.muted, fontFamily = t.body, fontSize = 12.sp)
                OutlinedTextField(
                    value = text, onValueChange = { text = it },
                    label = { Text("Новое значение (0 — ${fmt(max)})", color = t.colors.muted, fontFamily = t.body) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = color, unfocusedBorderColor = t.colors.line,
                        focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
                        focusedLabelColor = color
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val v = text.replace(',', '.').toFloatOrNull()?.coerceIn(0f, max)
                    if (v != null) onSetValue(v)
                },
                colors = ButtonDefaults.buttonColors(containerColor = color)
            ) { Text("Сохранить", fontFamily = t.display, color = Color.White) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

// ═══════════════════════════════════════════════════════════════════════════
//  XANTHEIA CONSTRUCTOR  (4-step wizard)
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun XantheiaConstructorTab(
    character: Character,
    onCast: (Spell) -> Unit,
    onSave: (Spell) -> Unit
) {
    var currentStep by remember { mutableIntStateOf(1) }
    var selectedCore by remember { mutableStateOf<CoreOption?>(null) }
    var selectedMod by remember { mutableStateOf(MODIFICATIONS[0]) }
    var selectedExpression by remember { mutableStateOf<ExpressionOption?>(null) }
    var selectedAccents by remember { mutableStateOf(mapOf<String, Int>()) }
    var spellName by remember { mutableStateOf("") }

    // Derived values (cheap to compute every recomposition)
    val calcResult: SpellCalcResult? = if (selectedCore != null && selectedExpression != null)
        calculateCost(selectedCore!!, selectedMod, selectedExpression!!, selectedAccents)
    else null

    val powerLevel = getPowerLevel(selectedExpression?.id ?: "", selectedAccents)
    val effectText = selectedCore?.let { getEffect(it.id, powerLevel) } ?: ""

    // Resource check for the Cast button
    val quarNeeded  = calcResult?.posP?.coerceAtLeast(0f) ?: 0f
    val ombraNeeded = (-(calcResult?.negP ?: 0f)).coerceAtLeast(0f)
    val verunaNeeded = calcResult?.posE?.coerceAtLeast(0f) ?: 0f
    val sumeniNeeded = (-(calcResult?.negE ?: 0f)).coerceAtLeast(0f)
    val totalCost = quarNeeded + ombraNeeded + verunaNeeded + sumeniNeeded
    val canCast = totalCost > 0f &&
            character.quarResource  >= quarNeeded  &&
            character.ombraResource >= ombraNeeded &&
            character.verunaResource >= verunaNeeded &&
            character.sumeniResource >= sumeniNeeded

    // Build the Spell domain object from current wizard state
    fun buildSpell() = Spell(
        id = UUID.randomUUID().toString(),
        characterId = character.id,
        name = spellName.trim(),
        coreId = selectedCore?.id ?: "",
        coreName = selectedCore?.name ?: "",
        modId = selectedMod.id,
        modName = if (selectedMod.id == "none") "" else selectedMod.name,
        expressionId = selectedExpression?.id ?: "",
        expressionName = selectedExpression?.name ?: "",
        accentsJson = selectedAccents.toAccentsString(),
        powerLevel = powerLevel,
        effectText = effectText,
        quarCost  = quarNeeded,
        ombraCost = ombraNeeded,
        verunaCost = verunaNeeded,
        sumeniCost = sumeniNeeded
    )

    fun resetAll() {
        currentStep = 1
        selectedCore = null
        selectedMod = MODIFICATIONS[0]
        selectedExpression = null
        selectedAccents = mapOf()
        spellName = ""
    }

    Column(modifier = Modifier.fillMaxSize()) {
        StepIndicatorRow(currentStep)
        when (currentStep) {
            1 -> CoreGridStep(
                selectedCoreId = selectedCore?.id,
                onSelect = { core ->
                    selectedCore = core
                    selectedMod = MODIFICATIONS[0]
                    currentStep = 2
                }
            )
            2 -> ModGridStep(
                selectedCore = selectedCore,
                selectedModId = selectedMod.id,
                onSelect = { mod ->
                    selectedMod = mod
                    currentStep = 3
                },
                onBack = { currentStep = 1 }
            )
            3 -> ExpressionGridStep(
                selectedExprId = selectedExpression?.id,
                onSelect = { expr ->
                    selectedExpression = expr
                    selectedAccents = mapOf()
                    currentStep = 4
                },
                onBack = { currentStep = 2 }
            )
            4 -> AccentsStep(
                core = selectedCore,
                mod = selectedMod,
                expression = selectedExpression,
                accents = selectedAccents,
                calcResult = calcResult,
                powerLevel = powerLevel,
                effectText = effectText,
                spellName = spellName,
                canCast = canCast,
                onAccentChange = { accentId, idx ->
                    selectedAccents = selectedAccents + (accentId to idx)
                },
                onSpellNameChange = { spellName = it },
                onCast  = { onCast(buildSpell())  },
                onSave  = { onSave(buildSpell())  },
                onBack  = { currentStep = 3 },
                onReset = { resetAll() }
            )
        }
    }
}

// ── Step indicator ────────────────────────────────────────────────────────

@Composable
private fun StepIndicatorRow(currentStep: Int) {
    val t = LocalXanteya.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (step in 1..4) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(
                        when {
                            step < currentStep  -> t.colors.accent
                            step == currentStep -> t.colors.hot
                            else                -> t.colors.panel
                        },
                        CircleShape
                    )
                    .border(
                        1.dp,
                        when {
                            step < currentStep  -> t.colors.accent
                            step == currentStep -> t.colors.hot
                            else                -> t.colors.line
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (step < currentStep) "✓" else step.toString(),
                    color = if (step <= currentStep) t.colors.bg else t.colors.muted,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            }
            if (step < 4) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .background(if (step < currentStep) t.colors.accent else t.colors.line)
                )
            }
        }
    }
}

// ── Step 1: Core ─────────────────────────────────────────────────────────

@Composable
private fun CoreGridStep(
    selectedCoreId: String?,
    onSelect: (CoreOption) -> Unit
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ШАГ 1: ВЫБОР ЯДРА", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)

        CORES.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { core ->
                    SelectionCard(
                        title = core.name,
                        subtitle = coreCostStr(core),
                        selected = core.id == selectedCoreId,
                        onClick = { onSelect(core) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ── Step 2: Modification ─────────────────────────────────────────────────

@Composable
private fun ModGridStep(
    selectedCore: CoreOption?,
    selectedModId: String,
    onSelect: (ModOption) -> Unit,
    onBack: () -> Unit
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ШАГ 2: МОДИФИКАЦИЯ", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)

        MODIFICATIONS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { mod ->
                    val allowed = mod.id == "none" ||
                            (selectedCore != null && mod.requiresCoreId == selectedCore.id)
                    val note = if (!allowed && mod.requiresCoreId != null)
                        "Только для: ${CORES.find { it.id == mod.requiresCoreId }?.name ?: ""}"
                    else ""
                    SelectionCard(
                        title    = mod.name,
                        subtitle = modCostStr(mod),
                        note     = note,
                        selected = mod.id == selectedModId && allowed,
                        disabled = !allowed,
                        onClick  = { if (allowed) onSelect(mod) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.muted),
                border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
            ) { Text("← Назад", fontFamily = t.display) }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ── Step 3: Expression ───────────────────────────────────────────────────

@Composable
private fun ExpressionGridStep(
    selectedExprId: String?,
    onSelect: (ExpressionOption) -> Unit,
    onBack: () -> Unit
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("ШАГ 3: ВЫРАЖЕНИЕ", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)

        EXPRESSIONS.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { expr ->
                    SelectionCard(
                        title    = expr.name,
                        subtitle = "${expr.slots} ячеек",
                        selected = expr.id == selectedExprId,
                        onClick  = { onSelect(expr) },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp)) {
            OutlinedButton(
                onClick = onBack, modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.muted),
                border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
            ) { Text("← Назад", fontFamily = t.display) }
        }
        Spacer(Modifier.height(100.dp))
    }
}

// ── Step 4: Accents + Result + Save ──────────────────────────────────────

@Composable
private fun AccentsStep(
    core: CoreOption?,
    mod: ModOption,
    expression: ExpressionOption?,
    accents: Map<String, Int>,
    calcResult: SpellCalcResult?,
    powerLevel: Int,
    effectText: String,
    spellName: String,
    canCast: Boolean,
    onAccentChange: (String, Int) -> Unit,
    onSpellNameChange: (String) -> Unit,
    onCast: () -> Unit,
    onSave: () -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    val t = LocalXanteya.current
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("ШАГ 4: АКЦЕНТЫ", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp, letterSpacing = 1.sp)
        }

        // Accent blocks
        val accentList = ACCENTS_BY_EXPRESSION[expression?.id ?: ""] ?: emptyList()
        items(accentList, key = { it.id }) { accent ->
            AccentBlock(
                accent       = accent,
                selectedIdx  = accents[accent.id] ?: 0,
                onSelect     = { idx -> onAccentChange(accent.id, idx) }
            )
        }

        // Result card
        if (calcResult != null && core != null && expression != null) {
            item {
                CostResultCard(
                    calcResult  = calcResult,
                    core        = core,
                    mod         = mod,
                    expression  = expression,
                    powerLevel  = powerLevel,
                    effectText  = effectText
                )
            }
        }

        // Save & Cast section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(t.colors.panel, RoundedCornerShape(t.corner))
                    .border(1.dp, t.colors.line, RoundedCornerShape(t.corner))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("💾 Сохранить заклинание", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                OutlinedTextField(
                    value = spellName,
                    onSpellNameChange,
                    label = { Text("Название", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = t.colors.accent,
                        unfocusedBorderColor = t.colors.line,
                        focusedTextColor     = t.colors.text,
                        unfocusedTextColor   = t.colors.text,
                        focusedLabelColor    = t.colors.accent,
                        unfocusedLabelColor  = t.colors.muted
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onCast,
                        enabled = canCast,
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = t.colors.accent,
                            contentColor = t.colors.bg,
                            disabledContainerColor = t.colors.panel.copy(alpha = 0.5f)
                        )
                    ) {
                        Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Применить", fontFamily = t.display, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OutlinedButton(
                        onClick = onSave,
                        enabled = spellName.isNotBlank() || (calcResult != null),
                        modifier = Modifier.weight(1f).height(46.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.accent),
                        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
                    ) {
                        Text("Сохранить", fontFamily = t.display, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Navigation buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.muted),
                    border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
                ) { Text("← Назад", fontFamily = t.display) }
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed),
                    border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Сброс", fontFamily = t.display)
                }
            }
        }

        item { Spacer(Modifier.height(100.dp)) }
    }
}

// ── Accent block (one per accent option) ─────────────────────────────────

@Composable
private fun AccentBlock(
    accent: AccentOption,
    selectedIdx: Int,
    onSelect: (Int) -> Unit
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(1.dp, t.colors.line, RoundedCornerShape(t.corner))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            accent.name,
            color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
        )
        accent.levels.forEachIndexed { idx, level ->
            val isSelected = idx == selectedIdx
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isSelected) t.colors.bg else t.colors.panel,
                        RoundedCornerShape(t.corner)
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) t.colors.accent else t.colors.line,
                        shape = RoundedCornerShape(t.corner)
                    )
                    .clickable { onSelect(idx) }
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Text(
                    level.value,
                    color = if (isSelected) t.colors.accent else t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(level.description, color = t.colors.muted, fontFamily = t.body, fontSize = 11.sp)
            }
        }
    }
}

// ── Cost result card (shown in step 4 after accents are configured) ───────

@Composable
private fun CostResultCard(
    calcResult: SpellCalcResult,
    core: CoreOption,
    mod: ModOption,
    expression: ExpressionOption,
    powerLevel: Int,
    effectText: String
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(1.5.dp, t.colors.accent, RoundedCornerShape(t.corner))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text("✨ Итоговая стоимость", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 14.sp)

        // Cost display — positives and negatives
        val isFree = calcResult.posP == 0f && calcResult.negP == 0f &&
                calcResult.posE == 0f && calcResult.negE == 0f
        if (isFree) {
            Text("Бесплатно", color = t.colors.muted, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (calcResult.posP > 0f)
                    Text("+${fmtF(calcResult.posP)}п", color = ResourceQuarColor,  fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                if (calcResult.negP < 0f)
                    Text("${fmtF(calcResult.negP)}п",  color = ResourceOmbraColor,  fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                if (calcResult.posE > 0f)
                    Text("+${fmtF(calcResult.posE)}э", color = ResourceVerunaColor, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
                if (calcResult.negE < 0f)
                    Text("${fmtF(calcResult.negE)}э",  color = ResourceSumeniColor, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 22.sp)
            }
        }
        Text("Множитель: x${calcResult.multiplier}", color = t.colors.muted, fontFamily = t.body, fontSize = 12.sp)

        HorizontalDivider(color = t.colors.line)

        // Params
        Text("📜 Параметры", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        val modStr = if (mod.id != "none") " + ${mod.name}" else ""
        Text("Ядро: ${core.name}$modStr", color = t.colors.text, fontFamily = t.body, fontSize = 12.sp)
        Text("Выражение: ${expression.name} (${expression.slots} яч.)", color = t.colors.text, fontFamily = t.body, fontSize = 12.sp)
        Text("Уровень силы: x$powerLevel", color = t.colors.text, fontFamily = t.body, fontSize = 12.sp)

        HorizontalDivider(color = t.colors.line)

        // Effect
        Text("⚡ Эффект", color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
        Text(
            effectText.ifEmpty { "Эффект не определён" },
            color = t.colors.hot, fontFamily = t.body, fontSize = 14.sp, fontStyle = FontStyle.Italic
        )
    }
}

// ── Generic selection card (core / mod / expression) ─────────────────────

@Composable
private fun SelectionCard(
    title: String,
    subtitle: String,
    note: String = "",
    selected: Boolean,
    disabled: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val t = LocalXanteya.current
    val bgColor = when {
        selected  -> t.colors.panel
        disabled  -> t.colors.panel.copy(alpha = 0.35f)
        else      -> t.colors.bg
    }
    val borderColor = when {
        selected  -> t.colors.accent
        disabled  -> t.colors.line.copy(alpha = 0.25f)
        else      -> t.colors.line
    }
    Column(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(t.corner))
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(t.corner)
            )
            .then(if (!disabled) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            title,
            color = if (disabled) t.colors.muted.copy(alpha = 0.5f) else t.colors.text,
            fontFamily = t.display,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        if (subtitle.isNotEmpty()) {
            Text(
                subtitle,
                color = if (disabled) t.colors.muted.copy(alpha = 0.5f) else t.colors.accent,
                fontFamily = t.body,
                fontSize = 11.sp
            )
        }
        if (note.isNotEmpty()) {
            Text(note, color = t.colors.hot, fontFamily = t.body, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════════════
//  SAVED SPELLS TAB
// ═══════════════════════════════════════════════════════════════════════════

@Composable
private fun SavedSpellsTab(
    spells: List<Spell>,
    character: Character,
    onCast: (Spell) -> Unit,
    onDelete: (Spell) -> Unit,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {}
) {
    val t = LocalXanteya.current
    Column(modifier = Modifier.fillMaxSize()) {
        // Export / Import toolbar
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onExport) {
                Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = t.colors.muted)
            }
            IconButton(onClick = onImport) {
                Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = t.colors.muted)
            }
        }

        if (spells.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("📖", fontSize = 40.sp)
                    Text(
                        "Нет сохранённых заклинаний",
                        color = t.colors.text,
                        fontFamily = t.display,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        "Создайте заклинание в конструкторе\nи нажмите «Сохранить»",
                        color = t.colors.muted,
                        fontFamily = t.body,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp, start = 16.dp, end = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(spells, key = { it.id }) { spell ->
                    SavedSpellCard(
                        spell = spell,
                        character = character,
                        onCast   = { onCast(spell) },
                        onDelete = { onDelete(spell) }
                    )
                }
                item { Spacer(Modifier.height(100.dp)) }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SavedSpellCard(
    spell: Spell,
    character: Character,
    onCast: () -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalXanteya.current
    var showDetails by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    val canCast = character.quarResource  >= spell.quarCost  &&
            character.ombraResource >= spell.ombraCost  &&
            character.verunaResource >= spell.verunaCost &&
            character.sumeniResource >= spell.sumeniCost

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(
                width = 1.dp,
                color = if (canCast) t.colors.accent.copy(alpha = 0.5f) else t.colors.line,
                shape = RoundedCornerShape(t.corner)
            )
            .combinedClickable(
                onClick = { showDetails = !showDetails },
                onLongClick = { showDetails = true }
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Header row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = spell.name.ifBlank { "Без названия" },
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = spell.costSummary,
                    color = t.colors.accent,
                    fontFamily = t.body,
                    fontSize = 12.sp
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = onCast,
                    enabled = canCast,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = t.colors.accent,
                        contentColor = t.colors.bg,
                        disabledContainerColor = t.colors.panel.copy(alpha = 0.5f),
                        disabledContentColor = t.colors.muted
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Использовать", fontSize = 11.sp, fontFamily = t.display)
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = t.colors.hot.copy(alpha = 0.8f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Expandable details
        if (showDetails) {
            HorizontalDivider(color = t.colors.line)

            if (spell.coreName.isNotEmpty()) {
                val coreWithMod = spell.coreName +
                        if (spell.modName.isNotEmpty()) " + ${spell.modName}" else ""
                SpellDetailRow("Ядро", coreWithMod)
            }
            if (spell.expressionName.isNotEmpty()) SpellDetailRow("Выражение", spell.expressionName)
            if (spell.powerLevel > 0) SpellDetailRow("Сила", "x${spell.powerLevel}")
            if (spell.effectText.isNotEmpty()) SpellDetailRow("Эффект", spell.effectText)

            // Cost badges
            val hasCost = spell.quarCost > 0f || spell.ombraCost > 0f ||
                    spell.verunaCost > 0f || spell.sumeniCost > 0f
            if (hasCost) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (spell.quarCost > 0f)   SpellCostBadge("+п", spell.quarCost,  ResourceQuarColor)
                    if (spell.ombraCost > 0f)  SpellCostBadge("-п", spell.ombraCost, ResourceOmbraColor)
                    if (spell.verunaCost > 0f) SpellCostBadge("+э", spell.verunaCost, ResourceVerunaColor)
                    if (spell.sumeniCost > 0f) SpellCostBadge("-э", spell.sumeniCost, ResourceSumeniColor)
                }
            }

            if (!canCast) {
                Text("⚠ Недостаточно ресурсов", color = t.colors.hot, fontFamily = t.body, fontSize = 11.sp)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = t.colors.panel,
            title = { Text("Удалить заклинание?", color = t.colors.text, fontFamily = t.display) },
            text = {
                Text(
                    "«${spell.name.ifBlank { "Без названия" }}» будет удалено из книги.",
                    color = t.colors.muted,
                    fontFamily = t.body
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Удалить", color = t.colors.hot, fontFamily = t.display)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = t.colors.muted, fontFamily = t.display)
                }
            }
        )
    }
}

@Composable
private fun SpellDetailRow(label: String, value: String) {
    if (value.isBlank()) return
    val t = LocalXanteya.current
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text("$label:", color = t.colors.muted, fontFamily = t.body, fontSize = 11.sp, modifier = Modifier.width(70.dp))
        Text(value, color = t.colors.text, fontFamily = t.body, fontSize = 11.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun SpellCostBadge(symbol: String, cost: Float, color: Color) {
    val t = LocalXanteya.current
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(t.corner / 2))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    ) {
        Text(
            "$symbol ${fmtF(cost)}",
            color = color, fontFamily = t.display, fontSize = 11.sp, fontWeight = FontWeight.Bold
        )
    }
}
