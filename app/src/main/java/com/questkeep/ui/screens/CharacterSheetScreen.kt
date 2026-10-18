package com.questkeep.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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
    onOpenDiceRoller: () -> Unit = {},
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> onOpenDiceRoller() },
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
    val t = LocalXanteya.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }

    Box(modifier = Modifier.fillMaxSize().background(t.colors.bg)) {
        if (campaignId == null) {
            CenteredMessage(stringResource(R.string.no_campaign_selected))
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = t.colors.panel,
                    contentColor = t.colors.accent
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("👤 Персонаж", fontFamily = t.display, color = if (selectedTab == 0) t.colors.accent else t.colors.muted) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("🐉 НПС", fontFamily = t.display, color = if (selectedTab == 1) t.colors.accent else t.colors.muted) }
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
                                onApplyDamage  = { viewModel.applyDamage(it) },
                                onApplyHeal    = { viewModel.applyHealing(it) },
                                onApplyArmorDamage = { viewModel.applyArmorDamage(it) },
                                onApplyArmorRepair = { viewModel.applyArmorRepair(it) },
                                onResourceChange    = { name, delta -> viewModel.updateResource(name, delta) },
                                onInspirationChange = { delta -> viewModel.updateInspirationPoints(delta) },
                                onSetInspiration    = { viewModel.setInspirationPoints(it) },
                                onGmEdit    = { viewModel.openGmEditSheet() },
                                onPlayerEdit = { viewModel.openPlayerEditSheet() },
                                onGmBack = { viewModel.gmBackToPicker(campaignId) },
                                onOpenDiceRoller = onOpenDiceRoller,
                                onQuickRoll = onQuickRoll,
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
            containerColor = t.colors.panel,
            title = { Text("Удалить НПС?", color = t.colors.text, fontFamily = t.display) },
            text = { Text("«${npc.name}» будет удалён из списка НПС.", color = t.colors.muted, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { playerNpcViewModel.confirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentRed)
                ) { Text("Удалить", fontFamily = t.display) }
            },
            dismissButton = {
                TextButton(onClick = { playerNpcViewModel.cancelDelete() }) {
                    Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
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
    val t = LocalXanteya.current
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(stringResource(R.string.gm_select_character),
            style = MaterialTheme.typography.titleLarge,
            color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.gm_edit_character),
            style = MaterialTheme.typography.bodySmall, color = t.colors.muted, fontFamily = t.body)
        Spacer(Modifier.height(16.dp))
        if (characters.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.gm_no_characters),
                    color = t.colors.muted, textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(32.dp))
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(characters) { character ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCharacter(character) },
                        shape = RoundedCornerShape(t.corner),
                        color = t.colors.panel,
                        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(modifier = Modifier.size(48.dp)
                                .background(t.colors.bg, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center) {
                                Text(character.name.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                    color = t.colors.accent, fontFamily = t.display, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Column(Modifier.weight(1f)) {
                                Text(character.name, color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleMedium)
                                Text("${character.className} · Ур. ${character.level}",
                                    color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.bodySmall)
                                Text("HP: ${character.currentHp}/${character.maxHp}  🛡 ${character.armor}/${character.maxArmor}",
                                    color = t.colors.muted.copy(alpha = 0.7f), fontFamily = t.body, style = MaterialTheme.typography.labelSmall)
                            }
                            Icon(Icons.Filled.Edit, contentDescription = null,
                                tint = t.colors.accent, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }
}

// --- GM: Role Hint ---

@Composable
private fun GmRoleHint() {
    val t = LocalXanteya.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(32.dp)) {
            Icon(Icons.Filled.Shield, contentDescription = null,
                tint = t.colors.accent, modifier = Modifier.size(48.dp))
            Text(stringResource(R.string.gm_role), color = t.colors.accent,
                fontFamily = t.display,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.gm_role_hint), color = t.colors.muted,
                fontFamily = t.body,
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
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit = {},
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> onOpenDiceRoller() },
    spellViewModel: SpellViewModel,
    inventoryViewModel: InventoryViewModel = hiltViewModel(),
    itemCardViewModel: ItemCardViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    var selectedTab by remember { mutableIntStateOf(0) }
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE

    if (isLandscape) {
        Row(modifier = Modifier.fillMaxSize().background(t.colors.bg)) {
            // Left Column: Core Character Sheet (Master: Name, Class, HP, Armor, Attributes, Inspiration)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(t.colors.bg)
            ) {
                CharacterSheet(
                    character = character, isGm = isGm,
                    onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
                    onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
                    onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
                    onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
                    onSetInspiration = onSetInspiration,
                    onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
                    onOpenDiceRoller = onOpenDiceRoller,
                    onQuickRoll = onQuickRoll
                )
            }

            // Divider between columns
            Box(modifier = Modifier.fillMaxHeight().width(1.dp).background(t.colors.line))

            // Right Column: Detail Tabs (MagicTab / InventoryTabContent)
            Column(
                modifier = Modifier
                    .weight(1.25f)
                    .fillMaxHeight()
                    .background(t.colors.bg)
            ) {
                TabRow(
                    selectedTabIndex = if (selectedTab == 0) 0 else (selectedTab - 1),
                    containerColor = t.colors.panel,
                    contentColor = t.colors.accent
                ) {
                    Tab(
                        selected = selectedTab <= 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("✨ Магия Ксантеи", fontFamily = t.display, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab <= 1) t.colors.accent else t.colors.muted) }
                    )
                    Tab(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        text = { Text("📦 Инвентарь и Казна", fontFamily = t.display, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = if (selectedTab == 2) t.colors.accent else t.colors.muted) }
                    )
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth().background(t.colors.bg)) {
                    when (if (selectedTab == 0) 1 else selectedTab) {
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
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().background(t.colors.bg)) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = t.colors.panel,
                contentColor = t.colors.accent
            ) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("⚔ Персонаж", fontFamily = t.display, fontSize = 13.sp, color = if (selectedTab == 0) t.colors.accent else t.colors.muted) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("✨ Магия", fontFamily = t.display, fontSize = 13.sp, color = if (selectedTab == 1) t.colors.accent else t.colors.muted) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("\uD83D\uDCE6 Инвентарь", fontFamily = t.display, fontSize = 13.sp, color = if (selectedTab == 2) t.colors.accent else t.colors.muted) })
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth().background(t.colors.bg)) {
                when (selectedTab) {
                    0 -> CharacterSheet(
                        character = character, isGm = isGm,
                        onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
                        onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
                        onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
                        onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
                        onSetInspiration = onSetInspiration,
                        onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
                        onOpenDiceRoller = onOpenDiceRoller,
                        onQuickRoll = onQuickRoll
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
    val t = LocalXanteya.current
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
                    color = t.colors.muted,
                    fontFamily = t.display,
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
                            color = t.colors.muted,
                            fontFamily = t.body,
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
                containerColor = t.colors.accent,
                contentColor = t.colors.bg,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить предмет")
            }
        } else {
            // GM FAB — give item from library
            FloatingActionButton(
                onClick = { showLibrarySheet = true },
                containerColor = t.colors.accent,
                contentColor = t.colors.bg,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Filled.Shield, contentDescription = "Библиотека предметов")
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
    val t = LocalXanteya.current
    var quantityText by remember { mutableStateOf("1") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        title = { Text("Выдать «$cardName»", color = t.colors.text, fontFamily = t.display) },
        text = {
            OutlinedTextField(
                value = quantityText,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 3) quantityText = it },
                label = { Text("Количество", color = t.colors.muted, fontFamily = t.body) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = t.colors.accent,
                    unfocusedBorderColor = t.colors.line,
                    focusedTextColor = t.colors.text,
                    unfocusedTextColor = t.colors.text
                )
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1) },
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) { Text("Выдать", fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
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
    val t = LocalXanteya.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    "\uD83D\uDDE1 Библиотека предметов",
                    color = t.colors.accent,
                    fontFamily = t.display,
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
                    color = t.colors.muted,
                    fontFamily = t.body,
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
                        shape = RoundedCornerShape(t.corner),
                        color = t.colors.bg,
                        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    card.name, color = t.colors.text,
                                    fontFamily = t.display,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                if (card.description.isNotBlank()) {
                                    Text(
                                        card.description, color = t.colors.muted,
                                        fontFamily = t.body,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2
                                    )
                                }
                            }
                            Button(
                                onClick = { onGiveItem(card); onDismiss() },
                                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                            ) {
                                Text("Выдать", fontFamily = t.display,
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
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit = {},
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> onOpenDiceRoller() }
) {
    val t = LocalXanteya.current

    when (t.id) {
        "falloran" -> FalloranSheetContent(
            character = character, isGm = isGm,
            onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
            onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
            onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
            onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
            onSetInspiration = onSetInspiration,
            onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
            onOpenDiceRoller = onOpenDiceRoller,
            onQuickRoll = onQuickRoll
        )
        "nirventhal" -> NirventhalSheetContent(
            character = character, isGm = isGm,
            onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
            onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
            onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
            onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
            onSetInspiration = onSetInspiration,
            onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
            onOpenDiceRoller = onOpenDiceRoller,
            onQuickRoll = onQuickRoll
        )
        "tsingate" -> TsingateSheetContent(
            character = character, isGm = isGm,
            onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
            onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
            onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
            onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
            onSetInspiration = onSetInspiration,
            onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
            onOpenDiceRoller = onOpenDiceRoller,
            onQuickRoll = onQuickRoll
        )
        else -> XanteyaSheetContent(
            character = character, isGm = isGm,
            onLongTapHp = onLongTapHp, onLongTapArmor = onLongTapArmor,
            onApplyDamage = onApplyDamage, onApplyHeal = onApplyHeal,
            onApplyArmorDamage = onApplyArmorDamage, onApplyArmorRepair = onApplyArmorRepair,
            onResourceChange = onResourceChange, onInspirationChange = onInspirationChange,
            onSetInspiration = onSetInspiration,
            onGmEdit = onGmEdit, onPlayerEdit = onPlayerEdit, onGmBack = onGmBack,
            onOpenDiceRoller = onOpenDiceRoller,
            onQuickRoll = onQuickRoll
        )
    }
}

@Composable
private fun FalloranSheetContent(
    character: Character,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit,
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> }
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        HazardStripes(modifier = Modifier.fillMaxWidth().height(14.dp))

        // Header with pass title and character name
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f, fill = false)) {
                if (isGm) {
                    IconButton(onClick = onGmBack, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.accent)
                    }
                    Spacer(Modifier.width(4.dp))
                }
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "ПРОПУСК ЦМК № 372-04 · ЦЕХ 4",
                        fontFamily = SpecialEliteFamily,
                        fontSize = 11.sp,
                        color = t.colors.accent,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = character.name.ifBlank { "РЕЙН ВАЛЬДЕР" }.uppercase(),
                        fontFamily = RussoOneFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.text,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ThemeSwitcherPill()
                IconButton(
                    onClick = if (isGm) onGmEdit else onPlayerEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(Icons.Filled.Edit, contentDescription = "Редактировать", tint = t.colors.accent, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Gauges: Здоровье & Броня (Прочность + Блок)
        FalloranSegmentedGauge(
            label = "ЗДОРОВЬЕ",
            current = character.currentHp,
            max = character.maxHp,
            barColor = Color(0xFFD9342B),
            onTap = onLongTapHp
        )
        FalloranSegmentedGauge(
            label = "БРОНЯ (ПРОЧНОСТЬ)" + if (character.armorBlock > 0) " · БЛОК ${character.armorBlock}" else "",
            current = character.armor,
            max = character.maxArmor,
            barColor = Color(0xFFC0703A),
            onTap = onLongTapArmor
        )

        // 6 Riveted Plates (3x2) with Quick Roll
        FalloranRivetedStatsGrid(
            character = character,
            onStatClick = { name, value -> onQuickRoll(name, value) }
        )

        // Roll result hero: Big red 17
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "17",
                fontFamily = RussoOneFamily,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFD9342B)
            )
            Column {
                Text(
                    text = "d20 (14) + ЛОВ 3 · Скрытность",
                    fontFamily = SpecialEliteFamily,
                    fontSize = 13.sp,
                    color = t.colors.muted
                )
            }
        }

        // 6 Hexagon Nuts Dice Row
        val dice = listOf(4, 6, 8, 10, 12, 20)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            dice.forEach { sides ->
                FalloranDieButton(
                    n = sides,
                    active = sides == 20,
                    onClick = onOpenDiceRoller
                )
            }
        }

        // Order card
        RecentActionStampCard(
            title = "ПОСЛЕДНЯЯ ЗАЯВКА",
            actionText = "Кристальный разряд · 2d6+ВОЛ",
            stampText = "ОДОБРЕНО"
        )

        // Telegram card
        TelegramCard(
            author = "ГМ",
            text = "Стража на мосту. Видишь фонарь?"
        )

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun NirventhalSheetContent(
    character: Character,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit,
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> }
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Organic Header Card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = OrganicPanel,
            color = t.colors.panel,
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                TreeSpiralIcon(modifier = Modifier.size(54.dp))
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "НИРВЕНТАЛЬ · 372 г. ПВС",
                        fontFamily = CormorantGaramondFamily,
                        fontSize = 11.sp,
                        color = t.colors.muted,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = character.name.ifBlank { "Рейн Вальдер" },
                        fontFamily = PhilosopherFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.text,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = "Увядание II/V",
                            fontFamily = CormorantGaramondFamily,
                            fontSize = 12.sp,
                            color = t.colors.muted
                        )
                        WitheringLeaves(stage = 2, max = 5)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    ThemeSwitcherPill()
                    IconButton(onClick = if (isGm) onGmEdit else onPlayerEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = t.colors.accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Dual smooth bars: Здоровье & Броня (Прочность + Блок)
        NirventhalLeafGauge(
            label = "Здоровье",
            current = character.currentHp,
            max = character.maxHp,
            barColor = Color(0xFF6A9A80),
            onTap = onLongTapHp
        )
        NirventhalLeafGauge(
            label = "Броня (прочность)" + if (character.armorBlock > 0) " · Блок: ${character.armorBlock}" else "",
            current = character.armor,
            max = character.maxArmor,
            barColor = Color(0xFFB58943),
            onTap = onLongTapArmor
        )

        // Runic stats panel with Quick Roll
        NirventhalRunicStatsPanel(
            character = character,
            onStatClick = { name, value -> onQuickRoll(name, value) }
        )

        // Glowing purple 17 roll hero
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "17",
                fontFamily = PhilosopherFamily,
                fontSize = 54.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFA87BE0)
            )
            Column {
                Text(
                    text = "d20 (14) + ЛОВ 3 · Скрытность",
                    fontFamily = CormorantGaramondFamily,
                    fontSize = 14.sp,
                    color = t.colors.muted
                )
            }
        }

        // Tree ring dice row
        TreeRingDieRow(
            selectedDie = 20,
            onDieClick = { onOpenDiceRoller() }
        )

        // Whisper roots card
        WhisperRootsCard(text = "Корни шепчут. Кора под рукой тёплая.")

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun XanteyaSheetContent(
    character: Character,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit,
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> }
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Theme switcher & Edit row at top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isGm) {
                IconButton(onClick = onGmBack, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.accent)
                }
            } else {
                Spacer(Modifier.width(1.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                ThemeSwitcherPill()
                IconButton(onClick = if (isGm) onGmEdit else onPlayerEdit, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Edit, contentDescription = null, tint = t.colors.accent, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Sky arc with 2 suns & 2 moons
        CelestialSkyArc(modifier = Modifier.fillMaxWidth().height(40.dp))

        // Title in Forum
        Text(
            text = "К С А Н Т Е Я",
            fontFamily = ForumFamily,
            fontSize = 28.sp,
            letterSpacing = 8.sp,
            fontWeight = FontWeight.Bold,
            color = t.colors.hot
        )
        Text(
            text = "СЕЗОН КАСПА · ДЕНЬ 12 ИЗ 55",
            fontFamily = JuraFamily,
            fontSize = 11.sp,
            letterSpacing = 2.sp,
            color = t.colors.muted
        )

        // Astrolabe Dial Hero with 17
        AstrolabeDialHero(
            centerValue = 17,
            modifier = Modifier.clickable { onOpenDiceRoller() }
        )

        // Celestial circular dice row
        AtmosphericDiceRow(
            selectedDie = 20,
            onDieClick = { onOpenDiceRoller() }
        )

        // Character panel with Restored Health & Armor gauges + Cyan stat bars
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            color = t.colors.panel,
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    EtherCrystalIcon(modifier = Modifier.size(38.dp, 52.dp))
                    Column(modifier = Modifier.weight(1f, fill = false)) {
                        Text(
                            text = character.name.ifBlank { "Рейн Вальдер" },
                            fontFamily = ForumFamily,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = t.colors.text,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${character.className.ifBlank { "Странник" }} · Уровень ${character.level}",
                            fontFamily = JuraFamily,
                            fontSize = 12.sp,
                            color = t.colors.muted,
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Celestial Health & Armor gauges
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Health Gauge
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.colors.bg)
                            .border(1.dp, t.colors.line, RoundedCornerShape(10.dp))
                            .clickable(onClick = onLongTapHp)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("ЗДОРОВЬЕ", fontFamily = JuraFamily, fontSize = 10.sp, color = t.colors.muted)
                                Text("${character.currentHp}/${character.maxHp}", fontFamily = ForumFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.colors.text)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(t.colors.panel)
                            ) {
                                val fraction = if (character.maxHp > 0) (character.currentHp.toFloat() / character.maxHp).coerceIn(0f, 1f) else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFFE05A47))
                                )
                            }
                        }
                    }

                    // Armor Gauge (Прочность + Блок)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(t.colors.bg)
                            .border(1.dp, t.colors.line, RoundedCornerShape(10.dp))
                            .clickable(onClick = onLongTapArmor)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("БРОНЯ" + if (character.armorBlock > 0) " (${character.armorBlock})" else "", fontFamily = JuraFamily, fontSize = 10.sp, color = t.colors.muted)
                                Text("${character.armor}/${character.maxArmor}", fontFamily = ForumFamily, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = t.colors.text)
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(t.colors.panel)
                            ) {
                                val fraction = if (character.maxArmor > 0) (character.armor.toFloat() / character.maxArmor).coerceIn(0f, 1f) else 0f
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(fraction)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(0xFF4AC6C9))
                                )
                            }
                        }
                    }
                }

                HorizontalDivider(color = t.colors.line.copy(alpha = 0.5f))
                XanteyaStatsBarPanel(
                    character = character,
                    onStatClick = { name, value -> onQuickRoll(name, value) }
                )
            }
        }

        // Chronicle card
        ChronicleCard()

        Spacer(Modifier.height(80.dp))
    }
}

