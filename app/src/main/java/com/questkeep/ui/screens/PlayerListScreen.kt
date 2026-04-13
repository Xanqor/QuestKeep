package com.questkeep.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
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
    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.loadPlayers(campaignId)
    }

    val players by viewModel.players.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isGm = viewModel.isCurrentUserGm()

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Список игроков",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
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
                        Text("Кампания не выбрана", color = TextSecondary)
                    }
                }
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryPurple)
                    }
                }
                players.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "В кампании пока нет игроков",
                            color = TextSecondary,
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
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        item {
                            Text(
                                "Участники кампании",
                                style = MaterialTheme.typography.labelMedium,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        items(players, key = { it.userId }) { player ->
                            PlayerCard(
                                player = player,
                                isGm = isGm,
                                currentUserId = viewModel.currentUserId,
                                onGrantCoGm = {
                                    if (campaignId != null) viewModel.grantCoGm(campaignId, player.userId)
                                },
                                onRevokeCoGm = {
                                    if (campaignId != null) viewModel.revokeCoGm(campaignId, player.userId)
                                }
                            )
                        }
                        item { Spacer(Modifier.height(80.dp)) }
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
    var showConfirm by remember { mutableStateOf(false) }
    val isMe = player.userId == currentUserId

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Avatar circle
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    when {
                        player.isGm    -> SecondaryGold.copy(alpha = 0.2f)
                        player.isCoGm  -> PrimaryPurple.copy(alpha = 0.25f)
                        else           -> DarkSurfaceVariant
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
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (player.isGm) {
                    RoleBadge("ГМ", SecondaryGold, SecondaryGold.copy(alpha = 0.15f))
                } else if (player.isCoGm) {
                    RoleBadge("Со-ГМ", PrimaryPurpleLight, PrimaryPurpleDark)
                }
            }
            Text(
                text = when {
                    player.isGm   -> "Мастер игры"
                    player.isCoGm -> "Помощник мастера"
                    else          -> "Игрок"
                },
                color = TextDisabled,
                style = MaterialTheme.typography.labelSmall
            )
        }

        // Co-GM toggle (only GM can do this, and only for non-GM players, and not for self)
        if (isGm && !player.isGm && !isMe) {
            if (player.isCoGm) {
                FilledTonalButton(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = PrimaryPurpleDark,
                        contentColor = PrimaryPurpleLight
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Со-ГМ", fontSize = 12.sp)
                }
            } else {
                OutlinedButton(
                    onClick = { showConfirm = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(
                        Icons.Filled.Shield,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text("Дать права", fontSize = 12.sp)
                }
            }
        }
    }

    // Confirmation dialog for GM rights change
    if (showConfirm) {
        AlertDialog(
            onDismissRequest = { showConfirm = false },
            containerColor = DarkCard,
            title = {
                Text(
                    if (player.isCoGm) "Убрать права Со-ГМ?" else "Дать права Со-ГМ?",
                    color = TextPrimary
                )
            },
            text = {
                Text(
                    if (player.isCoGm)
                        "${player.nickname} потеряет права мастера игры."
                    else
                        "${player.nickname} получит права мастера игры и сможет редактировать персонажей.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showConfirm = false
                        if (player.isCoGm) onRevokeCoGm() else onGrantCoGm()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (player.isCoGm) AccentRed else PrimaryPurple
                    )
                ) {
                    Text(if (player.isCoGm) "Убрать" else "Дать")
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirm = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun RoleBadge(label: String, textColor: androidx.compose.ui.graphics.Color, bgColor: androidx.compose.ui.graphics.Color) {
    Box(
        modifier = Modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 10.sp, color = textColor, fontWeight = FontWeight.Bold)
    }
}
