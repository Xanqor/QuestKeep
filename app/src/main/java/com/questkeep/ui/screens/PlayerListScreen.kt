package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.Character
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.PlayerListItem
import com.questkeep.viewmodel.PlayerListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerListScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: PlayerListViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.loadPlayers(campaignId)
    }

    val players by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGm = viewModel.isCurrentUserGm()

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Список игроков",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold
                    )
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                campaignId == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Кампания не выбрана", color = t.colors.muted, fontFamily = t.body)
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = t.colors.accent)
                    }
                }
                players.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "В кампании пока нет игроков",
                            color = t.colors.muted,
                            fontFamily = t.body,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(32.dp)
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
                    ) {
                        item {
                            Text(
                                "Участники кампании (${players.size})",
                                style = MaterialTheme.typography.labelMedium,
                                color = t.colors.muted,
                                fontFamily = t.display,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(players, key = { it.userId }) { player ->
                            PlayerCard(
                                player = player,
                                isGm = isGm,
                                currentUserId = viewModel.currentUserId,
                                onGrantCoGm = { viewModel.grantCoGm(campaignId, player.userId) },
                                onRevokeCoGm = { viewModel.revokeCoGm(campaignId, player.userId) }
                            )
                        }
                        item { Spacer(Modifier.height(100.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlayerCard(
    player: PlayerListItem,
    isGm: Boolean,
    currentUserId: String?,
    onGrantCoGm: () -> Unit,
    onRevokeCoGm: () -> Unit
) {
    val t = LocalXanteya.current
    var showConfirm by remember { mutableStateOf(false) }
    val isMe = player.userId == currentUserId
    val character = player.character

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(BorderStroke(1.dp, t.colors.line), RoundedCornerShape(t.corner))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // ── 1. Player Header (User info + Role + Co-GM Action) ────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar circle
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            player.isGm   -> t.colors.accent.copy(alpha = 0.25f)
                            player.isCoGm -> t.colors.hot.copy(alpha = 0.25f)
                            else          -> t.colors.bg
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(player.avatarEmoji, fontSize = 22.sp)
            }

            // Name + role info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = player.nickname + if (isMe) " (Вы)" else "",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (player.isGm) {
                        RoleBadge("ГМ", t.colors.accent, t.colors.accent.copy(alpha = 0.15f))
                    } else if (player.isCoGm) {
                        RoleBadge("Со-ГМ", t.colors.hot, t.colors.hot.copy(alpha = 0.15f))
                    }
                }
                Text(
                    text = when {
                        player.isGm   -> "Мастер игры"
                        player.isCoGm -> "Помощник мастера"
                        else          -> "Игрок"
                    },
                    color = t.colors.muted,
                    fontFamily = t.body,
                    style = MaterialTheme.typography.labelSmall
                )
            }

            // Co-GM toggle button (for GM only, excluding self)
            if (isGm && !player.isGm && !isMe) {
                if (player.isCoGm) {
                    FilledTonalButton(
                        onClick = { showConfirm = true },
                        shape = RoundedCornerShape(t.corner / 2),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = t.colors.hot.copy(alpha = 0.15f),
                            contentColor = t.colors.hot
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Со-ГМ", fontSize = 11.sp, fontFamily = t.display)
                    }
                } else {
                    OutlinedButton(
                        onClick = { showConfirm = true },
                        shape = RoundedCornerShape(t.corner / 2),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.accent),
                        border = BorderStroke(1.dp, t.colors.line),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Filled.Shield, contentDescription = null, modifier = Modifier.size(13.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Дать права", fontSize = 11.sp, fontFamily = t.display)
                    }
                }
            }
        }

        // ── 2. Character Data Section (HP, Броня, Блок, 6 Характеристик) ─────────
        if (character != null) {
            HorizontalDivider(
                color = t.colors.line.copy(alpha = 0.4f),
                thickness = 1.dp
            )

            // Character identity line
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = character.name.ifBlank { "Безымянный герой" },
                        color = t.colors.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = t.colors.bg.copy(alpha = 0.8f),
                    border = BorderStroke(1.dp, t.colors.line)
                ) {
                    Text(
                        text = "Ур. ${character.level} · ${character.className.ifBlank { "Искатель" }}",
                        fontSize = 11.sp,
                        color = t.colors.muted,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            // Combat Vitals (HP Bar, Armor Durability, Block DR)
            CharacterVitalsRow(character = character)

            // 6 System Characteristics (ТЕЛ, ДВИ, ВНИ, ИНТ, ВЛИ, ВОЛ)
            CharacterStatsRow(character = character)
        } else {
            // Subtle indication when no character is assigned
            Text(
                text = if (player.isGm) "Мастер кампании (без персонажа)" else "Персонаж ещё не создан",
                color = t.colors.muted.copy(alpha = 0.7f),
                fontFamily = t.body,
                fontSize = 12.sp,
                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }

    // Confirmation dialog for GM rights change
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = {
                Text(
                    if (player.isCoGm) "Убрать права Со-ГМ?" else "Дать права Со-ГМ?",
                    color = t.colors.text,
                    fontFamily = t.display
                )
            },
            text = {
                Text(
                    if (player.isCoGm)
                        "${player.nickname} потеряет права мастера игры."
                    else
                        "${player.nickname} получит права мастера игры и сможет редактировать персонажей.",
                    color = t.colors.muted,
                    fontFamily = t.body
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        if (player.isCoGm) onRevokeCoGm() else onGrantCoGm()
                    },
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isCoGm) t.colors.hot else t.colors.accent,
                        contentColor = t.colors.bg
                    )
                ) {
                    Text(if (player.isCoGm) "Убрать" else "Дать", fontFamily = t.display)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Отмена", color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }
}

@Composable
private fun CharacterVitalsRow(character: Character) {
    val t = LocalXanteya.current
    val hpRatio = if (character.maxHp > 0) {
        (character.currentHp.toFloat() / character.maxHp).coerceIn(0f, 1f)
    } else 0f

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // HP pill with visual bar
        Column(
            modifier = Modifier
                .weight(1.3f)
                .background(t.colors.bg.copy(alpha = 0.7f), RoundedCornerShape(t.corner / 2))
                .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.7f)), RoundedCornerShape(t.corner / 2))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "❤️ HP",
                    fontSize = 11.sp,
                    color = t.colors.hot,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${character.currentHp}/${character.maxHp}",
                    fontSize = 12.sp,
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(t.colors.line)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = hpRatio)
                        .background(if (hpRatio > 0.25f) t.colors.accent else t.colors.hot)
                )
            }
        }

        // Armor durability pill
        Column(
            modifier = Modifier
                .weight(1f)
                .background(t.colors.bg.copy(alpha = 0.7f), RoundedCornerShape(t.corner / 2))
                .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.7f)), RoundedCornerShape(t.corner / 2))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("🛡️ Броня", fontSize = 10.sp, color = t.colors.muted, fontFamily = t.display)
            Text(
                "${character.armor}/${character.maxArmor}",
                fontSize = 12.sp,
                color = t.colors.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold
            )
        }

        // Flat damage reduction (Block) pill
        Column(
            modifier = Modifier
                .weight(0.9f)
                .background(t.colors.bg.copy(alpha = 0.7f), RoundedCornerShape(t.corner / 2))
                .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.7f)), RoundedCornerShape(t.corner / 2))
                .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
            Text("🧱 Блок", fontSize = 10.sp, color = t.colors.muted, fontFamily = t.display)
            Text(
                "${character.armorBlock}",
                fontSize = 12.sp,
                color = t.colors.accent,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun CharacterStatsRow(character: Character) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StatMiniChip("ТЕЛ", character.constitution)
        StatMiniChip("ДВИ", character.movement)
        StatMiniChip("ВНИ", character.perception)
        StatMiniChip("ИНТ", character.intelligence)
        StatMiniChip("ВЛИ", character.influence)
        StatMiniChip("ВОЛ", character.willpower)
    }
}

@Composable
private fun StatMiniChip(label: String, value: Int) {
    val t = LocalXanteya.current
    val sign = if (value >= 0) "+$value" else "$value"
    Column(
        modifier = Modifier
            .background(t.colors.bg.copy(alpha = 0.6f), RoundedCornerShape(t.corner / 2))
            .border(BorderStroke(1.dp, t.colors.line.copy(alpha = 0.7f)), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            fontSize = 9.sp,
            color = t.colors.muted,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = sign,
            fontSize = 12.sp,
            color = if (value != 0) t.colors.accent else t.colors.text,
            fontFamily = t.display,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun RoleBadge(label: String, textColor: Color, bgColor: Color) {
    val t = LocalXanteya.current
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(t.corner / 2))
            .border(1.dp, textColor.copy(alpha = 0.5f), RoundedCornerShape(t.corner / 2))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = textColor, fontFamily = t.display, fontWeight = FontWeight.Bold)
    }
}