@Composable
private fun TsingateSheetContent(
    character: Character,
    isGm: Boolean,
    onLongTapHp: () -> Unit,
    onLongTapArmor: () -> Unit,
    onApplyDamage: (Int) -> Unit,
    onApplyHeal: (Int) -> Unit,
    onApplyArmorDamage: (Int) -> Unit,
    onApplyArmorRepair: (Int) -> Unit,
    onResourceChange: (String, Float) -> Unit,
    onInspirationChange: (Int) -> Unit,
    onSetInspiration: (Int) -> Unit,
    onGmEdit: () -> Unit,
    onPlayerEdit: () -> Unit,
    onGmBack: () -> Unit,
    onOpenDiceRoller: () -> Unit,
    onQuickRoll: (name: String, modifier: Int) -> Unit = { _, _ -> }
) {
    val t = LocalXanteya.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Nautical Header
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = CutCornerShape,
            color = t.colors.panel,
            border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f, fill = false)) {
                    Text(
                        text = "СУДОВОЙ РЕЕСТР · ПОРТ ЦИНГЕЙТ",
                        fontFamily = PtSerifFamily,
                        fontSize = 11.sp,
                        color = t.colors.accent,
                        letterSpacing = 1.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = character.name.ifBlank { "Рейн Вальдер" },
                        fontFamily = RuslanDisplayFamily,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = t.colors.text,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    ThemeSwitcherPill()
                    IconButton(onClick = if (isGm) onGmEdit else onPlayerEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = null, tint = t.colors.accent, modifier = Modifier.size(16.dp))
                    }
                }
            }
        }

        // Gauges: Корпус (HP) & Обшивка (Броня)
        FalloranSegmentedGauge(
            label = "КОРПУС (HP)",
            current = character.currentHp,
            max = character.maxHp,
            barColor = Color(0xFFC9A14A),
            onTap = onLongTapHp
        )
        FalloranSegmentedGauge(
            label = "ОБШИВКА (БРОНЯ)" + if (character.armorBlock > 0) " · БЛОК ${character.armorBlock}" else "",
            current = character.armor,
            max = character.maxArmor,
            barColor = Color(0xFF6B9EA8),
            onTap = onLongTapArmor
        )

        // Center compass roll hero
        AtmosphericRollHero(
            rollValue = 17,
            formula = "d20 + 3 [Бой] · Штормовой шквал"
        )

        // Brass compass dice row
        TsingateCompassDieRow(
            selectedDie = 20,
            onDieClick = { onOpenDiceRoller() }
        )

        // CutCornerShape playing cards stats grid with Quick Roll
        TsingateStatsGrid(
            character = character,
            onStatClick = { name, value -> onQuickRoll(name, value) }
        )

        // Ship log
        RecentActionStampCard(
            title = "СУДОВОЙ ЖУРНАЛ",
            actionText = "Штиль в проливе Циньгейт. Курс на маяк.",
            stampText = "ЗАПИСЬ"
        )

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
    var armorBlock    by remember { mutableStateOf(character.armorBlock.toString()) }
    var constitution  by remember { mutableStateOf(character.constitution.toString()) }
    var movement      by remember { mutableStateOf(character.movement.toString()) }
    var perception    by remember { mutableStateOf(character.perception.toString()) }
    var intelligence  by remember { mutableStateOf(character.intelligence.toString()) }
    var influence     by remember { mutableStateOf(character.influence.toString()) }
    var willpower     by remember { mutableStateOf(character.willpower.toString()) }
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

    val t = LocalXanteya.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(stringResource(R.string.gm_edit_title),
                    color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.Bold,
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
                GmIntField(stringResource(R.string.stat_armor_durability), armorCur, { armorCur = it }, Modifier.weight(1f))
                GmIntField("Макс. прочность", armorMax, { armorMax = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_armor_block), armorBlock, { armorBlock = it }, Modifier.weight(1f))
            }
            GmSectionLabel("Основные характеристики")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_constitution), constitution, { constitution = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_movement),     movement,     { movement = it },     Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_perception),   perception,   { perception = it },   Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_intelligence), intelligence, { intelligence = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_influence),    influence,    { influence = it },    Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_willpower),    willpower,    { willpower = it },    Modifier.weight(1f))
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
                        armorBlock    = armorBlock.toIntOrNull() ?: character.armorBlock,
                        constitution  = constitution.toIntOrNull() ?: character.constitution,
                        movement      = movement.toIntOrNull() ?: character.movement,
                        perception    = perception.toIntOrNull() ?: character.perception,
                        intelligence  = intelligence.toIntOrNull() ?: character.intelligence,
                        influence     = influence.toIntOrNull() ?: character.influence,
                        willpower     = willpower.toIntOrNull() ?: character.willpower,
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
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) { Text(stringResource(R.string.save), fontFamily = t.display, fontWeight = FontWeight.Bold) }
        }
    }
}

