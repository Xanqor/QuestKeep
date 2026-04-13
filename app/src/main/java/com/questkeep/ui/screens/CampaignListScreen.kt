package com.questkeep.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.domain.entities.Campaign
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.CampaignUiEvent
import com.questkeep.viewmodel.CampaignViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun CampaignListScreen(
    /** campaignId, isGm, gmId */
    onCampaignSelected: (String, Boolean, String) -> Unit,
    viewModel: CampaignViewModel = hiltViewModel()
) {
    val campaigns by viewModel.campaigns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog by remember { mutableStateOf(false) }
    var editingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var deletingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var leavingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.events.collectLatest { event ->
            when (event) {
                is CampaignUiEvent.CreateSuccess -> {
                    showCreateDialog = false
                    // For new campaigns the GM is the current user; gmId is resolved later in UI
                    onCampaignSelected(event.campaignId, true, viewModel.currentUserId ?: "")
                }
                is CampaignUiEvent.JoinSuccess -> {
                    showJoinDialog = false
                    // For joined campaigns we don't know gmId yet; empty string — resolved via campaign data
                    val gmId = campaigns.find { it.id == event.campaignId }?.gmId ?: ""
                    onCampaignSelected(event.campaignId, false, gmId)
                }
                is CampaignUiEvent.DeleteSuccess -> { deletingCampaign = null }
                is CampaignUiEvent.UpdateSuccess -> { editingCampaign = null }
                is CampaignUiEvent.Error -> snackbarMessage = event.message
            }
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { snackbarHostState.showSnackbar(it); snackbarMessage = null }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(onClick = { showJoinDialog = true }, containerColor = DarkCard) {
                    Text(stringResource(R.string.join_short), color = PrimaryPurpleLight, fontWeight = FontWeight.Bold)
                }
                FloatingActionButton(onClick = { showCreateDialog = true }, containerColor = PrimaryPurple) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_campaign))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(stringResource(R.string.campaigns),
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                // Separate "Create" button visible in the header when campaigns already exist
                if (campaigns.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color(0xFF1A0A2E),
                            contentColor = PrimaryPurpleLight
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null,
                            modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.create_campaign), fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (isLoading && campaigns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            } else if (campaigns.isEmpty()) {
                OnboardingHints(
                    onCreateClick = { showCreateDialog = true },
                    onJoinClick   = { showJoinDialog = true }
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(campaigns) { campaign ->
                        CampaignCard(
                            campaign = campaign,
                            isGm     = viewModel.isGm(campaign),
                            onClick  = { onCampaignSelected(campaign.id, viewModel.isGm(campaign), campaign.gmId) },
                            onLongClick = {
                                if (viewModel.isGm(campaign)) editingCampaign = campaign
                                else leavingCampaign = campaign
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreateDialog) {
        CreateCampaignDialog(isLoading, { showCreateDialog = false }) { n, d, s, m -> viewModel.createCampaign(n, d, s, m) }
    }
    if (showJoinDialog) {
        JoinCampaignDialog(isLoading, { showJoinDialog = false }) { code -> viewModel.joinCampaign(code) }
    }

    // GM: edit/delete options
    editingCampaign?.let { campaign ->
        CampaignOptionsDialog(
            campaign   = campaign,
            isLoading  = isLoading,
            onDismiss  = { editingCampaign = null },
            onSave     = { n, d, s, m -> viewModel.updateCampaign(campaign, n, d, s, m) },
            onDelete   = { deletingCampaign = campaign; editingCampaign = null }
        )
    }

    // Delete confirmation
    deletingCampaign?.let { campaign ->
        AlertDialog(
            onDismissRequest = { deletingCampaign = null },
            containerColor   = DarkCard,
            title = { Text(stringResource(R.string.delete_campaign), color = AccentRed) },
            text  = { Text(stringResource(R.string.delete_campaign_confirm), color = TextSecondary) },
            confirmButton = {
                Button(onClick = { viewModel.deleteCampaign(campaign.id) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)) {
                    Text(stringResource(R.string.delete_campaign))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCampaign = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }

    // Player: leave confirmation
    leavingCampaign?.let { campaign ->
        AlertDialog(
            onDismissRequest = { leavingCampaign = null },
            containerColor   = DarkCard,
            title = { Text(stringResource(R.string.leave_campaign), color = TextPrimary) },
            text  = { Text(stringResource(R.string.leave_campaign_confirm), color = TextSecondary) },
            confirmButton = {
                Button(onClick = { viewModel.leaveCampaign(campaign.id); leavingCampaign = null },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                    Text(stringResource(R.string.leave_campaign))
                }
            },
            dismissButton = {
                TextButton(onClick = { leavingCampaign = null }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}

// --- Campaign Options Dialog (GM long-press) ---

@Composable
private fun CampaignOptionsDialog(
    campaign: Campaign, isLoading: Boolean,
    onDismiss: () -> Unit, onSave: (String, String, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    var name        by remember { mutableStateOf(campaign.name) }
    var description by remember { mutableStateOf(campaign.description) }
    var gameSystem  by remember { mutableStateOf(campaign.gameSystem) }
    var maxPlayers  by remember { mutableStateOf(campaign.maxPlayers.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = DarkCard,
        title = { Text(stringResource(R.string.edit_campaign), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.campaign_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.campaign_description)) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3)
                OutlinedTextField(value = gameSystem, onValueChange = { gameSystem = it },
                    label = { Text(stringResource(R.string.game_system)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxPlayers, onValueChange = { maxPlayers = it },
                    label = { Text(stringResource(R.string.max_players)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
                // Delete option
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_campaign), color = AccentRed)
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, description, gameSystem, maxPlayers.toIntOrNull() ?: 5) },
                enabled = name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
                else Text(stringResource(R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) }
        }
    )
}

// --- Campaign Card (long-press for options) ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CampaignCard(
    campaign: Campaign, isGm: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier.size(42.dp)
                .background(if (isGm) Color(0xFF3A2A00) else Color(0xFF1A0A2E), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            androidx.compose.material3.Icon(
                imageVector  = if (isGm) Icons.Filled.Shield else Icons.Filled.Groups,
                contentDescription = null,
                tint = if (isGm) SecondaryGold else PrimaryPurpleLight,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(campaign.name, style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                val roleLabel = if (isGm) stringResource(R.string.gm_role) else stringResource(R.string.player_role)
                val roleBg    = if (isGm) Color(0xFF3A2A00) else Color(0xFF1A0A2E)
                val roleColor = if (isGm) SecondaryGold     else PrimaryPurpleLight
                Text(roleLabel, fontSize = 11.sp, color = roleColor,
                    modifier = Modifier.background(roleBg, RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp))
            }
            Text(campaign.gameSystem, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            Text(stringResource(R.string.players_count, campaign.playerIds.size, campaign.maxPlayers),
                style = MaterialTheme.typography.labelSmall, color = TextDisabled)
            Text(if (isGm) stringResource(R.string.campaign_role_hint_gm) else stringResource(R.string.campaign_role_hint_player),
                style = MaterialTheme.typography.labelSmall,
                color = if (isGm) SecondaryGold.copy(alpha = 0.6f) else PrimaryPurpleLight.copy(alpha = 0.5f))
        }
        if (isGm) {
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.invite_code), style = MaterialTheme.typography.labelSmall, color = TextDisabled)
                Text(campaign.inviteCode, style = MaterialTheme.typography.labelLarge,
                    color = SecondaryGoldLight, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// --- Onboarding (empty state) ---

@Composable
private fun OnboardingHints(onCreateClick: () -> Unit, onJoinClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.no_campaigns), color = TextSecondary,
            style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OnboardingCard(Icons.Filled.Shield, SecondaryGold, Color(0xFF3A2A00),
            stringResource(R.string.gm_role), stringResource(R.string.create_campaign_hint),
            stringResource(R.string.create_campaign), PrimaryPurple, onCreateClick)
        Spacer(Modifier.height(12.dp))
        OnboardingCard(Icons.Filled.Person, PrimaryPurpleLight, Color(0xFF1A0A2E),
            stringResource(R.string.player_role), stringResource(R.string.join_campaign_hint),
            stringResource(R.string.join_campaign), DarkCard, onJoinClick, PrimaryPurpleLight)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun OnboardingCard(
    icon: ImageVector, iconTint: Color, iconBg: Color,
    title: String, hint: String, actionLabel: String,
    actionColor: Color, onClick: () -> Unit,
    actionContentColor: Color = OnPrimary
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(DarkCard, RoundedCornerShape(12.dp))
            .combinedClickable(onClick = onClick, onLongClick = {})
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(modifier = Modifier.size(48.dp).background(iconBg, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            Text(hint, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onClick,
            colors = ButtonDefaults.buttonColors(containerColor = actionColor, contentColor = actionContentColor),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
            Text(actionLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

// --- Create / Join dialogs ---

@Composable
private fun CreateCampaignDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String, Int) -> Unit) {
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var gameSystem  by remember { mutableStateOf("QuestKeep RPG") }
    var maxPlayers  by remember { mutableStateOf("5") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkCard,
        title = { Text(stringResource(R.string.create_campaign), color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.campaign_name)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.campaign_description)) }, modifier = Modifier.fillMaxWidth(), maxLines = 3)
                OutlinedTextField(value = gameSystem, onValueChange = { gameSystem = it },
                    label = { Text(stringResource(R.string.game_system)) }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = maxPlayers, onValueChange = { maxPlayers = it },
                    label = { Text(stringResource(R.string.max_players)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number))
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(name, description, gameSystem, maxPlayers.toIntOrNull() ?: 5) },
                enabled = name.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
                else Text(stringResource(R.string.create))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) } }
    )
}

@Composable
private fun JoinCampaignDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var code by remember { mutableStateOf("") }
    AlertDialog(onDismissRequest = onDismiss, containerColor = DarkCard,
        title = { Text(stringResource(R.string.join_campaign), color = TextPrimary) },
        text = {
            OutlinedTextField(value = code, onValueChange = { code = it.uppercase().take(6) },
                label = { Text(stringResource(R.string.invite_code)) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters))
        },
        confirmButton = {
            Button(onClick = { onConfirm(code) }, enabled = code.length == 6 && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
                else Text(stringResource(R.string.join))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = TextSecondary) } }
    )
}
