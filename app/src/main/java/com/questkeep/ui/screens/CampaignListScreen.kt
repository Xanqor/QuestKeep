package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.domain.entities.Campaign
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.CampaignUiEvent
import com.questkeep.viewmodel.CampaignViewModel

@Composable
fun CampaignListScreen(
    onCampaignSelected: (campaignId: String, isGm: Boolean, gmId: String) -> Unit,
    viewModel: CampaignViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val campaigns by viewModel.campaigns.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var showJoinDialog   by remember { mutableStateOf(false) }
    var editingCampaign  by remember { mutableStateOf<Campaign?>(null) }
    var deletingCampaign by remember { mutableStateOf<Campaign?>(null) }
    var leavingCampaign  by remember { mutableStateOf<Campaign?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CampaignUiEvent.CreateSuccess -> {
                    showCreateDialog = false
                    onCampaignSelected(event.campaignId, true, viewModel.currentUserId ?: "")
                }
                is CampaignUiEvent.JoinSuccess -> {
                    showJoinDialog = false
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
        containerColor = t.colors.bg,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SmallFloatingActionButton(
                    onClick = { showJoinDialog = true },
                    containerColor = t.colors.panel,
                    contentColor = t.colors.accent,
                    shape = RoundedCornerShape(t.corner)
                ) {
                    Text(stringResource(R.string.join_short), fontFamily = t.display, fontWeight = FontWeight.Bold)
                }
                FloatingActionButton(
                    onClick = { showCreateDialog = true },
                    containerColor = t.colors.accent,
                    contentColor = t.colors.bg,
                    shape = RoundedCornerShape(t.corner)
                ) {
                    Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.create_campaign))
                }
            }
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.campaigns),
                    style = MaterialTheme.typography.titleLarge,
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold
                )
                if (campaigns.isNotEmpty()) {
                    FilledTonalButton(
                        onClick = { showCreateDialog = true },
                        shape = RoundedCornerShape(t.corner),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = t.colors.accent.copy(alpha = 0.15f),
                            contentColor = t.colors.accent
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(stringResource(R.string.create_campaign), fontFamily = t.display, fontSize = 12.sp)
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            if (isLoading && campaigns.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = t.colors.accent)
                }
            } else if (campaigns.isEmpty()) {
                OnboardingHints(
                    onCreateClick = { showCreateDialog = true },
                    onJoinClick   = { showJoinDialog = true }
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp)
                ) {
                    items(campaigns, key = { it.id }) { campaign ->
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
                    item { Spacer(Modifier.height(100.dp)) }
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
            containerColor   = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text(stringResource(R.string.delete_campaign), color = t.colors.hot, fontFamily = t.display, fontWeight = FontWeight.Bold) },
            text  = { Text(stringResource(R.string.delete_campaign_confirm), color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteCampaign(campaign.id) },
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.hot, contentColor = t.colors.bg)
                ) {
                    Text(stringResource(R.string.delete_campaign), fontFamily = t.display)
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingCampaign = null }) {
                    Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }

    // Leave confirmation
    leavingCampaign?.let { campaign ->
        AlertDialog(
            onDismissRequest = { leavingCampaign = null },
            containerColor   = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text(stringResource(R.string.leave_campaign), color = t.colors.hot, fontFamily = t.display, fontWeight = FontWeight.Bold) },
            text  = { Text(stringResource(R.string.leave_campaign_confirm), color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { viewModel.leaveCampaign(campaign.id); leavingCampaign = null },
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.hot, contentColor = t.colors.bg)
                ) {
                    Text(stringResource(R.string.leave_campaign), fontFamily = t.display)
                }
            },
            dismissButton = {
                TextButton(onClick = { leavingCampaign = null }) {
                    Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }
}

