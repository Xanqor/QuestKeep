package com.questkeep.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.R
import com.questkeep.domain.entities.Character
import com.questkeep.domain.entities.ItemCard
import com.questkeep.ui.components.*
import com.questkeep.ui.theme.*
import com.questkeep.domain.entities.PlayerNpc
import com.questkeep.viewmodel.CharacterSheetUiState
import com.questkeep.viewmodel.PlayerNpcUiState
import com.questkeep.viewmodel.PlayerNpcViewModel
import com.questkeep.viewmodel.CharacterSheetViewModel
import com.questkeep.viewmodel.InventoryViewModel
import com.questkeep.viewmodel.ItemCardViewModel
import com.questkeep.viewmodel.SpellViewModel

@Composable
fun CharacterSheetScreen(
    campaignId: String?,
    isGm: Boolean = false,
    onNavigateToBestiary: () -> Unit = {},
    viewModel: CharacterSheetViewModel = hiltViewModel(),
    spellViewModel: SpellViewModel = hiltViewModel(),
    playerNpcViewModel: PlayerNpcViewModel = hiltViewModel()
) {
    LaunchedEffect(campaignId, isGm) { viewModel.loadCharacter(campaignId, isGm) }
    LaunchedEffect(campaignId) { if (campaignId != null) playerNpcViewModel.loadNpcs(campaignId) }
    val uiState by viewModel.uiState.collectAsState()
    val showDamageDialog    by viewModel.showDamageDialog.collectAsState()
    val showArmorDialog     by viewModel.showArmorDialog.collectAsState()
    val showGmEditSheet     by viewModel.showGmEditSheet.collectAsState()
    val showPlayerEditSheet by viewModel.showPlayerEditSheet.collectAsState()
    val playerNpcUiState by playerNpcViewModel.uiState.collectAsState()
    val editingNpc by playerNpcViewModel.editingNpc.collectAsState()
    val deletingNpc by playerNpcViewModel.deletingNpc.collectAsState()
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        if (campaignId == null) {
            CenteredMessage(stringResource(R.string.no_campaign_selected))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = DarkSurface,
                    contentColor = PrimaryPurple
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("👤 Персонаж", color = if (selectedTab == 0) PrimaryPurpleLight else TextSecondary) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🐉 НПС", color = if (selectedTab == 1) PrimaryPurpleLight else TextSecondary) }
                    )
                }
                when (selectedTab) {
                    1 -> NpcTabContent(
                        campaignId = campaignId,
                        playerNpcUiState = playerNpcUiState,
                        onCreateNpc = { playerNpcViewModel.startCreating() },
                        onEditNpc = { playerNpcViewModel.startEditing(it) },
                        onDeleteNpc = { playerNpcViewModel.requestDelete(it) }
                    )
                    else -> Box(modifier = Modifier.fillMaxSize()) {
                        when (val state = uiState) {
                            is CharacterSheetUiState.NoCampaign -> {}
                            is CharacterSheetUiState.Loading -> CenteredLoader()
                            is CharacterSheetUiState.NoCharacter -> {
                                if (isGm) GmRoleHint() else NoCharacterView(campaignId, viewModel)
                            }
                            is CharacterSheetUiState.GmPicker -> GmCharacterPicker(
                                characters = state.characters,
                                onSelectCharacter = { viewModel.gmSelectCharacter(it) }
                            )
                            is CharacterSheetUiState.Success -> CharacterSheetWithTabs(
                                character = state.character,
                                campaignId = campaignId,
                                isGm = isGm,
                                onLongTapHp    = { viewModel.openDamageDialog() },
                                onLongTapArmor = { viewModel.openArmorDialog()  },
                                onResourceChange    = { name, delta -> viewModel.updateResource(name, delta) },
                                onInspirationChange = { delta -> viewModel.updateInspirationPoints(delta) },
                                onGmEdit    = { viewModel.openGmEditSheet() },
                                onPlayerEdit = { viewModel.openPlayerEditSheet() },
                                onGmBack = { if (campaignId != null) viewModel.gmBackToPicker(campaignId) },
                                spellViewModel = spellViewModel
                            )
                            is CharacterSheetUiState.Error -> CenteredMessage(state.message)
                        }
                    }
                }
            }
        }
    }

    if (showDamageDialog) {
        QuickChangeDialog(
            title = stringResource(R.string.hp_change),
            positiveLabel = stringResource(R.string.heal),
            positiveColor = AccentGreen,
            negativeLabel = stringResource(R.string.damage),
            negativeColor = AccentRed,
            onPositive = { viewModel.applyHealing(it); viewModel.closeDamageDialog() },
            onNegative = { viewModel.applyDamage(it);  viewModel.closeDamageDialog() },
            onDismiss  = { viewModel.closeDamageDialog() }
        )
    }
    if (showArmorDialog) {
        QuickChangeDialog(
            title = stringResource(R.string.armor_change),
            positiveLabel = stringResource(R.string.armor_repair),
            positiveColor = Color(0xFF5B8DD9),
            negativeLabel = stringResource(R.string.armor_damage_verb),
            negativeColor = AccentRed,
            onPositive = { viewModel.applyArmorRepair(it); viewModel.closeArmorDialog() },
            onNegative = { viewModel.applyArmorDamage(it); viewModel.closeArmorDialog() },
            onDismiss  = { viewModel.closeArmorDialog() }
        )
    }
    if (showGmEditSheet) {
        val character = viewModel.currentCharacterSnapshot
        if (character != null) {
            GmEditSheet(
                character = character,
                onSave    = { updated -> viewModel.gmFullUpdateCharacter(updated); viewModel.closeGmEditSheet() },
                onDismiss = { viewModel.closeGmEditSheet() }
            )
        }
    }
    if (showPlayerEditSheet) {
        val character = viewModel.currentCharacterSnapshot
        if (character != null) {
            PlayerEditSheet(
                character = character,
                onSave    = { updated -> viewModel.playerUpdateCharacter(updated); viewModel.closePlayerEditSheet() },
                onDismiss = { viewModel.closePlayerEditSheet() }
            )
        }
    }
    editingNpc?.let { npc ->
        NpcSaveSheet(
            npc = npc,
            onDismiss = { playerNpcViewModel.cancelEdit() },
            onSave = { playerNpcViewModel.saveNpc(it) }
        )
    }
    deletingNpc?.let { npc ->
        AlertDialog(
            onDismissRequest = { playerNpcViewModel.cancelDelete() },
            containerColor = DarkCard,
            title = { Text("Удалить НПС?", color = TextPrimary) },
            text = { Text("«${npc.name}» будет удалён из списка НПС.", color = TextSecondary) },
            confirmButton = {
                Button(
                    onClick = { playerNpcViewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Удалить") }
            },
            dismissButton = {
                TextButton(onClick = { playerNpcViewModel.cancelDelete() }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        )
    }
}
// --- GM: Character Picker ---

@Composable
private fun GmCharacterPicker(
    characters: List<Character>,
    onSelectCharacter: (Character) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.gm_select_character),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.gm_edit_character),
            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(16.dp))
        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.gm_no_characters),
                    color = TextSecondary, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(characters) { character ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .background(DarkCard, RoundedCornerShape(12.dp))
                            .clickable { onSelectCharacter(character) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(modifier = Modifier.size(48.dp)
                            .background(Color(0xFF1A0A2E), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center) {
                            Text(character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                color = PrimaryPurpleLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(Modifier.weight(1f)) {
                            Text(character.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                                style = MaterialTheme.typography.titleMedium)
                            Text("${character.className} · Ур. ${character.level}",
                                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                            Text("HP: ${character.currentHp}/${character.maxHp}  🛡 ${character.armor}/${character.maxArmor}",
                                color = TextDisabled, style = MaterialTheme.typography.labelSmall)
                        }
                        Icon(Icons.Filled.Edit, contentDescription = null,
                            tint = PrimaryPurpleLight, modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}

// --- GM: Role Hint ---

@Composable
private fun GmRoleHint() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null,
                tint = SecondaryGold, modifier = Modifier.size(48.dp))
            Text(stringResource(R.string.gm_role), color = SecondaryGold,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gm_role_hint), color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
        }
    }
}
// --- Character Sheet with tabs (Персонаж / Магия) ---

@Composable
private fun CharacterSheetWithTabs(
    character: Character,
    campaignId: String?,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    spellViewModel: SpellViewModel,
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    itemCardViewModel: ItemCardViewModel = hiltViewModel()
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    Column(modifier = Modifier.fillMaxSize().background(DarkBackground)) {
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = PrimaryPurpleLight
        ) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                text = { Text("⚔ Персонаж", fontSize = 13.sp) })
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                text = { Text("✨ Магия", fontSize = 13.sp) })
            Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                text = { Text("\uD83D\uDCE6 Инвентарь", fontSize = 13.sp) })
        }
        when (selectedTab) {
            0 -> CharacterSheet(
                character = character, isGm = isGm,
                onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
                onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
                onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack
            )
            1 -> MagicTab(
                character = character,
                spellViewModel = spellViewModel
            )
            2 -> InventoryTabContent(
                character = character,
                campaignId = campaignId,
                isGm = isGm,
                inventoryViewModel = inventoryViewModel,
                itemCardViewModel = itemCardViewModel
            )
        }
    }
}

