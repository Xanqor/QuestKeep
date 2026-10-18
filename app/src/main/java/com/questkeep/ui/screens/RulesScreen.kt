package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.questkeep.ui.theme.LocalXanteya

enum class RulesCategory(val title: String) {
    ALL("Все"),
    QUICK("Шпаргалка"),
    CHECKS("Проверки и d6"),
    STATS("Характеристики"),
    ACTIONS("Действия и ОД"),
    COMBAT("Бой и Защита"),
    REST("Здоровье и Отдых"),
    XP("Опыт и Развитие"),
    GM("Ведущему (ГМ)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RulesScreen(
    onBack: () -> Unit
) {
    val t = LocalXanteya.current
    var selectedCategory by remember { mutableStateOf(RulesCategory.ALL) }
    var searchQuery by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Свод правил NEXUR",
                            color = t.colors.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Text(
                            text = "Официальная редакция v1.0",
                            color = t.colors.accent,
                            fontFamily = t.body,
                            fontSize = 11.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = t.colors.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(t.colors.bg)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Поиск по правилам...", color = t.colors.muted, fontFamily = t.body) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = t.colors.muted) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Очистить", tint = t.colors.muted)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(t.corner),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = t.colors.accent,
                    unfocusedBorderColor = t.colors.line,
                    focusedTextColor = t.colors.text,
                    unfocusedTextColor = t.colors.text,
                    focusedContainerColor = t.colors.panel,
                    unfocusedContainerColor = t.colors.panel
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
            )

            // Category filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                RulesCategory.entries.forEach { category ->
                    val isSelected = selectedCategory == category
                    Surface(
                        shape = RoundedCornerShape(t.corner / 2),
                        color = if (isSelected) t.colors.accent else t.colors.panel,
                        border = BorderStroke(1.dp, if (isSelected) t.colors.accent else t.colors.line),
                        modifier = Modifier.clickable { selectedCategory = category }
                    ) {
                        Text(
                            text = category.title,
                            color = if (isSelected) t.colors.bg else t.colors.text,
                            fontFamily = t.display,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Rules Content List
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(top = 4.dp, bottom = 96.dp)
            ) {
                // 1. Шпаргалка (Быстрые правила)
                if (shouldShow(RulesCategory.QUICK, selectedCategory, searchQuery, "шпаргалка быстрые правила таблица")) {
                    item { QuickReferenceSection() }
                }

                // 2. Проверки и d6
                if (shouldShow(RulesCategory.CHECKS, selectedCategory, searchQuery, "проверки d6 успехи сложность критический провал преимущество помеха вдохновение")) {
                    item { ChecksAndD6Section() }
                }

                // 3. Характеристики
                if (shouldShow(RulesCategory.STATS, selectedCategory, searchQuery, "характеристики телосложение движение внимательность интеллект влияние воля")) {
                    item { CharacteristicsSection() }
                }

                // 4. Действия и ОД
                if (shouldShow(RulesCategory.ACTIONS, selectedCategory, searchQuery, "время действия од очки действий малое среднее большое ход")) {
                    item { ActionsAndApSection() }
                }

                // 5. Бой, Защита и Броня
                if (shouldShow(RulesCategory.COMBAT, selectedCategory, searchQuery, "бой атака защита броня щиты поглощение прочность урон перемещение рывок")) {
                    item { CombatAndArmorSection() }
                }

                // 6. Здоровье и Отдых
                if (shouldShow(RulesCategory.REST, selectedCategory, searchQuery, "здоровье оз очки здоровья отдых стремительный быстрый средний медленный восстановление")) {
                    item { HealthAndRestSection() }
                }

                // 7. Опыт и Развитие
                if (shouldShow(RulesCategory.XP, selectedCategory, searchQuery, "опыт xp развитие повышение характеристик домены навыки прокачка")) {
                    item { ExperienceAndProgressionSection() }
                }

                // 8. Ведущему (ГМ)
                if (shouldShow(RulesCategory.GM, selectedCategory, searchQuery, "ведущий гм правила ведущего особые ситуации неигровые персонажи")) {
                    item { GmRulesSection() }
                }
            }
        }
    }
}

