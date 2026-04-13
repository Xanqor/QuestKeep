package com.questkeep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.NpcCard
import com.questkeep.ui.theme.AccentRed
import com.questkeep.ui.theme.DarkBackground
import com.questkeep.ui.theme.DarkCard
import com.questkeep.ui.theme.DarkSurface
import com.questkeep.ui.theme.DarkSurfaceVariant
import com.questkeep.ui.theme.OnPrimary
import com.questkeep.ui.theme.PrimaryPurple
import com.questkeep.ui.theme.SecondaryGold
import com.questkeep.ui.theme.TextPrimary
import com.questkeep.ui.theme.TextSecondary
import com.questkeep.viewmodel.BestiaryUiState
import com.questkeep.viewmodel.BestiaryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BestiaryScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: BestiaryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val editingNpc by viewModel.editingNpc.collectAsState()
    val deletingNpc by viewModel.deletingNpc.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    // Local editor state
    var editorName by remember { mutableStateOf("") }
    var editorDescription by remember { mutableStateOf("") }
    var editorHp by remember { mutableStateOf("") }
    var editorArmor by remember { mutableStateOf("") }
    var editorDamage by remember { mutableStateOf("") }
    var editorTags by remember { mutableStateOf("") }

    // Sync editor fields when the editing NPC changes
    LaunchedEffect(editingNpc) {
        editorName        = editingNpc?.name        ?: ""
        editorDescription = editingNpc?.description ?: ""
        editorHp          = editingNpc?.hp?.toString()    ?: ""
        editorArmor       = editingNpc?.armor?.toString() ?: ""
        editorDamage      = editingNpc?.damage      ?: ""
        editorTags        = editingNpc?.tags?.joinToString(", ") ?: ""
    }

    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.loadNpcs(campaignId)
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = viewModel.exportNpcsJson()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
        viewModel.importNpcsFromJson(json)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Бестиарий",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("npcs.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = TextPrimary)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startCreating() },
                containerColor = PrimaryPurple,
                contentColor = OnPrimary
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить NPC")
            }
        }
    ) { paddingValues ->
        when (val state = uiState) {
            is BestiaryUiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryPurple)
                }
            }
            is BestiaryUiState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = state.message, color = AccentRed, fontSize = 16.sp)
                }
            }
            is BestiaryUiState.Success -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.npcs) { npc ->
                        NpcCardItem(
                            npc = npc,
                            onEdit = { viewModel.startEditing(npc) },
                            onDelete = { viewModel.requestDelete(npc) }
                        )
                    }
                }
            }
        }
    }

    // Delete confirmation dialog
    if (deletingNpc != null) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelDelete() },
            containerColor = DarkCard,
            title = {
                Text(
                    text = "Удалить NPC?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Вы уверены, что хотите удалить «${deletingNpc!!.name}»?",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmDelete() }) {
                    Text("Удалить", color = AccentRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelDelete() }) {
                    Text("Отмена", color = TextSecondary)
                }
            }
        )
    }

    // ModalBottomSheet: visible when editingNpc is not null
    if (editingNpc != null) {
        ModalBottomSheet(
            onDismissRequest = { viewModel.cancelEdit() },
            sheetState = sheetState,
            containerColor = DarkCard
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = if (editingNpc!!.id.isEmpty()) "Создать NPC" else "Редактировать NPC",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                // NPC name
                OutlinedTextField(
                    value = editorName,
                    onValueChange = { editorName = it },
                    label = { Text("Имя NPC", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // NPC description (multiline)
                OutlinedTextField(
                    value = editorDescription,
                    onValueChange = { editorDescription = it },
                    label = { Text("Описание", color = TextSecondary) },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Stats row: HP + Armor + Damage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // HP integer field
                    OutlinedTextField(
                        value = editorHp,
                        onValueChange = { editorHp = it.filter { c -> c.isDigit() } },
                        label = { Text("❤️ HP", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                    // Armor integer field
                    OutlinedTextField(
                        value = editorArmor,
                        onValueChange = { editorArmor = it.filter { c -> c.isDigit() } },
                        label = { Text("🛡️ Броня", color = TextSecondary) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                    // Damage string field (e.g. 2d6+3)
                    OutlinedTextField(
                        value = editorDamage,
                        onValueChange = { editorDamage = it },
                        label = { Text("⚔️ Урон", color = TextSecondary) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        colors = bestiaryTextFieldColors()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tags field (comma-separated)
                OutlinedTextField(
                    value = editorTags,
                    onValueChange = { editorTags = it },
                    label = { Text("Теги (через запятую)", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = bestiaryTextFieldColors()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Save button
                Button(
                    onClick = {
                        scope.launch {
                            val parsedTags = editorTags
                                .split(",")
                                .map { it.trim() }
                                .filter { it.isNotEmpty() }
                            viewModel.saveNpc(
                                editingNpc!!.copy(
                                    name        = editorName,
                                    description = editorDescription,
                                    hp          = editorHp.toIntOrNull() ?: 0,
                                    armor       = editorArmor.toIntOrNull() ?: 0,
                                    damage      = editorDamage,
                                    tags        = parsedTags
                                )
                            )
                            sheetState.hide()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryPurple,
                        contentColor = OnPrimary
                    )
                ) {
                    Text("Сохранить", fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

// NPC card showing name, description, mini-stats, tags, and action icons
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun NpcCardItem(
    npc: NpcCard,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(DarkCard)
            .padding(14.dp)
    ) {
        Column {
            // NPC name
            Text(
                text = npc.name,
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )

            // Description preview (2 lines max)
            if (npc.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = npc.description,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 2
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Mini-stats row: HP / Armor / Damage
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("❤️ HP: ${npc.hp}", color = AccentRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("🛡️ Броня: ${npc.armor}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                if (npc.damage.isNotBlank()) {
                    Text("⚔️ Урон: ${npc.damage}", color = SecondaryGold, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }

            // Tags chips
            if (npc.tags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    npc.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(text = tag, color = TextSecondary, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Edit / Delete action row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onEdit) {
                    Icon(
                        imageVector = Icons.Filled.Edit,
                        contentDescription = "Редактировать",
                        tint = SecondaryGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Удалить",
                        tint = AccentRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

// Shared OutlinedTextField color scheme for Bestiary editor
@Composable
private fun bestiaryTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = DarkSurfaceVariant,
    cursorColor = PrimaryPurple,
    focusedLabelColor = PrimaryPurple,
    unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface
)