// --- Inventory Tab ---

@Composable
private fun InventoryTabContent(
    character: Character,
    campaignId: String?,
    isGm: Boolean,
    inventoryViewModel: InventoryViewModel,
    itemCardViewModel: ItemCardViewModel
) {
    val items by inventoryViewModel.items.collectAsState()
    val char by inventoryViewModel.character.collectAsState()
    val itemCards by itemCardViewModel.itemCards.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCrownsDialog by remember { mutableStateOf(false) }
    var giveDialogCard by remember { mutableStateOf<ItemCard?>(null) }
    var showLibrarySheet by remember { mutableStateOf(false) }

    // Load inventory for the specific character's playerId (works for both player and GM view)
    LaunchedEffect(character.playerId, campaignId) {
        if (campaignId != null) {
            inventoryViewModel.loadInventory(character.playerId, campaignId)
            if (isGm) itemCardViewModel.loadItemCards(campaignId)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            // Wallet — clickable to edit only for player
            item {
                WalletCard(
                    crowns = (char ?: character).crowns,
                    onEditClick = { showCrownsDialog = true }
                )
            }

            // Inventory items header
            item {
                Text(
                    "Предметы (${items.size})",
                    color = TextSecondary,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (isGm) "Инвентарь пуст" else "Инвентарь пуст.\nНажмите + чтобы добавить предмет",
                            color = TextDisabled,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    InventoryItemRow(
                        item = item,
                        onIncrement = { inventoryViewModel.updateQuantity(item, +1) },
                        onDecrement = { inventoryViewModel.updateQuantity(item, -1) },
                        onDelete = { inventoryViewModel.deleteItem(item) },
                        onSendToBank = if (!isGm) {
                            { qty -> inventoryViewModel.sendToBankPartial(item, qty) }
                        } else null
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }

        if (!isGm) {
            // Player FAB — add new item
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryPurple,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить предмет", tint = OnPrimary)
            }
        } else {
            // GM FAB — give item from library
            FloatingActionButton(
                onClick = { showLibrarySheet = true },
                containerColor = SecondaryGold,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = "Библиотека предметов", tint = OnSecondary)
            }
        }
    }

    if (showAddDialog) {
        AddInventoryItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, qty ->
                if (campaignId != null) inventoryViewModel.addItem(campaignId, name, description, qty)
                showAddDialog = false
            }
        )
    }

    if (showCrownsDialog) {
        EditCrownsDialog(
            currentCrowns = (char ?: character).crowns,
            onDismiss = { showCrownsDialog = false },
            onConfirm = { newCrowns ->
                inventoryViewModel.updateCrowns(newCrowns)
                showCrownsDialog = false
            }
        )
    }

    giveDialogCard?.let { card ->
        GiveFromInventoryDialog(
            cardName = card.name,
            onDismiss = { giveDialogCard = null },
            onConfirm = { qty ->
                itemCardViewModel.giveItemToCharacter(card, character.playerId, qty)
                giveDialogCard = null
            }
        )
    }

    if (showLibrarySheet) {
        ItemLibrarySheet(
            itemCards = itemCards,
            onDismiss = { showLibrarySheet = false },
            onGiveItem = { card -> giveDialogCard = card }
        )
    }
}

