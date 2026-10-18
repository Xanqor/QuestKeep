package com.questkeep.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.ItemCard
import com.questkeep.domain.entities.ItemRarity
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.ItemCardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCardManagerScreen(
    campaignId: String?,
    onBack: () -> Unit,
    itemCardViewModel: ItemCardViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val itemCards by itemCardViewModel.itemCards.collectAsState()
    val saveSuccess by itemCardViewModel.saveSuccess.collectAsState()
    val characters by itemCardViewModel.allCharacters.collectAsState()
    val editingCard by itemCardViewModel.editingCard.collectAsState()
    val isSaving by itemCardViewModel.isSaving.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var giveDialogCard by remember { mutableStateOf<ItemCard?>(null) }

    LaunchedEffect(campaignId) {
        if (campaignId != null) {
            itemCardViewModel.loadItemCards(campaignId)
            itemCardViewModel.loadCharacters(campaignId)
        }
    }

    LaunchedEffect(saveSuccess) {
        if (saveSuccess) {
            showCreateDialog = false
            itemCardViewModel.resetSaveSuccess()
        }
    }

    val context = LocalContext.current
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val json = itemCardViewModel.exportItemCardsJson()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val cid = campaignId ?: return@rememberLauncherForActivityResult
        val json = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) } ?: return@rememberLauncherForActivityResult
        itemCardViewModel.importItemCardsFromJson(json, cid)
    }

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text("Карточки предметов", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.text)
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("item_cards.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = t.colors.accent)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = t.colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = t.colors.accent,
                contentColor = t.colors.bg,
                shape = RoundedCornerShape(t.corner)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Создать карточку")
            }
        }
    ) { padding ->
        if (itemCards.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Карточек предметов нет.\nСоздайте первую карточку!",
                    color = t.colors.muted,
                    fontFamily = t.body,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
            ) {
                items(itemCards, key = { it.id }) { card ->
                    ItemCardRow(
                        card = card,
                        onDelete = { itemCardViewModel.deleteItemCard(card) },
                        onGive = { giveDialogCard = card },
                        onEdit = { itemCardViewModel.startEditing(card) }
                    )
                }
                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }

    // Create item card dialog
    if (showCreateDialog && campaignId != null) {
        CreateItemCardDialog(
            isSaving = itemCardViewModel.isSaving.collectAsState().value,
            onDismiss = { showCreateDialog = false },
            onConfirm = { name, description, rarity ->
                itemCardViewModel.saveItemCard(campaignId, name, description, rarity)
            }
        )
    }

    // Give item dialog — pick character + quantity
    giveDialogCard?.let { card ->
        GiveItemDialog(
            card = card,
            characters = characters,
            onDismiss = { giveDialogCard = null },
            onConfirm = { characterId, quantity ->
                itemCardViewModel.giveItemToCharacter(card, characterId, quantity)
                giveDialogCard = null
            }
        )
    }

    // Edit item card dialog
    editingCard?.let { card ->
        EditItemCardDialog(
            card = card,
            isSaving = isSaving,
            onDismiss = { itemCardViewModel.cancelEdit() },
            onConfirm = { name, description, rarity ->
                itemCardViewModel.updateItemCard(card.copy(name = name, description = description, rarity = rarity))
            }
        )
    }
}

@Composable
private fun ItemCardRow(
    card: ItemCard,
    onDelete: () -> Unit,
    onGive: () -> Unit,
    onEdit: () -> Unit = {}
) {
    val t = LocalXanteya.current
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            card.name,
                            color = t.colors.text,
                            fontFamily = t.display,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium
                        )
                        RarityChip(card.rarity)
                    }
                    if (card.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            card.description,
                            color = t.colors.muted,
                            fontFamily = t.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onGive,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.accent),
                    border = BorderStroke(1.dp, t.colors.accent)
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выдать игроку", fontFamily = t.display, style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = t.colors.accent, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить", tint = t.colors.hot, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text("Удалить карточку?", color = t.colors.text, fontFamily = t.display) },
            text = { Text("«${card.name}» будет удалена из библиотеки.", color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.hot, contentColor = t.colors.bg),
                    shape = RoundedCornerShape(t.corner)
                ) { Text("Удалить", fontFamily = t.display) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = t.colors.muted, fontFamily = t.body)
                }
            }
        )
    }
}