// Resource row: current stepper + max stepper side by side
@Composable
private fun GmResourceRow(
    label: String, current: Float, max: Float, color: Color,
    onCurChange: (Float) -> Unit, onMaxChange: (Float) -> Unit
) {
    val t = LocalXanteya.current
    val fmtFloat: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    Row(modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Text(label, color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(56.dp))
        // Current
        Text("Тек:", color = t.colors.muted.copy(alpha = 0.7f), fontSize = 10.sp)
        IconButton(onClick = { onCurChange((current - 0.5f).coerceAtLeast(0f)) }, modifier = Modifier.size(28.dp)) {
            Text("−", color = AccentRed, fontWeight = FontWeight.Bold)
        }
        Text(fmtFloat(current), color = color, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onCurChange((current + 0.5f).coerceAtMost(max)) }, modifier = Modifier.size(28.dp)) {
            Text("+", color = AccentGreen, fontWeight = FontWeight.Bold)
        }
        // Max
        Text("Макс:", color = t.colors.muted.copy(alpha = 0.7f), fontSize = 10.sp)
        IconButton(onClick = { onMaxChange((max - 0.5f).coerceAtLeast(0f)) }, modifier = Modifier.size(28.dp)) {
            Text("−", color = AccentRed, fontWeight = FontWeight.Bold)
        }
        Text(fmtFloat(max), color = t.colors.muted, fontWeight = FontWeight.Bold,
            modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
        IconButton(onClick = { onMaxChange(max + 0.5f) }, modifier = Modifier.size(28.dp)) {
            Text("+", color = AccentGreen, fontWeight = FontWeight.Bold)
        }
    }
}