@Composable
private fun GiveFromInventoryDialog(
    cardName: String,
    onDismiss: () -> Unit,
    onConfirm: (quantity: Int) -> Unit
) {
    var quantityText by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Выдать «$cardName»", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) quantityText = it },
                label = { Text("Количество") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = inventoryFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Выдать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

// --- GM: Item Library Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemLibrarySheet(
    itemCards: List<ItemCard>,
    onDismiss: () -> Unit,
    onGiveItem: (ItemCard) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    "\uD83D\uDDE1 Библиотека предметов",
                    color = SecondaryGoldLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }
        }
    ) {
        if (itemCards.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "Библиотека пуста.\nСоздайте карточки предметов в разделе ГМ.",
                    color = TextDisabled,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 32.dp)
            ) {
                items(itemCards, key = { it.id }) { card ->
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        color = DarkSurface
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    card.name, color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (card.description.isNotBlank()) {
                                    Text(
                                        card.description, color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                }
                            }
                            Button(
                                onClick = { onGiveItem(card); onDismiss() },
                                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Выдать", color = OnSecondary,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- Character Sheet ---

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CharacterSheet(
    character: Character,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            if (isGm) {
                IconButton(onClick = onGmBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextSecondary)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(character.name, style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary, fontWeight = FontWeight.Bold)
                    val roleLabel = if (isGm) stringResource(R.string.gm_role) else stringResource(R.string.player_role)
                    val roleBg    = if (isGm) Color(0xFF3A2A00) else Color(0xFF1A0A2E)
                    val roleColor = if (isGm) SecondaryGold     else PrimaryPurpleLight
                    Row(modifier = Modifier.background(roleBg, RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(if (isGm) Icons.Filled.Shield else Icons.Filled.Person,
                            contentDescription = null, tint = roleColor, modifier = Modifier.size(12.dp))
                        Text(roleLabel, fontSize = 11.sp, color = roleColor, fontWeight = FontWeight.SemiBold)
                    }
                }
                Text("${character.className} · Ур. ${character.level}",
                    style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
            if (isGm) {
                FilledTonalButton(
                    onClick = onGmEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF3A2A00), contentColor = SecondaryGold),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text(stringResource(R.string.edit_all_fields), fontSize = 12.sp)
                }
            } else {
                // Player can edit their own stats
                FilledTonalButton(
                    onClick = onPlayerEdit,
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = Color(0xFF1A0A2E), contentColor = PrimaryPurpleLight),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Изменить", fontSize = 12.sp)
                }
            }
        }
        HorizontalDivider(color = DarkSurfaceVariant)
        Box(modifier = Modifier.combinedClickable(onLongClick = onLongTapHp, onClick = {})) {
            HpBar(current = character.currentHp, max = character.maxHp,
                modifier = Modifier.fillMaxWidth())
        }
        if (character.maxArmor > 0) {
            Box(modifier = Modifier.combinedClickable(onLongClick = onLongTapArmor, onClick = {})) {
                ArmorBar(current = character.armor, max = character.maxArmor,
                    modifier = Modifier.fillMaxWidth())
            }
        }
        Text(stringResource(R.string.main_stats),
            style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        val stats = listOf(
            stringResource(R.string.stat_wellbeing)     to character.wellbeing,
            stringResource(R.string.stat_movement)      to character.movement,
            stringResource(R.string.stat_combat)        to character.combat,
            stringResource(R.string.stat_thinking)      to character.thinking,
            stringResource(R.string.stat_attention)     to character.attention,
            stringResource(R.string.stat_communication) to character.communication
        )
        LazyStatsGrid(stats)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatCard(stringResource(R.string.dmg),        character.damage,     Modifier.weight(1f))
            StatCard(stringResource(R.string.dodge),      character.dodge,      Modifier.weight(1f))
            StatCard(stringResource(R.string.initiative), character.initiative, Modifier.weight(1f))
        }
        HorizontalDivider(color = DarkSurfaceVariant)
        Text(stringResource(R.string.magic_resources),
            style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        ResourceBar("Куар",   character.quarResource,   character.maxQuarResource,   ResourceQuarColor,   Modifier.fillMaxWidth())
        ResourceBar("Омбра",  character.ombraResource,  character.maxOmbraResource,  ResourceOmbraColor,  Modifier.fillMaxWidth())
        ResourceBar("Веруна", character.verunaResource, character.maxVerunaResource, ResourceVerunaColor, Modifier.fillMaxWidth())
        ResourceBar("Сумени", character.sumeniResource, character.maxSumeniResource, ResourceSumeniColor, Modifier.fillMaxWidth())
        Row(modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween) {
            Text(stringResource(R.string.inspiration),
                style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onInspirationChange(-1) }, modifier = Modifier.size(32.dp)) {
                    Text("−", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
                InspirationDots(current = character.inspirationPoints)
                IconButton(onClick = { onInspirationChange(1) }, modifier = Modifier.size(32.dp)) {
                    Text("+", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                }
            }
        }
        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun LazyStatsGrid(stats: List<Pair<String, Int>>) {
    val rows = stats.chunked(3)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { (label, value) -> StatCard(label, value, Modifier.weight(1f)) }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

// --- GM Full-Edit Sheet ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GmEditSheet(character: Character, onSave: (Character) -> Unit, onDismiss: () -> Unit) {
    var name          by remember { mutableStateOf(character.name) }
    var className     by remember { mutableStateOf(character.className) }
    var level         by remember { mutableStateOf(character.level.toString()) }
    var currentHp     by remember { mutableStateOf(character.currentHp.toString()) }
    var maxHp         by remember { mutableStateOf(character.maxHp.toString()) }
    var armorCur      by remember { mutableStateOf(character.armor.toString()) }
    var armorMax      by remember { mutableStateOf(character.maxArmor.toString()) }
    var wellbeing     by remember { mutableStateOf(character.wellbeing.toString()) }
    var movement      by remember { mutableStateOf(character.movement.toString()) }
    var combat        by remember { mutableStateOf(character.combat.toString()) }
    var thinking      by remember { mutableStateOf(character.thinking.toString()) }
    var attention     by remember { mutableStateOf(character.attention.toString()) }
    var communication by remember { mutableStateOf(character.communication.toString()) }
    var damage        by remember { mutableStateOf(character.damage.toString()) }
    var dodge         by remember { mutableStateOf(character.dodge.toString()) }
    var initiative    by remember { mutableStateOf(character.initiative.toString()) }
    var quarCur       by remember { mutableStateOf(character.quarResource) }
    var quarMax       by remember { mutableStateOf(character.maxQuarResource) }
    var ombraCur      by remember { mutableStateOf(character.ombraResource) }
    var ombraMax      by remember { mutableStateOf(character.maxOmbraResource) }
    var verunaCur     by remember { mutableStateOf(character.verunaResource) }
    var verunaMax     by remember { mutableStateOf(character.maxVerunaResource) }
    var sumeniCur     by remember { mutableStateOf(character.sumeniResource) }
    var sumeniMax     by remember { mutableStateOf(character.maxSumeniResource) }
    var inspiration   by remember { mutableStateOf(character.inspirationPoints) }
    var xp            by remember { mutableStateOf(character.experiencePoints.toString()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(stringResource(R.string.gm_edit_title),
                    color = SecondaryGold, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            GmSectionLabel("Персонаж")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmTextField(stringResource(R.string.character_name), name, { name = it }, Modifier.weight(2f))
                GmTextField(stringResource(R.string.character_class), className, { className = it }, Modifier.weight(2f))
                GmIntField(stringResource(R.string.stat_level), level, { level = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Здоровье")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_hp_current), currentHp, { currentHp = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_hp_max), maxHp, { maxHp = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Броня")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField("Текущая", armorCur, { armorCur = it }, Modifier.weight(1f))
                GmIntField("Максимум", armorMax, { armorMax = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Основные характеристики")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_wellbeing), wellbeing, { wellbeing = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_movement), movement, { movement = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_combat), combat, { combat = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_thinking), thinking, { thinking = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_attention), attention, { attention = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_communication), communication, { communication = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Боевые параметры")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_damage_label), damage, { damage = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_dodge_label), dodge, { dodge = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_initiative_label), initiative, { initiative = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Магические ресурсы")
            GmResourceRow("Куар", quarCur, quarMax, ResourceQuarColor, onCurChange = { quarCur = it }, onMaxChange = { quarMax = it })
            GmResourceRow("Омбра", ombraCur, ombraMax, ResourceOmbraColor, onCurChange = { ombraCur = it }, onMaxChange = { ombraMax = it })
            GmResourceRow("Веруна", verunaCur, verunaMax, ResourceVerunaColor, onCurChange = { verunaCur = it }, onMaxChange = { verunaMax = it })
            GmResourceRow("Сумени", sumeniCur, sumeniMax, ResourceSumeniColor, onCurChange = { sumeniCur = it }, onMaxChange = { sumeniMax = it })
            GmSectionLabel(stringResource(R.string.experience_points))
            GmIntField(stringResource(R.string.xp_short), xp, { xp = it }, Modifier.fillMaxWidth(0.4f))
            GmSectionLabel("Вдохновение")
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                InspirationDots(current = inspiration)
                Row {
                    IconButton(onClick = { inspiration = (inspiration - 1).coerceAtLeast(0) }, modifier = Modifier.size(36.dp)) {
                        Text("−", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    IconButton(onClick = { inspiration = (inspiration + 1).coerceAtMost(6) }, modifier = Modifier.size(36.dp)) {
                        Text("+", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(character.copy(
                        name          = name.trim().ifBlank { character.name },
                        className     = className.trim().ifBlank { character.className },
                        level         = level.toIntOrNull() ?: character.level,
                        currentHp     = currentHp.toIntOrNull() ?: character.currentHp,
                        maxHp         = maxHp.toIntOrNull() ?: character.maxHp,
                        armor         = armorCur.toIntOrNull() ?: character.armor,
                        maxArmor      = armorMax.toIntOrNull() ?: character.maxArmor,
                        wellbeing     = wellbeing.toIntOrNull() ?: character.wellbeing,
                        movement      = movement.toIntOrNull() ?: character.movement,
                        combat        = combat.toIntOrNull() ?: character.combat,
                        thinking      = thinking.toIntOrNull() ?: character.thinking,
                        attention     = attention.toIntOrNull() ?: character.attention,
                        communication = communication.toIntOrNull() ?: character.communication,
                        damage        = damage.toIntOrNull() ?: character.damage,
                        dodge         = dodge.toIntOrNull() ?: character.dodge,
                        initiative    = initiative.toIntOrNull() ?: character.initiative,
                        quarResource  = quarCur,   maxQuarResource  = quarMax,
                        ombraResource = ombraCur,  maxOmbraResource = ombraMax,
                        verunaResource= verunaCur, maxVerunaResource= verunaMax,
                        sumeniResource= sumeniCur, maxSumeniResource= sumeniMax,
                        inspirationPoints = inspiration,
                        experiencePoints = xp.toIntOrNull()?.coerceAtLeast(0) ?: character.experiencePoints,
                        updatedAt = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        }
    }
}

// Resource row: current stepper + max stepper side by side
@Composable
private fun GmResourceRow(
    label: String, current: Float, max: Float, color: Color,
    onCurChange: (Float) -> Unit, onMaxChange: (Float) -> Unit
) {
    val fmtFloat: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(56.dp))
        // Current
        Text("Тек:", color = TextDisabled, fontSize = 10.sp)
        IconButton(onClick = { onCurChange((current - 0.5f).coerceAtLeast(0f)) }, modifier = Modifier.size(28.dp)) {
            Text("−", color = AccentRed, fontWeight = FontWeight.Bold)
        }
        Text(fmtFloat(current), color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onCurChange((current + 0.5f).coerceAtMost(max)) }, modifier = Modifier.size(28.dp)) {
            Text("+", color = AccentGreen, fontWeight = FontWeight.Bold)
        }
        // Max
        Text("Макс:", color = TextDisabled, fontSize = 10.sp)
        IconButton(onClick = { onMaxChange((max - 0.5f).coerceAtLeast(0f)) }, modifier = Modifier.size(28.dp)) {
            Text("−", color = AccentRed, fontWeight = FontWeight.Bold)
        }
        Text(fmtFloat(max), color = TextSecondary, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onMaxChange(max + 0.5f) }, modifier = Modifier.size(28.dp)) {
            Text("+", color = AccentGreen, fontWeight = FontWeight.Bold)
        }
    }
}

// --- GM Edit helpers ---

@Composable private fun GmSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = SecondaryGold, fontWeight = FontWeight.SemiBold)
}

@Composable private fun GmTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp) }, singleLine = true, modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkSurfaceVariant,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextDisabled))
}

@Composable private fun GmIntField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() || c == '-' }) onValueChange(it) },
        label = { Text(label, fontSize = 10.sp, maxLines = 1) },
        singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkSurfaceVariant,
            focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
            focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextDisabled))
}