private fun shouldShow(
    sectionCategory: RulesCategory,
    activeCategory: RulesCategory,
    query: String,
    keywords: String
): Boolean {
    val matchesCategory = activeCategory == RulesCategory.ALL || activeCategory == sectionCategory
    val matchesQuery = query.isBlank() || keywords.contains(query.trim(), ignoreCase = true)
    return matchesCategory && matchesQuery
}

// ─────────────────────────────────────────────────────────────────────────────
// Section Composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun QuickReferenceSection() {
    RuleCard(
        title = "⚡ Быстрые правила (Шпаргалка)",
        badge = "Раздел 21"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RuleTableRow("Проверка", "d6 = Характеристика (1–3: 0, 4–5: 1, 6: 2 успеха)")
            RuleTableRow("Успех", "Успехов ≥ Сложности")
            RuleTableRow("Критический успех", "На 2+ успеха выше Сложности")
            RuleTableRow("Критический провал", "0 успехов на всех кубах")
            RuleTableRow("Преимущество / Помеха", "Доп. бросок с выбором лучшего / худшего (макс. 2)")
            RuleTableRow("Вдохновение (макс. 6)", "Доп. бросок (лучший) ИЛИ +1 кубик к пулу")
            RuleTableRow("Очки действий (ОД)", "3 ОД за ход; не переносятся между раундами")
            RuleTableRow("Очки здоровья (ОЗ)", "Телосложение × 6 (0 ОЗ = критическое состояние)")
            RuleTableRow("Скорость", "Движение × 2 м за ход (без ОД); Рывок = 1 ОД")
            RuleTableRow("Грузоподъёмность", "Телосложение × 20")
        }
    }
}

@Composable
private fun ChecksAndD6Section() {
    val t = LocalXanteya.current
    RuleCard(
        title = "🎲 Проверки и Механика d6",
        badge = "Часть I • Основы"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Для проверки бросается количество шестигранных кубиков (d6), равное значению проверяемой характеристики. Сложность (необходимое число успехов) задаётся Ведущим.",
                fontFamily = t.body,
                color = t.colors.text,
                fontSize = 13.sp
            )

            // D6 faces breakdown
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DieFaceCard(label = "Грани 1–3", successes = "0 успехов", color = t.colors.muted, modifier = Modifier.weight(1f))
                DieFaceCard(label = "Грани 4–5", successes = "1 успех", color = t.colors.accent, modifier = Modifier.weight(1f))
                DieFaceCard(label = "Грань 6", successes = "2 успеха", color = t.colors.hot, modifier = Modifier.weight(1f))
            }

            RuleTableRow("Крит. успех", "Количество успехов превышает Сложность на 2 или более.")
            RuleTableRow("Крит. провал", "Проверка дала 0 успехов на всех костях.")
            RuleTableRow("Преимущество", "Дополнительный бросок с выбором лучшего результата. Помеха — выбор худшего. Преимущества и Помехи взаимно нейтрализуются (максимум 2).")
            RuleTableRow("Вдохновение", "Тратится на: либо доп. бросок (выбрать лучший), либо +1 дополнительный d6 к проверке. Способы не совмещаются!")
            RuleTableRow("Совместная проверка", "Каждый участник бросает проверку, успехи всех складываются против общей Сложности.")
        }
    }
}

@Composable
private fun CharacteristicsSection() {
    RuleCard(
        title = "🧬 6 Характеристик Персонажа",
        badge = "Часть II"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            StatRow("Телосложение (Т)", "Физическая сила, выносливость, ранения, основа пассивной защиты и здоровья (ОЗ = Т × 6).")
            StatRow("Движение (Дв)", "Координация, реакция, скорость перемещения, ближний бой и уклонение.")
            StatRow("Внимательность (Вн)", "Восприятие, чутьё, наблюдательность, способность замечать детали и дальний бой.")
            StatRow("Интеллект (М)", "Память, логика, знания, анализ и дедукция.")
            StatRow("Влияние (Вл)", "Способность воздействовать на разум других, убеждение, переговоры и харизма.")
            StatRow("Воля (В)", "Сила духа, самоконтроль, концентрация и сопротивление внешнему воздействию.")
        }
    }
}