@Composable
private fun CampaignOptionsDialog(
    campaign: Campaign, isLoading: Boolean,
    onDismiss: () -> Unit, onSave: (String, String, String, Int) -> Unit,
    onDelete: () -> Unit
) {
    val t = LocalXanteya.current
    var name        by remember { mutableStateOf(campaign.name) }
    var description by remember { mutableStateOf(campaign.description) }
    var gameSystem  by remember { mutableStateOf(campaign.gameSystem) }
    var maxPlayers  by remember { mutableStateOf(campaign.maxPlayers.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text(stringResource(R.string.edit_campaign), color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.campaign_name), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.campaign_description), color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = gameSystem, onValueChange = { gameSystem = it },
                    label = { Text(stringResource(R.string.game_system), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = maxPlayers, onValueChange = { if (it.all { c -> c.isDigit() }) maxPlayers = it },
                    label = { Text(stringResource(R.string.max_players), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = campaignFieldColors()
                )
                TextButton(onClick = onDelete, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.delete_campaign), color = t.colors.hot, fontFamily = t.display)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(name, description, gameSystem, maxPlayers.toIntOrNull() ?: 5) },
                enabled = name.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = t.colors.bg)
                else Text(stringResource(R.string.save), fontFamily = t.display)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CampaignCard(
    campaign: Campaign, isGm: Boolean,
    onClick: () -> Unit, onLongClick: () -> Unit
) {
    val t = LocalXanteya.current
    val clipboardManager = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current
    var copied by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(t.corner),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        border = BorderStroke(1.dp, if (isGm) t.colors.accent else t.colors.line)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header: System Pill & Role Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = t.colors.bg,
                    border = BorderStroke(1.dp, t.colors.line)
                ) {
                    Text(
                        text = if (campaign.gameSystem.isNotBlank()) campaign.gameSystem else "RPG",
                        style = MaterialTheme.typography.labelSmall,
                        color = t.colors.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                val roleLabel = if (isGm) "👑 Гейм-мастер" else "🗡 Игрок"
                val roleBg    = if (isGm) t.colors.accent.copy(alpha = 0.2f) else t.colors.bg
                val roleColor = if (isGm) t.colors.accent else t.colors.muted

                Surface(
                    shape = RoundedCornerShape(t.corner / 2),
                    color = roleBg,
                    border = BorderStroke(1.dp, if (isGm) t.colors.accent else t.colors.line)
                ) {
                    Text(
                        text = roleLabel,
                        fontSize = 11.sp,
                        color = roleColor,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = campaign.name,
                style = MaterialTheme.typography.titleLarge,
                color = t.colors.text,
                fontFamily = t.display,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            )

            if (campaign.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = campaign.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = t.colors.muted,
                    fontFamily = t.body,
                    maxLines = 2
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider(color = t.colors.line)
            Spacer(Modifier.height(10.dp))

            // Bottom row: Player counter and Invite Code
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = Icons.Filled.Groups,
                        contentDescription = null,
                        tint = t.colors.muted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "${campaign.playerIds.size} / ${campaign.maxPlayers} игроков",
                        style = MaterialTheme.typography.labelMedium,
                        color = t.colors.muted,
                        fontFamily = t.body
                    )
                }

                if (isGm) {
                    Surface(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(campaign.inviteCode))
                            haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.TextHandleMove)
                            copied = true
                        },
                        shape = RoundedCornerShape(t.corner / 2),
                        color = t.colors.bg,
                        border = BorderStroke(1.dp, t.colors.accent.copy(alpha = 0.6f))
                    ) {
                        Text(
                            text = if (copied) "✓ Скопирован" else "Код: ${campaign.inviteCode}",
                            style = MaterialTheme.typography.labelSmall,
                            color = t.colors.accent,
                            fontFamily = t.display,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Открыть лист →",
                        style = MaterialTheme.typography.labelSmall,
                        color = t.colors.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingHints(onCreateClick: () -> Unit, onJoinClick: () -> Unit) {
    val t = LocalXanteya.current
    Column(modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.no_campaigns), color = t.colors.muted,
            fontFamily = t.body, style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        OnboardingCard(
            icon = Icons.Filled.Shield,
            title = stringResource(R.string.gm_role),
            hint = stringResource(R.string.create_campaign_hint),
            actionLabel = stringResource(R.string.create_campaign),
            onClick = onCreateClick
        )
        Spacer(Modifier.height(12.dp))
        OnboardingCard(
            icon = Icons.Filled.Person,
            title = stringResource(R.string.player_role),
            hint = stringResource(R.string.join_campaign_hint),
            actionLabel = stringResource(R.string.join_campaign),
            onClick = onJoinClick
        )
    }
}

@Composable
private fun OnboardingCard(
    icon: ImageVector,
    title: String, hint: String, actionLabel: String,
    onClick: () -> Unit
) {
    val t = LocalXanteya.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(t.colors.panel, RoundedCornerShape(t.corner))
            .border(BorderStroke(1.dp, t.colors.line), RoundedCornerShape(t.corner))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier.size(48.dp).background(t.colors.bg, RoundedCornerShape(t.corner / 2))
                .border(1.dp, t.colors.line, RoundedCornerShape(t.corner / 2)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = t.colors.accent, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.titleSmall)
            Text(hint, color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.bodySmall)
        }
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(t.corner),
            colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(actionLabel, fontFamily = t.display, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun CreateCampaignDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: (String, String, String, Int) -> Unit) {
    val t = LocalXanteya.current
    var name        by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var gameSystem  by remember { mutableStateOf("QuestKeep RPG") }
    var maxPlayers  by remember { mutableStateOf("5") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text(stringResource(R.string.create_campaign), color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.campaign_name), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text(stringResource(R.string.campaign_description), color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(), maxLines = 3,
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = gameSystem, onValueChange = { gameSystem = it },
                    label = { Text(stringResource(R.string.game_system), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    colors = campaignFieldColors()
                )
                OutlinedTextField(
                    value = maxPlayers, onValueChange = { if (it.all { c -> c.isDigit() }) maxPlayers = it },
                    label = { Text(stringResource(R.string.max_players), color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = campaignFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, gameSystem, maxPlayers.toIntOrNull() ?: 5) },
                enabled = name.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = t.colors.bg)
                else Text(stringResource(R.string.create), fontFamily = t.display)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body) } }
    )
}

@Composable
private fun JoinCampaignDialog(isLoading: Boolean, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    val t = LocalXanteya.current
    var code by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text(stringResource(R.string.join_campaign), color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = code, onValueChange = { code = it.uppercase().take(6) },
                label = { Text(stringResource(R.string.invite_code), color = t.colors.muted, fontFamily = t.body) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                colors = campaignFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(code) },
                enabled = code.length == 6 && !isLoading,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) {
                if (isLoading) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = t.colors.bg)
                else Text(stringResource(R.string.join), fontFamily = t.display)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body) } }
    )
}

@Composable
private fun campaignFieldColors(): TextFieldColors {
    val t = LocalXanteya.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = t.colors.accent,
        unfocusedBorderColor = t.colors.line,
        focusedTextColor = t.colors.text,
        unfocusedTextColor = t.colors.text,
        focusedLabelColor = t.colors.accent,
        unfocusedLabelColor = t.colors.muted,
        cursorColor = t.colors.accent,
        focusedContainerColor = t.colors.bg,
        unfocusedContainerColor = t.colors.bg
    )
}
