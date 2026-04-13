package com.questkeep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.auth.AuthState
import com.questkeep.auth.AuthViewModel
import com.questkeep.ui.theme.*

@Composable
fun MenuScreen(
    onLogout: () -> Unit,
    isGm: Boolean = false,
    campaignId: String? = null,
    onNavigateToProfile: () -> Unit = {},
    onNavigateToGmNotes: () -> Unit = {},
    onNavigateToBestiary: () -> Unit = {},
    onNavigateToPlayerList: () -> Unit = {},
    onNavigateToItemCards: () -> Unit = {},
    onNavigateToInventory: () -> Unit = {},
    onNavigateToSharedInventory: () -> Unit = {},
    onNavigateToPlayerNotes: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(16.dp)
    ) {
        // Scrollable content — padded at bottom so logout button doesn't overlap
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 64.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(stringResource(R.string.menu), style = MaterialTheme.typography.titleLarge,
                color = TextPrimary, fontWeight = FontWeight.Bold)

            // Profile card (tappable → navigate to profile)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = DarkCard,
                onClick = onNavigateToProfile
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(PrimaryPurpleDark),
                        contentAlignment = Alignment.Center
                    ) {
                        // Always show emoji avatar if set, otherwise fallback to nickname letter
                        val emoji = user?.avatarEmoji
                        if (!emoji.isNullOrEmpty()) {
                            Text(emoji, fontSize = 24.sp)
                        } else {
                            Text(
                                text = user?.nickname?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = OnPrimary, fontWeight = FontWeight.Bold, fontSize = 24.sp
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.nickname ?: "Player", style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    Text("›", color = TextSecondary, fontSize = 20.sp)
                }
            }

            // Account section
            MenuSection(title = stringResource(R.string.account)) {
                MenuRow(label = stringResource(R.string.edit_profile), onClick = onNavigateToProfile)
                HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                MenuRow(label = "📝 Мои заметки", onClick = onNavigateToPlayerNotes)
            }

            // Campaign section — shown only when a campaign is selected
            if (campaignId != null) {
                MenuSection(title = "Кампания") {
                    MenuRow(label = "🎒 Инвентарь", onClick = onNavigateToInventory)
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "👥 Список игроков", onClick = onNavigateToPlayerList)
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "🏦 Банк кампании", onClick = onNavigateToSharedInventory)
                }
            }

            // GM Tools section — only shown when in a campaign as GM
            if (isGm && campaignId != null) {
                MenuSection(title = "Инструменты ГМ") {
                    MenuRow(label = stringResource(R.string.gm_notes), onClick = onNavigateToGmNotes)
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = stringResource(R.string.bestiary), onClick = onNavigateToBestiary)
                    HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "\uD83D\uDDE1 Карточки предметов", onClick = onNavigateToItemCards)
                }
            }
        }

        // Logout button pinned to the bottom — always visible
        OutlinedButton(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentRed)
        ) {
            Text(stringResource(R.string.logout), fontWeight = FontWeight.SemiBold)
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = DarkCard,
            title = { Text(stringResource(R.string.logout), color = TextPrimary) },
            text = { Text(stringResource(R.string.logout_confirm), color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { showLogoutConfirm = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text(stringResource(R.string.logout)) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}

@Composable
private fun MenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .padding(vertical = 4.dp)
    ) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = TextSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
        HorizontalDivider(color = DarkSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        content()
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = TextPrimary, style = MaterialTheme.typography.bodyMedium)
            Text("›", color = TextSecondary, fontSize = 18.sp)
        }
    }
}