// --- No Character (Player) ---

@Composable
private fun NoCharacterView(campaignId: String?, viewModel: CharacterSheetViewModel) {
    var showCreate by remember { mutableStateOf(false) }
    var name      by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showCreate && campaignId != null) {
            Column(modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.create_character), color = TextPrimary,
                    style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.character_name)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it },
                    label = { Text(stringResource(R.string.character_class)) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.createCharacter(campaignId, name, className) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                ) { Text(stringResource(R.string.create)) }
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel), color = TextSecondary)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = TextDisabled, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.no_character), color = TextSecondary,
                    textAlign = TextAlign.Center)
                if (campaignId != null) {
                    Button(onClick = { showCreate = true },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
                    ) { Text(stringResource(R.string.create_character)) }
                }
            }
        }
    }
}

// --- Shared Quick-Change Dialog (HP or Armor) ---

@Composable
private fun QuickChangeDialog(
    title: String,
    positiveLabel: String, positiveColor: Color,
    negativeLabel: String, negativeColor: Color,
    onPositive: (Int) -> Unit,
    onNegative: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = amount, onValueChange = { amount = it },
                label = { Text(stringResource(R.string.amount)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true)
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = { amount.toIntOrNull()?.let { onNegative(it) } }) {
                    Text(negativeLabel, color = negativeColor)
                }
                TextButton(onClick = { amount.toIntOrNull()?.let { onPositive(it) } }) {
                    Text(positiveLabel, color = positiveColor)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = TextSecondary)
            }
        }
    )
}

