package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.ProfileUiEvent
import com.questkeep.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val user by viewModel.user.collectAsState()
    val isUploadingAvatar by viewModel.isUploadingAvatar.collectAsState()
    val isSaving by viewModel.isSaving.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var nickname by remember(user) { mutableStateOf(user?.nickname ?: "") }
    var selectedEmoji by remember(user) { mutableStateOf(user?.avatarEmoji ?: "🧙") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val context = androidx.compose.ui.platform.LocalContext.current
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(
                    it,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: Exception) {}
            viewModel.uploadAvatar(it)
        }
    }

    // 32 Curated RPG Avatars
    val emojiList = listOf(
        "🧙", "⚔️", "🛡️", "🏹", "🗡️", "🔮", "💀", "🐉",
        "🦅", "🐺", "🦁", "🐻", "🔥", "❄️", "⚡", "🌊",
        "🌑", "☀️", "🌿", "💎", "👑", "🎭", "🗺️", "📜",
        "🍺", "🎲", "💣", "🏰", "⚗️", "🧿", "🪄", "🌌"
    )

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is ProfileUiEvent.SaveSuccess -> {
                    snackbarHostState.showSnackbar("Профиль успешно сохранён")
                    onBack()
                }
                is ProfileUiEvent.Error -> {
                    errorMessage = event.message
                    snackbarHostState.showSnackbar(event.message)
                }
            }
        }
    }

    Scaffold(
        containerColor = t.colors.bg,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Профиль искателя",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = t.colors.text
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 24.dp)
                .padding(bottom = 60.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ─── Hero Avatar with Theme Border & Photo Picker Overlay ─────────────────
            Box(
                modifier = Modifier.padding(bottom = 20.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier
                        .size(108.dp)
                        .clip(CircleShape)
                        .background(t.colors.panel)
                        .border(
                            width = 3.dp,
                            color = t.colors.accent,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    val avatarUrl = user?.avatarUrl
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Аватар",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().clip(CircleShape)
                        )
                    } else {
                        Text(text = selectedEmoji, fontSize = 48.sp, textAlign = TextAlign.Center)
                    }

                    if (isUploadingAvatar) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.6f)),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(36.dp),
                                color = t.colors.accent,
                                strokeWidth = 3.dp
                            )
                        }
                    }
                }

                // Camera button overlay
                Surface(
                    onClick = {
                        imagePickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = CircleShape,
                    color = t.colors.accent,
                    shadowElevation = 4.dp,
                    border = BorderStroke(2.dp, t.colors.bg),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.CameraAlt,
                            contentDescription = "Загрузить фото",
                            tint = t.colors.bg,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Text(
                text = user?.email?.ifBlank { "Аккаунт искателя" } ?: "Гость",
                style = MaterialTheme.typography.bodySmall,
                color = t.colors.muted,
                fontFamily = t.body
            )

            Spacer(modifier = Modifier.height(24.dp))

            // ─── Nickname Input Card ──────────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = t.colors.panel),
                shape = RoundedCornerShape(t.corner),
                border = BorderStroke(1.dp, t.colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Имя игрока / Никнейм",
                        style = MaterialTheme.typography.labelMedium,
                        color = t.colors.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = {
                            nickname = it
                            errorMessage = null
                        },
                        placeholder = { Text("Введите никнейм...", color = t.colors.muted, fontFamily = t.body) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(t.corner),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = t.colors.text,
                            unfocusedTextColor = t.colors.text,
                            focusedBorderColor = t.colors.accent,
                            unfocusedBorderColor = t.colors.line,
                            cursorColor = t.colors.accent,
                            focusedContainerColor = t.colors.bg,
                            unfocusedContainerColor = t.colors.bg
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ─── RPG Emoji Avatar Gallery ─────────────────────────────────────────────
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = t.colors.panel),
                shape = RoundedCornerShape(t.corner),
                border = BorderStroke(1.dp, t.colors.line)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Или выберите токен героя",
                            style = MaterialTheme.typography.labelMedium,
                            color = t.colors.accent,
                            fontFamily = t.display,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "32 символа",
                            style = MaterialTheme.typography.labelSmall,
                            color = t.colors.muted,
                            fontFamily = t.body
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(8),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        contentPadding = PaddingValues(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(emojiList) { emoji ->
                            val isSelected = emoji == selectedEmoji
                            Surface(
                                onClick = { selectedEmoji = emoji },
                                shape = RoundedCornerShape(t.corner),
                                color = if (isSelected) t.colors.accent.copy(alpha = 0.25f) else t.colors.bg,
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) t.colors.accent else t.colors.line
                                ),
                                modifier = Modifier.size(38.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = emoji,
                                        fontSize = 18.sp,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errorMessage!!,
                    color = t.colors.hot,
                    fontSize = 13.sp,
                    fontFamily = t.body,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // ─── Primary Save Button ──────────────────────────────────────────────────
            Button(
                onClick = {
                    errorMessage = null
                    viewModel.saveProfile(nickname, selectedEmoji)
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(
                    containerColor = t.colors.accent,
                    contentColor = t.colors.bg
                )
            ) {
                if (isSaving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = t.colors.bg, strokeWidth = 2.dp)
                } else {
                    Text(
                        text = "Сохранить профиль",
                        fontSize = 16.sp,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
