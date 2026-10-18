package com.questkeep.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.auth.AuthState
import com.questkeep.auth.AuthViewModel
import com.questkeep.ui.theme.*
import com.questkeep.utils.DiceParser
import com.questkeep.utils.DndRollResult
import com.questkeep.utils.NexurDie
import com.questkeep.utils.NexurRollResult
import com.questkeep.viewmodel.DiceRollerViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiceRollerModal(
    campaignId: String?,
    onDismiss: () -> Unit,
    gmId: String? = null,
    initialModifier: Int? = null,
    initialStatName: String? = null,
    viewModel: DiceRollerViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val selectedTab by viewModel.selectedTab.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val senderName = (authState as? AuthState.Authenticated)?.user?.nickname ?: "Игрок"
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current

    // D&D state
    val selectedDie by viewModel.selectedDndDie.collectAsState()
    val dndCount by viewModel.dndCount.collectAsState()
    val dndModifier by viewModel.dndModifier.collectAsState()
    val lastDndResult by viewModel.lastDndResult.collectAsState()

    // Nexur state
    val nexurPoolSize by viewModel.nexurPoolSize.collectAsState()
    val lastNexurResult by viewModel.lastNexurResult.collectAsState()

    // Animation state
    var isRolling by remember { mutableStateOf(false) }
    var rollingFace by remember { mutableStateOf("🎲") }
    var showResult by remember { mutableStateOf(false) }

    LaunchedEffect(initialModifier, initialStatName) {
        if (initialModifier != null) {
            viewModel.setSelectedTab(0)
            viewModel.setDndDie(20)
            viewModel.setDndCount(1)
            viewModel.setDndModifier(initialModifier)
        }
    }

    val resultScale by animateFloatAsState(
        targetValue = if (showResult) 1f else 0.7f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "result_scale"
    )

    val doRollPublic = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (selectedTab == 0) {
            viewModel.rollDndAndPost(campaignId, senderName, isPublic = true)
        } else {
            viewModel.rollNexurAndPost(campaignId, senderName, isPublic = true)
        }
        triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
    }

    val doRollPrivate = {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (selectedTab == 0) {
            viewModel.rollDndAndPost(campaignId, senderName, isPublic = false, privateTargetId = gmId)
        } else {
            viewModel.rollNexurAndPost(campaignId, senderName, isPublic = false, privateTargetId = gmId)
        }
        triggerRollAnimation(scope, { isRolling = it }, { rollingFace = it }, { showResult = it })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 10.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .background(t.colors.line, RoundedCornerShape(2.dp))
            )
        },
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        if (isLandscape) {
            // Landscape layout: flex-row with options on left and results/actions on right
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Left Column: Tab switcher, quick roll badge, die selectors / pool adjuster
                Column(
                    modifier = Modifier
                        .weight(1.15f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DiceModeTabs(
                        selectedTab = selectedTab,
                        onTabSelected = { tab ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setSelectedTab(tab)
                            showResult = if (tab == 0) lastDndResult != null else lastNexurResult != null
                        }
                    )

                    if (initialStatName != null) {
                        QuickRollBadge(initialStatName, initialModifier ?: dndModifier)
                    }

                    if (selectedTab == 0) {
                        DndSelectors(
                            selectedDie = selectedDie,
                            dndCount = dndCount,
                            dndModifier = dndModifier,
                            onSelectDie = { sides ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setDndDie(sides)
                            },
                            onChangeCount = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.changeDndCount(delta)
                            },
                            onChangeModifier = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.changeDndModifier(delta)
                            }
                        )
                    } else {
                        NexurSelectors(
                            nexurPoolSize = nexurPoolSize,
                            onChangePoolSize = { delta ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.changeNexurPoolSize(delta)
                            },
                            onSetPoolSize = { size ->
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.setNexurPoolSize(size)
                            }
                        )
                    }
                }

                // Right Column: Results & Roll Actions (fits without scrolling)
                Column(
                    modifier = Modifier
                        .weight(0.85f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (selectedTab == 0) {
                        DndResultDisplay(
                            isRolling = isRolling,
                            showResult = showResult,
                            rollingFace = rollingFace,
                            lastDndResult = lastDndResult,
                            resultScale = resultScale
                        )
                    } else {
                        NexurResultDisplay(
                            isRolling = isRolling,
                            showResult = showResult,
                            rollingFace = rollingFace,
                            lastNexurResult = lastNexurResult,
                            resultScale = resultScale
                        )
                    }

                    DiceActionButtons(
                        onRollPublic = doRollPublic,
                        onRollPrivate = doRollPrivate
                    )
                }
            }
        } else {
            // Portrait layout: Single column
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                DiceModeTabs(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.setSelectedTab(tab)
                        showResult = if (tab == 0) lastDndResult != null else lastNexurResult != null
                    }
                )

                if (initialStatName != null) {
                    QuickRollBadge(initialStatName, initialModifier ?: dndModifier)
                }

                if (selectedTab == 0) {
                    DndSelectors(
                        selectedDie = selectedDie,
                        dndCount = dndCount,
                        dndModifier = dndModifier,
                        onSelectDie = { sides ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setDndDie(sides)
                        },
                        onChangeCount = { delta ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.changeDndCount(delta)
                        },
                        onChangeModifier = { delta ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.changeDndModifier(delta)
                        }
                    )

                    if (isRolling || showResult) {
                        DndResultDisplay(
                            isRolling = isRolling,
                            showResult = showResult,
                            rollingFace = rollingFace,
                            lastDndResult = lastDndResult,
                            resultScale = resultScale
                        )
                    }
                } else {
                    NexurSelectors(
                        nexurPoolSize = nexurPoolSize,
                        onChangePoolSize = { delta ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.changeNexurPoolSize(delta)
                        },
                        onSetPoolSize = { size ->
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            viewModel.setNexurPoolSize(size)
                        }
                    )

                    if (isRolling || showResult) {
                        NexurResultDisplay(
                            isRolling = isRolling,
                            showResult = showResult,
                            rollingFace = rollingFace,
                            lastNexurResult = lastNexurResult,
                            resultScale = resultScale
                        )
                    }
                }

                DiceActionButtons(
                    onRollPublic = doRollPublic,
                    onRollPrivate = doRollPrivate
                )
            }
        }
    }
}

