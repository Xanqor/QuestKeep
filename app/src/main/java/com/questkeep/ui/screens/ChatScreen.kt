package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.auth.AuthState
import com.questkeep.auth.AuthViewModel
import com.questkeep.ui.components.ChatMessageItem
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.ChatViewModel
import com.questkeep.viewmodel.PlayerInfo
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
    campaignId: String?,
    viewModel: ChatViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val messages by viewModel.messages.collectAsState()
    val isSending by viewModel.isSending.collectAsState()
    val isUploadingImage by viewModel.isUploadingImage.collectAsState()
    val imageUploadError by viewModel.imageUploadError.collectAsState()
    val authState by authViewModel.authState.collectAsState()
    val senderName = (authState as? AuthState.Authenticated)?.user?.nickname ?: "Player"
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    var inputText by remember { mutableStateOf("") }

    val campaignPlayers by viewModel.campaignPlayers.collectAsState()
    val whisperTarget by viewModel.whisperTarget.collectAsState()

    // Image picker launcher
    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && campaignId != null) {
            viewModel.sendImageMessage(campaignId, uri, senderName)
        }
    }

    // @mention autocomplete state
    // We show suggestions when inputText contains "@" without a trailing space after it
    val atIndex = remember(inputText) { inputText.lastIndexOf('@') }
    val mentionPrefix = remember(inputText, atIndex) {
        if (atIndex >= 0 && !inputText.substring(atIndex).contains(' ')) {
            inputText.substring(atIndex + 1)
        } else null
    }
    val mentionSuggestions = remember(mentionPrefix, campaignPlayers) {
        if (mentionPrefix != null) {
            campaignPlayers.filter { it.name.contains(mentionPrefix, ignoreCase = true) }
        } else emptyList()
    }
    val showMentionDropdown = mentionSuggestions.isNotEmpty()

    LaunchedEffect(campaignId) {
        if (campaignId != null) {
            viewModel.loadMessages(campaignId)
            viewModel.loadCampaignPlayers(campaignId, senderName)
        }
    }

    // Show snackbar on image upload error
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(imageUploadError) {
        val err = imageUploadError
        if (err != null) {
            snackbarHostState.showSnackbar(err)
            viewModel.clearImageUploadError()
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            scope.launch { listState.animateScrollToItem(messages.size - 1) }
        }
    }

    Scaffold(
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { scaffoldPadding ->
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(scaffoldPadding)
            .background(DarkBackground)
    ) {
        if (campaignId == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_campaign_selected), color = TextSecondary)
            }
            return@Column
        }

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                ChatMessageItem(
                    message = message,
                    isCurrentUser = message.senderId == viewModel.currentUserId
                )
            }
        }

        // Whisper target selector (only if there are other players)
        if (campaignPlayers.isNotEmpty()) {
            WhisperTargetRow(
                players = campaignPlayers,
                selectedTarget = whisperTarget,
                onTargetSelected = { viewModel.setWhisperTarget(it) }
            )
        }

        // @mention suggestions dropdown (shown above input)
        if (showMentionDropdown) {
            MentionSuggestionsRow(
                suggestions = mentionSuggestions,
                onSelect = { player ->
                    // Replace "@prefix" with the player's name + set as whisper target
                    inputText = if (atIndex >= 0) inputText.substring(0, atIndex) else inputText
                    viewModel.setWhisperTarget(player)
                    focusManager.clearFocus()
                }
            )
        }

        // Input area — tinted purple if whisper mode active
        val inputBg = if (whisperTarget != null) Color(0xFF1A0A2E) else DarkSurface

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(inputBg)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Image picker button
            IconButton(
                onClick = { imageLauncher.launch("image/*") },
                enabled = !isUploadingImage,
                modifier = Modifier.size(40.dp)
            ) {
                if (isUploadingImage) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = PrimaryPurpleLight,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        Icons.Filled.Image,
                        contentDescription = "Отправить изображение",
                        tint = TextSecondary
                    )
                }
            }

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        if (whisperTarget != null)
                            "${stringResource(R.string.whisper_to)} ${whisperTarget!!.name}…"
                        else
                            stringResource(R.string.message_hint),
                        color = TextDisabled
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (inputText.isNotBlank() && !isSending) {
                        viewModel.sendMessage(campaignId, inputText, senderName)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                }),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = if (whisperTarget != null) PrimaryPurpleLight else PrimaryPurple,
                    unfocusedBorderColor = if (whisperTarget != null) PrimaryPurple else DarkSurfaceVariant,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )
            IconButton(
                onClick = {
                    if (inputText.isNotBlank() && !isSending) {
                        viewModel.sendMessage(campaignId, inputText, senderName)
                        inputText = ""
                        focusManager.clearFocus()
                    }
                },
                enabled = inputText.isNotBlank() && !isSending,
                modifier = Modifier
                    .background(
                        if (whisperTarget != null) PrimaryPurpleLight else PrimaryPurple,
                        RoundedCornerShape(50)
                    )
                    .size(48.dp)
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = stringResource(R.string.send),
                    tint = OnPrimary
                )
            }
        }
        // Spacer to clear the dice FAB
        Spacer(
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
                .background(inputBg)
        )
    }
    }
}

/**
 * Horizontal popup row showing @mention suggestions above the input field.
 * Tapping a suggestion sets whisper target and clears the @... from input.
 */
@Composable
private fun MentionSuggestionsRow(
    suggestions: List<PlayerInfo>,
    onSelect: (PlayerInfo) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF120826))
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("@", color = PrimaryPurpleLight, fontSize = 12.sp,
            fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 2.dp))
        suggestions.forEach { player ->
            Box(
                modifier = Modifier
                    .background(Color(0xFF1A0A2E), RoundedCornerShape(12.dp))
                    .border(1.dp, PrimaryPurple, RoundedCornerShape(12.dp))
                    .clickable { onSelect(player) }
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(player.name, color = PrimaryPurpleLight, fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/**
 * Horizontal row of player chips for whisper target selection.
 * First chip is always "Всем" (public). Other chips are players.
 */
@Composable
private fun WhisperTargetRow(
    players: List<PlayerInfo>,
    selectedTarget: PlayerInfo?,
    onTargetSelected: (PlayerInfo?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "🤫",
            fontSize = 14.sp,
            modifier = Modifier.padding(end = 2.dp)
        )

        // "Всем" chip — clears whisper target
        WhisperChip(
            label = stringResource(R.string.whisper_public),
            isSelected = selectedTarget == null,
            selectedColor = DarkSurfaceVariant,
            onClick = { onTargetSelected(null) }
        )

        // Player chips
        players.forEach { player ->
            WhisperChip(
                label = player.name,
                isSelected = selectedTarget?.userId == player.userId,
                selectedColor = PrimaryPurpleDark,
                onClick = {
                    onTargetSelected(if (selectedTarget?.userId == player.userId) null else player)
                }
            )
        }
    }
}

@Composable
private fun WhisperChip(
    label: String,
    isSelected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit
) {
    val bg = if (isSelected) selectedColor else DarkBackground
    val border = if (isSelected) PrimaryPurple else DarkSurfaceVariant
    val textColor = if (isSelected) PrimaryPurpleLight else TextSecondary

    Box(
        modifier = Modifier
            .background(bg, RoundedCornerShape(16.dp))
            .border(1.dp, border, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = textColor, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal)
    }
}