// --- GM Edit helpers ---

@Composable private fun GmSectionLabel(text: String) {
    val t = LocalXanteya.current
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.SemiBold)
}

@Composable private fun GmTextField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    OutlinedTextField(value = value, onValueChange = onValueChange,
        label = { Text(label, fontSize = 11.sp, color = t.colors.muted) }, singleLine = true, modifier = modifier,
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
            focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
            focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted))
}

@Composable private fun GmIntField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val t = LocalXanteya.current
    OutlinedTextField(
        value = value,
        onValueChange = { if (it.all { c -> c.isDigit() }) onValueChange(it) },
        label = { Text(label, fontSize = 10.sp, maxLines = 1, color = t.colors.muted) },
        singleLine = true, modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(fontSize = 14.sp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
            focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
            focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted))
}

// --- No Character (Player) ---

@Composable
private fun NoCharacterView(campaignId: String?, viewModel: CharacterSheetViewModel) {
    val t = LocalXanteya.current
    var showCreate by remember { mutableStateOf(false) }
    var name      by remember { mutableStateOf("") }
    var className by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (showCreate && campaignId != null) {
            Column(modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.create_character), color = t.colors.text,
                    fontFamily = t.display,
                    style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(value = name, onValueChange = { name = it },
                    label = { Text(stringResource(R.string.character_name), color = t.colors.muted) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = className, onValueChange = { className = it },
                    label = { Text(stringResource(R.string.character_class), color = t.colors.muted) },
                    singleLine = true, modifier = Modifier.fillMaxWidth())
                Button(onClick = { viewModel.createCharacter(campaignId, name, className) },
                    enabled = name.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
                ) { Text(stringResource(R.string.create), fontFamily = t.display) }
                TextButton(onClick = { showCreate = false }) {
                    Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
                }
            }
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Filled.Person, contentDescription = null,
                    tint = t.colors.muted, modifier = Modifier.size(48.dp))
                Text(stringResource(R.string.no_character), color = t.colors.muted,
                    fontFamily = t.body,
                    textAlign = TextAlign.Center)
                if (campaignId != null) {
                    Button(onClick = { showCreate = true },
                        colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
                    ) { Text(stringResource(R.string.create_character), fontFamily = t.display) }
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
    val t = LocalXanteya.current
    var amount by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text(title, color = t.colors.text, fontFamily = t.display) },
        text = {
            OutlinedTextField(
                value = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text(stringResource(R.string.amount), color = t.colors.muted, fontFamily = t.body) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = t.colors.accent,
                    unfocusedBorderColor = t.colors.line,
                    focusedTextColor = t.colors.text,
                    unfocusedTextColor = t.colors.text
                )
            )
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = {
                    amount.toIntOrNull()?.let { if (it > 0) onNegative(it) }
                }) {
                    Text(negativeLabel, color = negativeColor, fontFamily = t.display)
                }
                TextButton(onClick = {
                    amount.toIntOrNull()?.let { if (it > 0) onPositive(it) }
                }) {
                    Text(positiveLabel, color = positiveColor, fontFamily = t.display)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel), color = t.colors.muted, fontFamily = t.body)
            }
        }
    )
}

