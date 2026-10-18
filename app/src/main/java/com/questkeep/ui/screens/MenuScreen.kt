package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
    onNavigateToRules: () -> Unit = {},
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val authState by authViewModel.authState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user
    var showLogoutConfirm by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(t.colors.bg)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(top = 16.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                stringResource(R.string.menu),
                style = MaterialTheme.typography.titleLarge,
                color = t.colors.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold
            )

            // Profile card (tappable → navigate to profile)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(t.corner),
                color = t.colors.panel,
                border = BorderStroke(1.dp, t.colors.line),
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
                            .background(t.colors.accent.copy(alpha = 0.2f))
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val emoji = user?.avatarEmoji
                        if (!emoji.isNullOrEmpty()) {
                            Text(emoji, fontSize = 24.sp)
                        } else {
                            Text(
                                text = user?.nickname?.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = t.colors.accent,
                                fontFamily = t.display,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            user?.nickname ?: "Игрок",
                            style = MaterialTheme.typography.titleMedium,
                            color = t.colors.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(user?.email ?: "", style = MaterialTheme.typography.bodySmall, color = t.colors.muted, fontFamily = t.body)
                    }
                    Text("›", color = t.colors.muted, fontSize = 20.sp)
                }
            }

            // Account section
            MenuSection(title = stringResource(R.string.account)) {
                MenuRow(label = stringResource(R.string.edit_profile), onClick = onNavigateToProfile)
                HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                MenuRow(label = "📝 Мои заметки", onClick = onNavigateToPlayerNotes)
            }

            // Rules & Knowledge base
            MenuSection(title = "База знаний") {
                MenuRow(label = "📜 Свод правил NEXUR v1.0", onClick = onNavigateToRules)
            }

            // World Themes (Ксантея UI)
            val context = androidx.compose.ui.platform.LocalContext.current
            val sessionManager = remember { com.questkeep.data.local.SessionManager(context.applicationContext) }
            val currentThemeKey by sessionManager.selectedTheme.collectAsState()

            MenuSection(title = "Тематика мира (Ксантея UI)") {
                AllXanteyaThemes.forEachIndexed { index, theme ->
                    val isSelected = theme.id == currentThemeKey
                    TextButton(
                        onClick = { sessionManager.saveTheme(theme.id) },
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(t.corner)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = theme.name,
                                    color = if (isSelected) theme.colors.accent else t.colors.text,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontFamily = t.display,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text(
                                    text = theme.subtitle,
                                    color = t.colors.muted,
                                    fontFamily = t.body,
                                    fontSize = 11.sp
                                )
                            }
                            if (isSelected) {
                                Text("✓ Активна", color = theme.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                    if (index < AllXanteyaThemes.size - 1) {
                        HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }

            // Campaign section — shown only when a campaign is selected
            if (campaignId != null) {
                MenuSection(title = "Кампания") {
                    MenuRow(label = "🎒 Инвентарь", onClick = onNavigateToInventory)
                    HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "👥 Список игроков", onClick = onNavigateToPlayerList)
                    HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "🏦 Банк кампании", onClick = onNavigateToSharedInventory)
                }
            }

            // GM Tools section — only shown when in a campaign as GM
            if (isGm && campaignId != null) {
                MenuSection(title = "Инструменты ГМ") {
                    MenuRow(label = stringResource(R.string.gm_notes), onClick = onNavigateToGmNotes)
                    HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = stringResource(R.string.bestiary), onClick = onNavigateToBestiary)
                    HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
                    MenuRow(label = "🗡 Карточки предметов", onClick = onNavigateToItemCards)
                }
            }

            Spacer(Modifier.height(8.dp))

            // Logout button inside the scrollable column — avoids collision with floating widgets
            OutlinedButton(
                onClick = { showLogoutConfirm = true },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.hot),
                border = BorderStroke(1.dp, t.colors.hot.copy(alpha = 0.5f))
            ) {
                Text(stringResource(R.string.logout), fontFamily = t.display, fontWeight = FontWeight.SemiBold)
            }
        }
    }

    if (showLogoutConfirm) {
        AlertDialog(
            onDismissRequest = { showLogoutConfirm = false },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text(stringResource(R.string.logout), color = t.colors.text, fontFamily = t.display) },
            text = { Text(stringResource(R.string.logout_confirm), color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { showLogoutConfirm = false; onLogout() },
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.hot, contentColor = t.colors.bg),
                    shape = RoundedCornerShape(t.corner)
                ) { Text(stringResource(R.string.logout), fontFamily = t.display) }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutConfirm = false }) {
                    Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }
}

@Composable
private fun MenuSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(1.dp, t.colors.line, RoundedCornerShape(t.corner))
            .padding(vertical = 4.dp)
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = t.colors.muted,
            fontFamily = t.display,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        HorizontalDivider(color = t.colors.line, modifier = Modifier.padding(horizontal = 16.dp))
        content()
    }
}

@Composable
private fun MenuRow(label: String, onClick: () -> Unit) {
    val t = LocalXanteya.current
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        shape = RoundedCornerShape(t.corner)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = t.colors.text, fontFamily = t.display, style = MaterialTheme.typography.bodyMedium)
            Text("›", color = t.colors.muted, fontSize = 18.sp)
        }
    }
}
