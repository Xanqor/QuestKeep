package com.questkeep.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.InventoryItem
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.InventoryViewModel

/**
 * Inventory + Wallet screen for the current player.
 * Loads the player's own character automatically via AuthRepository.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val items by viewModel.items.collectAsState()
    val character by viewModel.character.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCrownsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.autoLoad(campaignId)
    }

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Инвентарь", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = PrimaryPurple
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить предмет", tint = OnPrimary)
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 12.dp)
        ) {
            item {
                WalletCard(
                    crowns = character?.crowns ?: 0,
                    onEditClick = { showCrownsDialog = true }
                )
            }

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
                        modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Инвентарь пуст.\nНажмите + чтобы добавить предмет",
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
                        onIncrement = { viewModel.updateQuantity(item, +1) },
                        onDecrement = { viewModel.updateQuantity(item, -1) },
                        onDelete = { viewModel.deleteItem(item) },
                        onSendToBank = { qty -> viewModel.sendToBankPartial(item, qty) }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddDialog) {
        AddInventoryItemDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { name, description, qty ->
                if (campaignId != null) viewModel.addItem(campaignId, name, description, qty)
                showAddDialog = false
            }
        )
    }

    if (showCrownsDialog) {
        EditCrownsDialog(
            currentCrowns = character?.crowns ?: 0,
            onDismiss = { showCrownsDialog = false },
            onConfirm = { newCrowns ->
                viewModel.updateCrowns(newCrowns)
                showCrownsDialog = false
            }
        )
    }
}

@Composable
internal fun WalletCard(crowns: Int, onEditClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard,
        onClick = onEditClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("\uD83D\uDCB0", fontSize = 28.sp) // 💰
                Column {
                    Text("Кошелёк", color = TextSecondary, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$crowns Крон",
                        color = SecondaryGoldLight,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("›", color = TextSecondary, fontSize = 20.sp)
        }
    }
}

@Composable
internal fun InventoryItemRow(
    item: InventoryItem,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    onDelete: () -> Unit,
    onSendToBank: ((Int) -> Unit)? = null
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSendToBankPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        color = DarkCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(item.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium)
                if (item.description.isNotBlank()) {
                    Text(item.description, color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall, maxLines = 2)
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Text("\u2212", color = TextSecondary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${item.quantity}",
                    color = TextPrimary, fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Text("+", color = PrimaryPurpleLight, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (onSendToBank != null) {
                IconButton(onClick = { showSendToBankPicker = true }, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Filled.Upload, contentDescription = "Сдать в банк",
                        tint = SecondaryGold, modifier = Modifier.size(18.dp))
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Filled.Delete, contentDescription = "Удалить",
                    tint = AccentRed, modifier = Modifier.size(18.dp))
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = DarkCard,
            title = { Text("Удалить предмет?", color = TextPrimary) },
            text = { Text("«${item.name}» будет удалён из инвентаря.", color = TextSecondary) },
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

    if (showSendToBankPicker && onSendToBank != null) {
        SendToBankDialog(
            itemName = item.name,
            maxQuantity = item.quantity,
            onDismiss = { showSendToBankPicker = false },
            onConfirm = { qty ->
                onSendToBank(qty)
                showSendToBankPicker = false
            }
        )
    }
}

@Composable
internal fun AddInventoryItemDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, description: String, quantity: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Добавить предмет", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true, colors = inventoryFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание") }, modifier = Modifier.fillMaxWidth(),
                    maxLines = 3, colors = inventoryFieldColors()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { c -> c.isDigit() }) quantityText = it },
                    label = { Text("Количество") }, modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = inventoryFieldColors()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    onConfirm(name, description, qty)
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Добавить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
internal fun EditCrownsDialog(
    currentCrowns: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var crownsText by remember { mutableStateOf(currentCrowns.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("\uD83D\uDCB0 Кошелёк", color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = crownsText,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 7) crownsText = it },
                label = { Text("Количество крон") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = inventoryFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(crownsText.toIntOrNull() ?: currentCrowns) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
internal fun SendToBankDialog(
    itemName: String,
    maxQuantity: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var selected by remember { mutableStateOf(maxQuantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Сдать в банк", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "«$itemName» — выберите количество для передачи:",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { if (selected > 1) selected-- },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("−", color = TextSecondary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$selected / $maxQuantity",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.widthIn(min = 72.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (selected < maxQuantity) selected++ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("+", color = PrimaryPurpleLight, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (maxQuantity > 1) {
                    TextButton(
                        onClick = { selected = maxQuantity },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text("Всё ($maxQuantity)", color = SecondaryGoldLight,
                            style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = SecondaryGold)
            ) { Text("Сдать", color = OnPrimary) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}

@Composable
internal fun inventoryFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PrimaryPurple,
    unfocusedBorderColor = DarkSurfaceVariant,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = PrimaryPurpleLight,
    unfocusedLabelColor = TextSecondary
)