// --- Utilities ---

@Composable
private fun CenteredMessage(msg: String) {
    val t = LocalXanteya.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(msg, color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, modifier = Modifier.padding(32.dp))
    }
}

@Composable
private fun CenteredLoader() {
    val t = LocalXanteya.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = t.colors.accent)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Player Self-Edit Sheet
// Allows players to edit: armor (cur+max), 6 main stats, max magic resources
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerEditSheet(character: Character, onSave: (Character) -> Unit, onDismiss: () -> Unit) {
    val t = LocalXanteya.current
    // Armor
    var armorCur  by remember { mutableStateOf(character.armor.toString()) }
    var armorMax  by remember { mutableStateOf(character.maxArmor.toString()) }
    var armorBlock by remember { mutableStateOf(character.armorBlock.toString()) }
    // 6 main stats
    var constitution  by remember { mutableStateOf(character.constitution.toString()) }
    var movement      by remember { mutableStateOf(character.movement.toString()) }
    var perception    by remember { mutableStateOf(character.perception.toString()) }
    var intelligence  by remember { mutableStateOf(character.intelligence.toString()) }
    var influence     by remember { mutableStateOf(character.influence.toString()) }
    var willpower     by remember { mutableStateOf(character.willpower.toString()) }
    // Max magic resources
    var quarMax   by remember { mutableStateOf(character.maxQuarResource) }
    var ombraMax  by remember { mutableStateOf(character.maxOmbraResource) }
    var verunaMax by remember { mutableStateOf(character.maxVerunaResource) }
    var sumeniMax by remember { mutableStateOf(character.maxSumeniResource) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    "✏ Редактирование персонажа",
                    color = t.colors.accent,
                    fontFamily = t.display,
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
                GmIntField(stringResource(R.string.stat_armor_durability), armorCur, { armorCur = it }, Modifier.weight(1f))
                GmIntField("Макс. прочность", armorMax, { armorMax = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_armor_block), armorBlock, { armorBlock = it }, Modifier.weight(1f))
            }

            // ── Main stats ───────────────────────────────────────────────────
            PlayerSectionLabel("Основные характеристики")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_constitution), constitution, { constitution = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_movement),     movement,     { movement = it },     Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_perception),   perception,   { perception = it },   Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GmIntField(stringResource(R.string.stat_intelligence), intelligence, { intelligence = it }, Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_influence),    influence,    { influence = it },    Modifier.weight(1f))
                GmIntField(stringResource(R.string.stat_willpower),    willpower,    { willpower = it },    Modifier.weight(1f))
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
                        armorBlock    = armorBlock.toIntOrNull() ?: character.armorBlock,
                        constitution  = constitution.toIntOrNull()     ?: character.constitution,
                        movement      = movement.toIntOrNull()      ?: character.movement,
                        perception    = perception.toIntOrNull()    ?: character.perception,
                        intelligence  = intelligence.toIntOrNull()  ?: character.intelligence,
                        influence     = influence.toIntOrNull()     ?: character.influence,
                        willpower     = willpower.toIntOrNull()     ?: character.willpower,
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
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) { Text(stringResource(R.string.save), fontFamily = t.display, fontWeight = FontWeight.Bold) }
        }
    }
}