@Composable
private fun ActionsAndApSection() {
    val t = LocalXanteya.current
    RuleCard(
        title = "⏳ Время и Очки Действий (ОД)",
        badge = "Часть III"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "В свой ход персонаж получает 3 ОД (очка действий). Неиспользованные ОД сгорают в конце хода и не переносятся.",
                fontFamily = t.body,
                color = t.colors.text,
                fontSize = 13.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ApCard("Малое", "1 ОД", "Быстрое взаимодействие, смена оружия, Рывок", Modifier.weight(1f))
                ApCard("Среднее", "2 ОД", "Стандартная атака, использование большинства навыков", Modifier.weight(1f))
                ApCard("Большое", "3 ОД", "Сложные приёмы, мощные заклинания", Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun CombatAndArmorSection() {
    val t = LocalXanteya.current
    RuleCard(
        title = "⚔️ Бой, Защита, Броня и Щиты",
        badge = "Часть IV"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            RuleTableRow("Атака", "Требует ОД. Ближний бой использует Движение; Дальний бой использует Внимательность.")
            RuleTableRow("Защита (0 ОД)", "Встречная проверка в ответ на атаку. НЕ ТРЕБУЕТ ОД! Характеристика: Телосложение (или Движение при уклонении). Победил тот, у кого больше успехов. При равенстве побеждает Защитник!")
            RuleTableRow("Порядок хода", "Стороны ходят по средней величине Движения участников (от большего к меньшему). Внутри группы порядок определяется свободно.")

            HorizontalDivider(color = t.colors.line, thickness = 1.dp, modifier = Modifier.padding(vertical = 4.dp))

            Text(
                text = "🛡️ Механика Брони и Щитов (Поглощение и Прочность)",
                fontFamily = t.display,
                color = t.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )

            Text(
                text = "• Поглощение: максимальное количество урона, которое броня/щит способны поглотить за одно попадание.\n" +
                        "• Прочность: запас брони/щита. Уменьшается на величину поглощённого урона (не более Поглощения).\n" +
                        "• Порядок распределения: ЩИТ ➔ БРОНЯ ➔ ОСТАТОК В ОЗ.",
                fontFamily = t.body,
                color = t.colors.text,
                fontSize = 12.sp
            )

            // Example box
            Surface(
                shape = RoundedCornerShape(t.corner / 2),
                color = t.colors.bg.copy(alpha = 0.8f),
                border = BorderStroke(1.dp, t.colors.line),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("💡 Пример поглощения:", fontSize = 11.sp, color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold)
                    Text(
                        "Входящий урон 5. Броня имеет Поглощение 3 и Прочность 10. Броня поглощает 3 урона и теряет 3 Прочности (остаётся 7). Оставшиеся 2 урона проходят напрямую в ОЗ персонажа.",
                        fontSize = 12.sp,
                        color = t.colors.text,
                        fontFamily = t.body
                    )
                }
            }
        }
    }
}

@Composable
private fun HealthAndRestSection() {
    val t = LocalXanteya.current
    RuleCard(
        title = "🩹 Здоровье и Восстановление (Отдых)",
        badge = "Часть IV • Здоровье"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Максимум ОЗ = Телосложение × 6. При 0 ОЗ персонаж оказывается в критическом состоянии и не может действовать.",
                fontFamily = t.body,
                color = t.colors.text,
                fontSize = 13.sp
            )

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                RuleTableRow("Стремительный отдых (15–30 мин)", "0 ОЗ (передышка для смены снаряжения)")
                RuleTableRow("Быстрый отдых (1–2 часа)", "Восстанавливает: Телосложение × 2 ОЗ")
                RuleTableRow("Средний отдых (4–8 часов)", "Восстанавливает: Телосложение × 4 ОЗ")
                RuleTableRow("Медленный отдых (24 часа)", "Восстанавливает: Телосложение × 6 ОЗ (полное)")
            }
        }
    }
}