// --- Utilities ---

@Composable
private fun CenteredMessage(msg: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = TextSecondary, style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}

@Composable
private fun CenteredLoader() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = PrimaryPurple)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player Self-Edit Sheet
// Allows players to edit: armor (cur+max), 6 main stats, max magic resources
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerEditSheet(character: Character, onSave: (Character) -> Unit, onDismiss: () -> Unit) {
    // Armor
    var armorCur  by remember { mutableStateOf(character.armor.toString()) }
    var armorMax  by remember { mutableStateOf(character.maxArmor.toString()) }
    // 6 main stats
    var wellbeing     by remember { mutableStateOf(character.wellbeing.toString()) }
    var movement      by remember { mutableStateOf(character.movement.toString()) }
    var combat        by remember { mutableStateOf(character.combat.toString()) }
    var thinking      by remember { mutableStateOf(character.thinking.toString()) }
    var attention     by remember { mutableStateOf(character.attention.toString()) }
    var communication by remember { mutableStateOf(character.communication.toString()) }
    // Max magic resources
    var quarMax   by remember { mutableStateOf(character.maxQuarResource) }
    var ombraMax  by remember { mutableStateOf(character.maxOmbraResource) }
    var verunaMax by remember { mutableStateOf(character.maxVerunaResource) }
    var sumeniMax by remember { mutableStateOf(character.maxSumeniResource) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    "✏ Редактирование персонажа",
                    color = PrimaryPurpleLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // ── Armor ────────────────────────────────────────────────────────
            PlayerSectionLabel("Броня")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField("Текущая", armorCur, { armorCur = it }, Modifier.weight(1f))
                GmIntField("Максимум", armorMax, { armorMax = it }, Modifier.weight(1f))
            }

            // ── Main stats ───────────────────────────────────────────────────
            PlayerSectionLabel("Основные характеристики")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_wellbeing), wellbeing, { wellbeing = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_movement),  movement,  { movement = it },  Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_combat),    combat,    { combat = it },    Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_thinking),      thinking,      { thinking = it },      Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_attention),     attention,     { attention = it },     Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_communication), communication, { communication = it }, Modifier.weight(1f))
            }

            // ── Max magic resources ──────────────────────────────────────────
            PlayerSectionLabel("Максимум магических ресурсов")
            PlayerMaxResourceRow("Куар",   quarMax,   ResourceQuarColor)   { quarMax = it }
            PlayerMaxResourceRow("Омбра",  ombraMax,  ResourceOmbraColor)  { ombraMax = it }
            PlayerMaxResourceRow("Веруна", verunaMax, ResourceVerunaColor) { verunaMax = it }
            PlayerMaxResourceRow("Сумени", sumeniMax, ResourceSumeniColor) { sumeniMax = it }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    onSave(character.copy(
                        armor         = armorCur.toIntOrNull() ?: character.armor,
                        maxArmor      = armorMax.toIntOrNull() ?: character.maxArmor,
                        wellbeing     = wellbeing.toIntOrNull()     ?: character.wellbeing,
                        movement      = movement.toIntOrNull()      ?: character.movement,
                        combat        = combat.toIntOrNull()        ?: character.combat,
                        thinking      = thinking.toIntOrNull()      ?: character.thinking,
                        attention     = attention.toIntOrNull()     ?: character.attention,
                        communication = communication.toIntOrNull() ?: character.communication,
                        maxQuarResource   = quarMax,
                        maxOmbraResource  = ombraMax,
                        maxVerunaResource = verunaMax,
                        maxSumeniResource = sumeniMax,
                        // Clamp current resources to new max
                        quarResource   = character.quarResource.coerceAtMost(quarMax),
                        ombraResource  = character.ombraResource.coerceAtMost(ombraMax),
                        verunaResource = character.verunaResource.coerceAtMost(verunaMax),
                        sumeniResource = character.sumeniResource.coerceAtMost(sumeniMax),
                        updatedAt = System.currentTimeMillis()
                    ))
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text(stringResource(R.string.save), fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PlayerSectionLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = PrimaryPurpleLight, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PlayerMaxResourceRow(
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit
) {
    val fmt: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Text(label, color = TextSecondary, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(56.dp))
        Text("Макс:", color = TextDisabled, fontSize = 10.sp)
        IconButton(
            onClick = { onValueChange((value - 0.5f).coerceAtLeast(0f)) },
            modifier = Modifier.size(32.dp)
        ) { Text("−", color = AccentRed, fontWeight = FontWeight.Bold) }
        Text(fmt(value), color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
        IconButton(
            onClick = { onValueChange(value + 0.5f) },
            modifier = Modifier.size(32.dp)
        ) { Text("+", color = AccentGreen, fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun npcSheetFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedBorderColor = PrimaryPurple, unfocusedBorderColor = DarkSurfaceVariant,
    cursorColor = PrimaryPurple, focusedLabelColor = PrimaryPurple, unfocusedLabelColor = TextSecondary,
    focusedContainerColor = DarkSurface, unfocusedContainerColor = DarkSurface
)

// ── NPC Tab: list of all campaign NPCs with create/edit/delete ─────────────────

@Composable
private fun NpcTabContent(
    campaignId: String,
    playerNpcUiState: PlayerNpcUiState,
    onCreateNpc: () -> Unit,
    onEditNpc: (PlayerNpc) -> Unit,
    onDeleteNpc: (PlayerNpc) -> Unit
) {
    val npcs = (playerNpcUiState as? PlayerNpcUiState.Success)?.npcs ?: emptyList()
    Box(modifier = Modifier.fillMaxSize()) {
        when {
            playerNpcUiState is PlayerNpcUiState.Loading -> CenteredLoader()
            npcs.isEmpty() -> CenteredMessage("Нет НПС. Нажмите + чтобы создать.")
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp, start = 0.dp, end = 0.dp)
            ) {
                items(npcs, key = { it.id }) { npc ->
                    NpcTabCard(npc = npc, onEdit = { onEditNpc(npc) }, onDelete = { onDeleteNpc(npc) })
                }
            }
        }
        FloatingActionButton(
            onClick = onCreateNpc,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = PrimaryPurple
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Создать НПС", tint = OnPrimary)
        }
    }
}

@Composable
private fun NpcTabCard(npc: PlayerNpc, onEdit: () -> Unit, onDelete: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkCard
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(npc.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    if (npc.description.isNotBlank()) {
                        Text(npc.description, color = TextSecondary,
                            style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать",
                            tint = PrimaryPurple, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Delete, contentDescription = "Удалить",
                            tint = AccentRed, modifier = Modifier.size(18.dp))
                    }
                }
            }
            if (npc.hp > 0 || npc.armor > 0 || npc.damage.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (npc.hp > 0)        Text("❤️ ${npc.hp}",    color = AccentRed,          style = MaterialTheme.typography.labelSmall)
                    if (npc.armor > 0)     Text("🛡 ${npc.armor}",  color = Color(0xFF5B8DD9), style = MaterialTheme.typography.labelSmall)
                    if (npc.damage.isNotBlank()) Text("⚔️ ${npc.damage}", color = SecondaryGoldLight, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (npc.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    npc.tags.forEach { tag ->
                        Text(tag, color = PrimaryPurpleLight, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(Color(0xFF1A0A2E), RoundedCornerShape(8.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp))
                    }
                }
            }
        }
    }
}

// ── NPC save sheet — handles both create (blank npc) and edit (pre-filled npc) ─

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NpcSaveSheet(
    npc: PlayerNpc,
    onDismiss: () -> Unit,
    onSave: (PlayerNpc) -> Unit
) {
    val isNew = npc.name.isBlank()
    var name        by remember(npc.id) { mutableStateOf(npc.name) }
    var description by remember(npc.id) { mutableStateOf(npc.description) }
    var hp          by remember(npc.id) { mutableStateOf(if (npc.hp > 0) npc.hp.toString() else "") }
    var armor       by remember(npc.id) { mutableStateOf(if (npc.armor > 0) npc.armor.toString() else "") }
    var damage      by remember(npc.id) { mutableStateOf(npc.damage) }
    var tags        by remember(npc.id) { mutableStateOf(npc.tags.joinToString(", ")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    if (isNew) "Создать НПС" else "Редактировать НПС",
                    color = PrimaryPurpleLight,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = name, onValueChange = { name = it },
                label = { Text("Имя НПС", color = TextSecondary) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = npcSheetFieldColors()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Описание", color = TextSecondary) },
                minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                colors = npcSheetFieldColors()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it.filter { c -> c.isDigit() } },
                    label = { Text("❤️ HP", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
                OutlinedTextField(
                    value = armor,
                    onValueChange = { armor = it.filter { c -> c.isDigit() } },
                    label = { Text("🛡️ Броня", color = TextSecondary) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
                OutlinedTextField(
                    value = damage, onValueChange = { damage = it },
                    label = { Text("⚔️ Урон", color = TextSecondary) },
                    singleLine = true, modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
            }
            OutlinedTextField(
                value = tags, onValueChange = { tags = it },
                label = { Text("Теги (через запятую)", color = TextSecondary) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = npcSheetFieldColors()
            )
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    val parsedTags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    onSave(
                        npc.copy(
                            name = name.trim(),
                            description = description.trim(),
                            hp = hp.toIntOrNull() ?: 0,
                            armor = armor.toIntOrNull() ?: 0,
                            damage = damage.trim(),
                            tags = parsedTags
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text(if (isNew) "Создать НПС" else "Сохранить", fontWeight = FontWeight.SemiBold) }
        }
    }
}