@Composable
private fun PlayerSectionLabel(text: String) {
    val t = LocalXanteya.current
    Text(text, style = MaterialTheme.typography.labelMedium,
        color = t.colors.accent, fontFamily = t.display, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun PlayerMaxResourceRow(
    label: String,
    value: Float,
    color: androidx.compose.ui.graphics.Color,
    onValueChange: (Float) -> Unit
) {
    val t = LocalXanteya.current
    val fmt: (Float) -> String = { v ->
        if (v == v.toInt().toFloat()) v.toInt().toString() else String.format("%.1f", v)
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(50)))
        Text(label, color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(56.dp))
        Text("Макс:", color = t.colors.muted.copy(alpha = 0.7f), fontSize = 10.sp)
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
private fun npcSheetFieldColors(): TextFieldColors {
    val t = LocalXanteya.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
        focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
        cursorColor = t.colors.accent, focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted,
        focusedContainerColor = t.colors.bg, unfocusedContainerColor = t.colors.bg
    )
}

// ── NPC Tab: list of all campaign NPCs with create/edit/delete ─────────────────

@Composable
private fun NpcTabContent(
    campaignId: String,
    playerNpcUiState: PlayerNpcUiState,
    onCreateNpc: () -> Unit,
    onEditNpc: (PlayerNpc) -> Unit,
    onDeleteNpc: (PlayerNpc) -> Unit
) {
    val t = LocalXanteya.current
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
            containerColor = t.colors.accent,
            contentColor = t.colors.bg
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Создать НПС")
        }
    }
}