@Composable
private fun ExperienceAndProgressionSection() {
    val t = LocalXanteya.current
    RuleCard(
        title = "📈 Опыт (XP) и Развитие",
        badge = "Часть V"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Nexur не использует универсальный общий уровень. Персонаж развивается точечно через характеристики, домены и навыки за очки опыта (XP).",
                fontFamily = t.body,
                color = t.colors.text,
                fontSize = 13.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = t.colors.bg.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, t.colors.line),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Старт: 12 XP", fontFamily = t.display, fontSize = 12.sp, color = t.colors.accent, fontWeight = FontWeight.Bold)
                        Text("Тратятся только на домены и навыки при создании.", fontFamily = t.body, fontSize = 11.sp, color = t.colors.muted)
                    }
                }
                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = t.colors.bg.copy(alpha = 0.7f),
                    border = BorderStroke(1.dp, t.colors.line),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Text("Сессия: ~3 XP", fontFamily = t.display, fontSize = 12.sp, color = t.colors.accent, fontWeight = FontWeight.Bold)
                        Text("Награда за полную сессию + бонусы за достижения.", fontFamily = t.body, fontSize = 11.sp, color = t.colors.muted)
                    }
                }
            }

            HorizontalDivider(color = t.colors.line, thickness = 1.dp)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Характеристики", fontFamily = t.display, fontSize = 12.sp, color = t.colors.accent, fontWeight = FontWeight.Bold)
                    CostRow("1 → 2", "6 XP")
                    CostRow("2 → 3", "10 XP")
                    CostRow("3 → 4", "14 XP")
                    CostRow("4 → 5", "16 XP")
                    CostRow("5 → 6", "20 XP")
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Доп. Домены", fontFamily = t.display, fontSize = 12.sp, color = t.colors.accent, fontWeight = FontWeight.Bold)
                    CostRow("0 → 1", "4 XP")
                    CostRow("1 → 2", "6 XP")
                    CostRow("2 → 3", "8 XP")
                    CostRow("3 → 4", "10 XP")
                    CostRow("4 → 5", "12 XP")
                    CostRow("5 → 6", "14 XP")
                }
            }
        }
    }
}

@Composable
private fun GmRulesSection() {
    val t = LocalXanteya.current
    RuleCard(
        title = "👑 Правила Ведущего (ГМ)",
        badge = "Часть VI"
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RuleTableRow("Роль Ведущего", "Ведущий описывает мир, управляет NPC, назначает Сложность и определяет последствия действий.")
            RuleTableRow("Правило особого случая", "Если ситуация не описана правилами напрямую, Ведущий определяет результат на основе логики сцены и общих принципов системы.")
            RuleTableRow("Приоритет правил", "Более конкретное правило навыка или ситуации ВСЕГДА имеет приоритет над общим правилом.")
            RuleTableRow("Фиксированные значения", "Для ускорения игры Ведущий вправе использовать средние фиксированные значения для NPC вместо броска кубиков.")
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// UI Helpers
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RuleCard(
    title: String,
    badge: String,
    content: @Composable () -> Unit
) {
    val t = LocalXanteya.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = t.colors.accent.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, t.colors.accent.copy(alpha = 0.3f))
                ) {
                    Text(
                        text = badge,
                        color = t.colors.accent,
                        fontSize = 10.sp,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun RuleTableRow(label: String, value: String) {
    val t = LocalXanteya.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.bg.copy(alpha = 0.4f), RoundedCornerShape(t.corner / 2))
            .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.5f)), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = t.colors.accent,
            fontFamily = t.display,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = t.colors.text,
            fontFamily = t.body,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatRow(title: String, description: String) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.bg.copy(alpha = 0.4f), RoundedCornerShape(t.corner / 2))
            .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.5f)), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(title, color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        Text(description, color = t.colors.text, fontFamily = t.body, fontSize = 11.sp)
    }
}

@Composable
private fun DieFaceCard(label: String, successes: String, color: Color, modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Surface(
        shape = RoundedCornerShape(t.corner / 2),
        color = t.colors.bg.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, t.colors.line),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(label, fontSize = 11.sp, color = t.colors.muted, fontFamily = t.body)
            Spacer(Modifier.height(2.dp))
            Text(successes, fontSize = 12.sp, color = color, fontFamily = t.display, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun ApCard(name: String, cost: String, desc: String, modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    Surface(
        shape = RoundedCornerShape(t.corner / 2),
        color = t.colors.bg.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, t.colors.line),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontSize = 12.sp, color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold)
                Text(cost, fontSize = 11.sp, color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(2.dp))
            Text(desc, fontSize = 10.sp, color = t.colors.muted, fontFamily = t.body)
        }
    }
}

@Composable
private fun CostRow(step: String, cost: String) {
    val t = LocalXanteya.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.bg.copy(alpha = 0.5f), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(step, fontSize = 11.sp, color = t.colors.text, fontFamily = t.body)
        Text(cost, fontSize = 11.sp, color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold)
    }
}
