package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
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
 * Fully themed according to the active realm (Falloran, Nirventhal, Tsingate, Xanteya).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: InventoryViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    val items by viewModel.items.collectAsState()
    val character by viewModel.character.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var showCrownsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(campaignId) {
        if (campaignId != null) viewModel.autoLoad(campaignId)
    }

    Scaffold(
        containerColor = t.colors.bg,
        topBar = {
            TopAppBar(
                title = { Text("Инвентарь", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.text)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = t.colors.accent,
                contentColor = t.colors.bg,
                shape = RoundedCornerShape(t.corner)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Добавить предмет")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(top = 12.dp, bottom = 100.dp)
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
                    color = t.colors.muted,
                    fontFamily = t.display,
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
                        onIncrement = { viewModel.updateQuantity(item, +1) },
                        onDecrement = { viewModel.updateQuantity(item, -1) },
                        onDelete = { viewModel.deleteItem(item) },
                        onSendToBank = { qty -> viewModel.sendToBankPartial(item, qty) }
                    )
                }
            }
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
    val t = LocalXanteya.current
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = BorderStroke(1.dp, t.colors.line),
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
                Text("💰", fontSize = 28.sp)
                Column {
                    Text("Кошелёк", color = t.colors.muted, fontFamily = t.body, style = MaterialTheme.typography.labelSmall)
                    Text(
                        "$crowns Крон",
                        color = t.colors.accent,
                        fontFamily = t.display,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            Text("›", color = t.colors.muted, fontSize = 20.sp)
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
    val t = LocalXanteya.current
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showSendToBankPicker by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        color = t.colors.panel,
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.name,
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.SemiBold,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (item.description.isNotBlank()) {
                    Text(
                        item.description,
                        color = t.colors.muted,
                        fontFamily = t.body,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onDecrement, modifier = Modifier.size(32.dp)) {
                    Text("−", color = t.colors.muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    "${item.quantity}",
                    color = t.colors.text,
                    fontFamily = t.display,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.widthIn(min = 24.dp),
                    textAlign = TextAlign.Center
                )
                IconButton(onClick = onIncrement, modifier = Modifier.size(32.dp)) {
                    Text("+", color = t.colors.accent, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
            if (onSendToBank != null) {
                IconButton(onClick = { showSendToBankPicker = true }, modifier = Modifier.size(36.dp)) {
                    Icon(
                        Icons.Filled.Upload,
                        contentDescription = "Сдать в банк",
                        tint = t.colors.accent,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            IconButton(onClick = { showDeleteConfirm = true }, modifier = Modifier.size(36.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Удалить",
                    tint = t.colors.hot,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text("Удалить предмет?", color = t.colors.text, fontFamily = t.display) },
            text = { Text("«${item.name}» будет удалён из инвентаря.", color = t.colors.muted, fontFamily = t.body) },
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
    val t = LocalXanteya.current
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("Добавить предмет", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Название", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = inventoryFieldColors()
                )
                OutlinedTextField(
                    value = description, onValueChange = { description = it },
                    label = { Text("Описание", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3,
                    colors = inventoryFieldColors()
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) quantityText = it },
                    label = { Text("Количество", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
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
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                shape = RoundedCornerShape(t.corner)
            ) { Text("Добавить", fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@Composable
internal fun EditCrownsDialog(
    currentCrowns: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val t = LocalXanteya.current
    var crownsText by remember { mutableStateOf(currentCrowns.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("💰 Кошелёк", color = t.colors.text, fontFamily = t.display) },
        text = {
            OutlinedTextField(
                value = crownsText,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 7) crownsText = it },
                label = { Text("Количество крон", color = t.colors.muted, fontFamily = t.body) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = inventoryFieldColors()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(crownsText.toIntOrNull()?.coerceAtLeast(0) ?: currentCrowns) },
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                shape = RoundedCornerShape(t.corner)
            ) { Text("Сохранить", fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
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
    val t = LocalXanteya.current
    var selected by remember { mutableStateOf(maxQuantity) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text("Сдать в банк", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "«$itemName» — выберите количество для передачи:",
                    color = t.colors.muted,
                    fontFamily = t.body,
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
                        Text("−", color = t.colors.muted, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        "$selected / $maxQuantity",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.widthIn(min = 72.dp),
                        textAlign = TextAlign.Center
                    )
                    IconButton(
                        onClick = { if (selected < maxQuantity) selected++ },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Text("+", color = t.colors.accent, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    }
                }
                if (maxQuantity > 1) {
                    TextButton(
                        onClick = { selected = maxQuantity },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            "Всё ($maxQuantity)",
                            color = t.colors.accent,
                            fontFamily = t.display,
                            style = MaterialTheme.typography.labelMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(selected) },
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                shape = RoundedCornerShape(t.corner)
            ) { Text("Сдать", fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}

@Composable
internal fun inventoryFieldColors(): TextFieldColors {
    val t = LocalXanteya.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = t.colors.accent,
        unfocusedBorderColor = t.colors.line,
        focusedTextColor = t.colors.text,
        unfocusedTextColor = t.colors.text,
        focusedLabelColor = t.colors.accent,
        unfocusedLabelColor = t.colors.muted,
        cursorColor = t.colors.accent
    )
}