@Composable
private fun NpcTabCard(npc: PlayerNpc, onEdit: () -> Unit, onDelete: () -> Unit) {
    val t = LocalXanteya.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = androidx.compose.foundation.BorderStroke(1.dp, t.colors.line)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(npc.name, color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    if (npc.description.isNotBlank()) {
                        Text(npc.description, color = t.colors.muted, fontFamily = t.body,
                            style = MaterialTheme.typography.bodySmall, maxLines = 2)
                    }
                }
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Edit, contentDescription = "Редактировать",
                            tint = t.colors.accent, modifier = Modifier.size(18.dp))
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
                    if (npc.hp > 0)        Text("❤️ ${npc.hp}",    color = AccentRed,          fontFamily = t.body, style = MaterialTheme.typography.labelSmall)
                    if (npc.armor > 0)     Text("🛡 ${npc.armor}",  color = Color(0xFF5B8DD9),  fontFamily = t.body, style = MaterialTheme.typography.labelSmall)
                    if (npc.damage.isNotBlank()) Text("⚔️ ${npc.damage}", color = t.colors.accent, fontFamily = t.body, style = MaterialTheme.typography.labelSmall)
                }
            }
            if (npc.tags.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.fillMaxWidth()) {
                    npc.tags.forEach { tag ->
                        Text(tag, color = t.colors.accent, fontFamily = t.body, style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier
                                .background(t.colors.bg, RoundedCornerShape(6.dp))
                                .border(1.dp, t.colors.line, RoundedCornerShape(6.dp))
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
    val t = LocalXanteya.current
    val isNew = npc.name.isBlank()
    var name        by remember(npc.id) { mutableStateOf(npc.name) }
    var description by remember(npc.id) { mutableStateOf(npc.description) }
    var hp          by remember(npc.id) { mutableStateOf(if (npc.hp > 0) npc.hp.toString() else "") }
    var armor       by remember(npc.id) { mutableStateOf(if (npc.armor > 0) npc.armor.toString() else "") }
    var damage      by remember(npc.id) { mutableStateOf(npc.damage) }
    var tags        by remember(npc.id) { mutableStateOf(npc.tags.joinToString(", ")) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        dragHandle = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BottomSheetDefaults.DragHandle()
                Text(
                    if (isNew) "Создать НПС" else "Редактировать НПС",
                    color = t.colors.accent,
                    fontFamily = t.display,
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
                label = { Text("Имя НПС", color = t.colors.muted, fontFamily = t.body) },
                singleLine = true, modifier = Modifier.fillMaxWidth(),
                colors = npcSheetFieldColors()
            )
            OutlinedTextField(
                value = description, onValueChange = { description = it },
                label = { Text("Описание", color = t.colors.muted, fontFamily = t.body) },
                minLines = 2, maxLines = 5, modifier = Modifier.fillMaxWidth(),
                colors = npcSheetFieldColors()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = hp,
                    onValueChange = { hp = it.filter { c -> c.isDigit() } },
                    label = { Text("❤️ HP", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
                OutlinedTextField(
                    value = armor,
                    onValueChange = { armor = it.filter { c -> c.isDigit() } },
                    label = { Text("🛡️ Броня", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
                OutlinedTextField(
                    value = damage, onValueChange = { damage = it },
                    label = { Text("⚔️ Урон", color = t.colors.muted, fontFamily = t.body) },
                    singleLine = true, modifier = Modifier.weight(1f),
                    colors = npcSheetFieldColors()
                )
            }
            OutlinedTextField(
                value = tags, onValueChange = { tags = it },
                label = { Text("Теги (через запятую)", color = t.colors.muted, fontFamily = t.body) },
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
                shape = RoundedCornerShape(t.corner),
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg)
            ) { Text(if (isNew) "Создать НПС" else "Сохранить", fontFamily = t.display, fontWeight = FontWeight.SemiBold) }
        }
    }
}
