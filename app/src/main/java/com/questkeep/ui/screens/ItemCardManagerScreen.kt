package com.questkeep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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

/**
 * GM screen for managing item card templates.
 * GM can create item cards and give them to specific player characters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemCardManagerScreen(
    campaignId: String?,
    onBack: () -> Unit,
    itemCardViewModel: ItemCardViewModel = hiltViewModel()
) {
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
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Карточки предметов", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { exportLauncher.launch("item_cards.json") }) {
                        Icon(Icons.Filled.Upload, contentDescription = "Экспорт", tint = TextPrimary)
                    }
                    IconButton(onClick = { importLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Filled.Download, contentDescription = "Импорт", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = PrimaryPurple
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Создать карточку", tint = OnPrimary)
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
                    color = TextDisabled,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                items(itemCards, key = { it.id }) { card ->
                    ItemCardRow(
                        card = card,
                        onDelete = { itemCardViewModel.deleteItemCard(card) },
                        onGive = { giveDialogCard = card },
                        onEdit = { itemCardViewModel.startEditing(card) }
                    )
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
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
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
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
                        Text(card.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.bodyMedium)
                        RarityChip(card.rarity)
                    }
                    if (card.description.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(card.description, color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall, maxLines = 3)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = onGive,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = PrimaryPurpleLight),
                    border = androidx.compose.foundation.BorderStroke(1.dp, PrimaryPurple)
                ) {
                    Icon(Icons.Filled.CardGiftcard, contentDescription = null,
                        modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Выдать игроку", style = MaterialTheme.typography.labelMedium)
                }
                IconButton(onClick = onEdit, modifier = Modifier.size(40.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать",
                        tint = SecondaryGold, modifier = Modifier.size(20.dp))
                }
                IconButton(
                    onClick = { showDeleteConfirm = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = "Удалить",
                        tint = AccentRed, modifier = Modifier.size(20.dp))
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkCard,
            title = { Text("Удалить карточку?", color = TextPrimary) },
            text = { Text("«${card.name}» будет удалена из библиотеки.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { onDelete(); showDeleteConfirm = false },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Отмена", color = TextSecondary)
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
            .background(color.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
            .border(1.dp, color.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(rarity.labelRu, color = color, style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun CreateItemCardDialog(
    isSaving: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, rarity: ItemRarity) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedRarity by remember { mutableStateOf(ItemRarity.COMMON) }
    var showRarityMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Новая карточка предмета", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, colors = gmDialogFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 4, colors = gmDialogFieldColors()
                )
                Box {
                    OutlinedButton(
                        onClick = { showRarityMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Редкость: ${selectedRarity.labelRu}", fontSize = 14.sp)
                            Text("▾", color = TextSecondary)
                        }
                    }
                    DropdownMenu(
                        expanded = showRarityMenu,
                        onDismissRequest = { showRarityMenu = false },
                        containerColor = DarkCard
                    ) {
                        ItemRarity.entries.forEach { rarity ->
                            DropdownMenuItem(
                                text = { Text(rarity.labelRu, color = TextPrimary) },
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
                else Text("Создать")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
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
    var selectedCharacter by remember { mutableStateOf(characters.firstOrNull()) }
    var quantityText by remember { mutableStateOf("1") }
    var showCharMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Выдать «${card.name}»", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                if (characters.isEmpty()) {
                    Text("Нет персонажей в кампании", color = TextDisabled)
                } else {
                    // Character picker
                    Box {
                        OutlinedButton(
                            onClick = { showCharMenu = true },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(selectedCharacter?.name ?: "Выберите персонажа", fontSize = 14.sp)
                                Text("▾", color = TextSecondary)
                            }
                        }
                        DropdownMenu(
                            expanded = showCharMenu,
                            onDismissRequest = { showCharMenu = false },
                            containerColor = DarkCard
                        ) {
                            characters.forEach { char ->
                                DropdownMenuItem(
                                    text = { Text("${char.name} (${char.className})", color = TextPrimary) },
                                    onClick = { selectedCharacter = char; showCharMenu = false }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = quantityText,
                        onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) quantityText = it },
                        label = { Text("Количество") },
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
                    // Use playerId (auth UID) as characterId — matches how inventory queries work
                    onConfirm(char.playerId, qty)
                },
                enabled = selectedCharacter != null,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Выдать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
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
    var name by remember(card.id) { mutableStateOf(card.name) }
    var description by remember(card.id) { mutableStateOf(card.description) }
    var selectedRarity by remember(card.id) { mutableStateOf(card.rarity) }
    var showRarityMenu by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Редактировать карточку", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, colors = gmDialogFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 4, colors = gmDialogFieldColors()
                )
                Box {
                    OutlinedButton(
                        onClick = { showRarityMenu = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                        border = androidx.compose.foundation.BorderStroke(1.dp, DarkSurfaceVariant)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Редкость: ${selectedRarity.labelRu}", fontSize = 14.sp)
                            Text("▾", color = TextSecondary)
                        }
                    }
                    DropdownMenu(expanded = showRarityMenu, onDismissRequest = { showRarityMenu = false }, containerColor = DarkCard) {
                        ItemRarity.entries.forEach { rarity ->
                            DropdownMenuItem(
                                text = { Text(rarity.labelRu, color = TextPrimary) },
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) {
                if (isSaving) CircularProgressIndicator(modifier = Modifier.size(16.dp), color = OnPrimary)
                else Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
private fun gmDialogFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = DarkSurfaceVariant,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = PrimaryPurpleLight,
    unfocusedLabelColor = TextSecondary
)