@Composable
private fun RarityChip(rarity: ItemRarity) {
    val color = when (rarity) {
        ItemRarity.COMMON    -> Color(0xFF9E9E9E)
        ItemRarity.UNCOMMON  -> Color(0xFF4CAF50)
        ItemRarity.RARE      -> Color(0xFF2196F3)
        ItemRarity.EPIC      -> Color(0xFF9C27B0)
        ItemRarity.LEGENDARY -> Color(0xFFFF9800)
    }
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(rarity.labelRu, color = color, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreateItemCardDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, rarity: ItemRarity) -> Unit
) {
    val t = LocalXanteya.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf(ItemRarity.COMMON) }
    var showRarityMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("Новая карточка предмета", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = gmDialogFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = gmDialogFieldColors()
                )
                Box {
                    OutlinedButton(
                        onClick = { showRarityMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(t.corner),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.text),
                        border = BorderStroke(1.dp, t.colors.line)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Редкость: ${selectedRarity.labelRu}", fontSize = 14.sp, fontFamily = t.body)
                            Text("▾", color = t.colors.muted)
                        }
                    }
                    DropdownMenu(
                        expanded = showRarityMenu,
                        onDismissRequest = { showRarityMenu = false },
                        containerColor = t.colors.panel
                    ) {
                        ItemRarity.entries.forEach { rarity ->
                            DropdownMenuItem(
                                text = { Text(rarity.labelRu, color = t.colors.text, fontFamily = t.body) },
                                onClick = { selectedRarity = rarity; showRarityMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedRarity) },
                enabled = name.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = t.colors.bg)
                else Text("Создать", fontFamily = t.display)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@Composable
private fun GiveItemDialog(
    card: ItemCard,
    characters: List<Character>,
    onDismiss: () -> Unit,
    onConfirm: (characterId: String, quantity: Int) -> Unit
) {
    val t = LocalXanteya.current
    var selectedCharacter by remember { mutableStateOf(characters.firstOrNull()) }
    var quantityText by remember { mutableStateOf("1") }
    var showCharMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("Выдать «${card.name}»", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (characters.isEmpty()) {
                    Text("Нет персонажей в кампании", color = t.colors.muted, fontFamily = t.body)
                } else {
                    Box {
                        OutlinedButton(
                            onClick = { showCharMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(t.corner),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.text),
                            border = BorderStroke(1.dp, t.colors.line)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedCharacter?.name ?: "Выберите персонажа", fontSize = 14.sp, fontFamily = t.body)
                                Text("▾", color = t.colors.muted)
                            }
                        }
                        DropdownMenu(
                            expanded = showCharMenu,
                            onDismissRequest = { showCharMenu = false },
                            containerColor = t.colors.panel
                        ) {
                            characters.forEach { char ->
                                DropdownMenuItem(
                                    text = { Text("${char.name} (${char.className})", color = t.colors.text, fontFamily = t.body) },
                                    onClick = { selectedCharacter = char; showCharMenu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) quantityText = it },
                        label = { Text("Количество", color = t.colors.muted, fontFamily = t.body) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = gmDialogFieldColors()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val char = selectedCharacter ?: return@Button
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onConfirm(char.playerId, qty)
                },
                enabled = selectedCharacter != null,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) { Text("Выдать", fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@Composable
private fun EditItemCardDialog(
    card: ItemCard,
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, rarity: ItemRarity) -> Unit
) {
    val t = LocalXanteya.current
    var name by remember(card.id) { mutableStateOf(card.name) }
    var description by remember(card.id) { mutableStateOf(card.description) }
    var selectedRarity by remember(card.id) { mutableStateOf(card.rarity) }
    var showRarityMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("Редактировать карточку", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = gmDialogFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 4,
                    colors = gmDialogFieldColors()
                )
                Box {
                    OutlinedButton(
                        onClick = { showRarityMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(t.corner),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = t.colors.text),
                        border = BorderStroke(1.dp, t.colors.line)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Редкость: ${selectedRarity.labelRu}", fontSize = 14.sp, fontFamily = t.body)
                            Text("▾", color = t.colors.muted)
                        }
                    }
                    DropdownMenu(expanded = showRarityMenu, onDismissRequest = { showRarityMenu = false }, containerColor = t.colors.panel) {
                        ItemRarity.entries.forEach { rarity ->
                            DropdownMenuItem(
                                text = { Text(rarity.labelRu, color = t.colors.text, fontFamily = t.body) },
                                onClick = { selectedRarity = rarity; showRarityMenu = false }
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, description, selectedRarity) },
                enabled = name.isNotBlank() && !isSaving,
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = t.colors.bg)
                else Text("Сохранить", fontFamily = t.display)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@Composable
private fun gmDialogFieldColors(): TextFieldColors {
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