@Composable
private fun QuickRollBadge(statName: String, modifier: Int) {
    val t = LocalXanteya.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = t.colors.accent.copy(alpha = 0.15f),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.accent),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("⚡", fontSize = 14.sp)
            Text(
                text = "Быстрый бросок: $statName (${if (modifier >= 0) "+$modifier" else "$modifier"})",
                fontFamily = t.display,
                color = t.colors.accent,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun DiceModeTabs(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val t = LocalXanteya.current
    TabRow(
        selectedTabIndex = selectedTab,
        containerColor = t.colors.bg,
        contentColor = t.colors.accent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(t.corner.coerceAtLeast(8.dp)))
            .border(1.dp, t.colors.line, RoundedCornerShape(t.corner.coerceAtLeast(8.dp)))
    ) {
        Tab(
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            text = {
                Text(
                    "⚔️ D&D",
                    fontFamily = t.display,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (selectedTab == 0) t.colors.accent else t.colors.muted
                )
            }
        )
        Tab(
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            text = {
                Text(
                    "✨ Nexur",
                    fontFamily = t.display,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 14.sp,
                    color = if (selectedTab == 1) t.colors.accent else t.colors.muted
                )
            }
        )
    }
}

@Composable
private fun DndSelectors(
    selectedDie: Int,
    dndCount: Int,
    dndModifier: Int,
    onSelectDie: (Int) -> Unit,
    onChangeCount: (Int) -> Unit,
    onChangeModifier: (Int) -> Unit
) {
    val t = LocalXanteya.current
    Text(
        text = "ТИП КУБИКА:",
        fontFamily = t.display,
        fontSize = 11.sp,
        color = t.colors.muted
    )

    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(DiceParser.DND_STANDARD_DICE) { sides ->
            HexDie(
                n = sides,
                active = selectedDie == sides,
                onClick = { onSelectDie(sides) }
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Count Adjuster
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = t.colors.bg),
            shape = RoundedCornerShape(t.corner),
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Количество", fontSize = 11.sp, color = t.colors.muted, fontFamily = t.body)
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { onChangeCount(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "-", tint = t.colors.text)
                    }
                    Text(
                        text = "$dndCount",
                        fontSize = 18.sp,
                        fontFamily = t.display,
                        color = t.colors.text,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onChangeCount(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "+", tint = t.colors.text)
                    }
                }
            }
        }

        // Modifier Adjuster
        Card(
            modifier = Modifier.weight(1f),
            colors = CardDefaults.cardColors(containerColor = t.colors.bg),
            shape = RoundedCornerShape(t.corner),
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
        ) {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Модификатор", fontSize = 11.sp, color = t.colors.muted, fontFamily = t.body)
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(onClick = { onChangeModifier(-1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Remove, contentDescription = "-", tint = t.colors.text)
                    }
                    Text(
                        text = if (dndModifier >= 0) "+$dndModifier" else "$dndModifier",
                        fontSize = 18.sp,
                        fontFamily = t.display,
                        color = if (dndModifier != 0) t.colors.accent else t.colors.text,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = { onChangeModifier(1) }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = "+", tint = t.colors.text)
                    }
                }
            }
        }
    }

    Text(
        text = "Формула: ${dndCount}d$selectedDie" + (if (dndModifier > 0) "+$dndModifier" else if (dndModifier < 0) "$dndModifier" else ""),
        fontSize = 12.sp,
        fontFamily = t.display,
        color = t.colors.accent
    )
}

@Composable
private fun NexurSelectors(
    nexurPoolSize: Int,
    onChangePoolSize: (Int) -> Unit,
    onSetPoolSize: (Int) -> Unit
) {
    val t = LocalXanteya.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = t.colors.bg),
        shape = RoundedCornerShape(t.corner),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = "МЕХАНИКА NEXUR (D6):",
                fontSize = 10.sp,
                fontFamily = t.display,
                color = t.colors.accent,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(3.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("⚀⚁⚂: 0", fontSize = 11.sp, color = t.colors.muted, fontFamily = t.body)
                Text("⚃⚄: 1 успех", fontSize = 11.sp, color = t.colors.accent, fontFamily = t.body)
                Text("⚅: 2 успеха ★", fontSize = 11.sp, color = t.colors.hot, fontFamily = t.body)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = t.colors.bg),
        shape = RoundedCornerShape(t.corner),
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("КУБОВ В ПУЛЕ", fontSize = 11.sp, color = t.colors.muted, fontFamily = t.display)
            Spacer(Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                FilledIconButton(
                    onClick = { onChangePoolSize(-1) },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = t.colors.panel)
                ) {
                    Icon(Icons.Filled.Remove, contentDescription = "-", tint = t.colors.text)
                }
                Text(
                    text = "$nexurPoolSize d6",
                    fontSize = 24.sp,
                    fontFamily = t.display,
                    color = t.colors.accent,
                    fontWeight = FontWeight.Bold
                )
                FilledIconButton(
                    onClick = { onChangePoolSize(1) },
                    modifier = Modifier.size(34.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = t.colors.panel)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "+", tint = t.colors.text)
                }
            }

            Spacer(Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                items(listOf(1, 2, 3, 4, 5, 6, 8, 10)) { size ->
                    AssistChip(
                        onClick = { onSetPoolSize(size) },
                        label = { Text("$size", fontFamily = t.display, fontSize = 11.sp) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = if (nexurPoolSize == size) t.colors.accent else t.colors.panel,
                            labelColor = if (nexurPoolSize == size) t.colors.bg else t.colors.text
                        ),
                        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line),
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DndResultDisplay(
    isRolling: Boolean,
    showResult: Boolean,
    rollingFace: String,
    lastDndResult: DndRollResult?,
    resultScale: Float
) {
    val t = LocalXanteya.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.bg, RoundedCornerShape(t.corner.coerceAtLeast(8.dp)))
            .border(1.5.dp, t.colors.accent, RoundedCornerShape(t.corner.coerceAtLeast(8.dp)))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRolling) {
            Text(text = rollingFace, fontSize = 48.sp)
        } else if (showResult && lastDndResult != null) {
            val res = lastDndResult
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(resultScale)
            ) {
                Text(
                    text = "${res.total}",
                    fontSize = 46.sp,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        res.isNat20 -> t.colors.accent
                        res.isNat1 -> t.colors.hot
                        else -> t.colors.text
                    }
                )
                if (res.isNat20) {
                    Text("★ КРИТИЧЕСКИЙ УСПЕХ! ★", fontFamily = t.display, color = t.colors.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                } else if (res.isNat1) {
                    Text("💀 КРИТИЧЕСКИЙ ПРОВАЛ! 💀", fontFamily = t.display, color = t.colors.hot, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Text(
                    text = "${res.count}d${res.sides} = [${res.rolls.joinToString(", ")}]" +
                            (if (res.modifier != 0) (if (res.modifier > 0) " +${res.modifier}" else " ${res.modifier}") else ""),
                    fontSize = 11.sp,
                    fontFamily = t.body,
                    color = t.colors.muted
                )
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("🎲", fontSize = 32.sp)
                Spacer(Modifier.height(2.dp))
                Text("Нажмите бросок", fontFamily = t.display, color = t.colors.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun NexurResultDisplay(
    isRolling: Boolean,
    showResult: Boolean,
    rollingFace: String,
    lastNexurResult: NexurRollResult?,
    resultScale: Float
) {
    val t = LocalXanteya.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.bg, RoundedCornerShape(t.corner.coerceAtLeast(8.dp)))
            .border(
                1.5.dp,
                if ((lastNexurResult?.triumphsCount ?: 0) > 0) t.colors.hot else t.colors.accent,
                RoundedCornerShape(t.corner.coerceAtLeast(8.dp))
            )
            .padding(vertical = 12.dp, horizontal = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isRolling) {
            Text(text = rollingFace, fontSize = 48.sp)
        } else if (showResult && lastNexurResult != null) {
            val res = lastNexurResult
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.scale(resultScale)
            ) {
                Text(
                    text = "${res.totalSuccesses}",
                    fontSize = 44.sp,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    color = if (res.totalSuccesses > 0) t.colors.accent else t.colors.hot
                )
                Text(
                    text = formatSuccessWord(res.totalSuccesses),
                    fontFamily = t.display,
                    fontSize = 14.sp,
                    color = t.colors.text,
                    fontWeight = FontWeight.SemiBold
                )
                if (res.triumphsCount > 0) {
                    Text(
                        text = "🌟 Триумфов: ${res.triumphsCount}",
                        color = t.colors.hot,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    res.dice.forEach { die ->
                        NexurDieBadge(die)
                    }
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("✨", fontSize = 32.sp)
                Spacer(Modifier.height(2.dp))
                Text("Нажмите бросок", fontFamily = t.display, color = t.colors.muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun DiceActionButtons(
    onRollPublic: () -> Unit,
    onRollPrivate: () -> Unit
) {
    val t = LocalXanteya.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onRollPublic,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(t.corner),
            colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent)
        ) {
            Text(
                stringResource(R.string.roll_public),
                fontFamily = t.display,
                color = t.colors.bg,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }

        OutlinedButton(
            onClick = onRollPrivate,
            modifier = Modifier.weight(1f).height(44.dp),
            shape = RoundedCornerShape(t.corner),
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.hot),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.hot)
        ) {
            Text(
                stringResource(R.string.roll_private),
                fontFamily = t.display,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun NexurDieBadge(die: NexurDie) {
    val t = LocalXanteya.current
    val icons = mapOf(1 to "⚀", 2 to "⚁", 3 to "⚂", 4 to "⚃", 5 to "⚄", 6 to "⚅")
    val bgColor = when (die.successes) {
        2 -> t.colors.hot.copy(alpha = 0.25f)
        1 -> t.colors.accent.copy(alpha = 0.25f)
        else -> t.colors.panel
    }
    val borderColor = when (die.successes) {
        2 -> t.colors.hot
        1 -> t.colors.accent
        else -> t.colors.line
    }

    Column(
        modifier = Modifier
            .padding(horizontal = 3.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(6.dp))
            .padding(vertical = 4.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = icons[die.face] ?: "${die.face}", fontSize = 18.sp)
        Text(
            text = when (die.successes) {
                2 -> "+2★"
                1 -> "+1"
                else -> "0"
            },
            fontSize = 10.sp,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold,
            color = when (die.successes) {
                2 -> t.colors.hot
                1 -> t.colors.accent
                else -> t.colors.muted
            }
        )
    }
}

private fun formatSuccessWord(count: Int): String {
    val mod10 = count % 10
    val mod100 = count % 100
    return when {
        mod100 in 11..14 -> "Успехов"
        mod10 == 1 -> "Успех"
        mod10 in 2..4 -> "Успеха"
        else -> "Успехов"
    }
}

fun triggerRollAnimation(
    scope: CoroutineScope,
    setIsRolling: (Boolean) -> Unit,
    setRollingFace: (String) -> Unit,
    setShowResult: (Boolean) -> Unit
) {
    scope.launch(Dispatchers.Main) {
        val faces = listOf("⚀", "⚁", "⚂", "⚃", "⚄", "⚅")
        setIsRolling(true)
        setShowResult(false)
        var i = 0
        val endTime = System.currentTimeMillis() + 500
        while (System.currentTimeMillis() < endTime) {
            setRollingFace(faces[i % faces.size])
            i++
            delay(60)
        }
        setRollingFace("🎲")
        setIsRolling(false)
        setShowResult(true)
    }
}
