package com.questkeep.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.questkeep.domain.entities.SharedInventoryItem
import com.questkeep.ui.theme.*
import com.questkeep.viewmodel.SharedInventoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedInventoryScreen(
    campaignId: String?,
    onBack: () -> Unit,
    viewModel: SharedInventoryViewModel = hiltViewModel()
) {
    val t = LocalXanteya.current
    LaunchedEffect(campaignId) { if (campaignId != null) viewModel.load(campaignId) }

    val items by viewModel.items.collectAsState()
    val bankCrowns by viewModel.bankCrowns.collectAsState()
    val error by viewModel.error.collectAsState()

    var showDepositItemDialog by remember { mutableStateOf(false) }
    var showDepositCrownsDialog by remember { mutableStateOf(false) }
    var showWithdrawCrownsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏦 Банк кампании", color = t.colors.text, fontFamily = t.display, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = t.colors.text)
                    }
                },
                actions = {
                    IconButton(onClick = { showDepositItemDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Сдать предмет", tint = t.colors.accent)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = t.colors.panel)
            )
        },
        containerColor = t.colors.bg
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // Crowns section
            item {
                CrownsCard(
                    bankCrowns = bankCrowns,
                    onDeposit = { showDepositCrownsDialog = true },
                    onWithdraw = { showWithdrawCrownsDialog = true }
                )
            }

            // Items header
            item {
                Text(
                    "Предметы в банке (${items.size})",
                    color = t.colors.muted,
                    fontFamily = t.display,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (items.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("Банк пуст", color = t.colors.muted, fontFamily = t.body, fontSize = 14.sp)
                    }
                }
            } else {
                items(items, key = { it.id }) { item ->
                    SharedInventoryItemCard(
                        item = item,
                        canWithdraw = true,
                        onWithdraw = { viewModel.withdrawItemToInventory(item) }
                    )
                }
            }
        }
    }

    // Error alert dialog
    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = t.colors.panel,
            shape = RoundedCornerShape(t.corner),
            title = { Text("Ошибка", color = t.colors.hot, fontFamily = t.display, fontWeight = FontWeight.Bold) },
            text = { Text(msg, color = t.colors.text, fontFamily = t.body) },
            confirmButton = {
                Button(
                    onClick = { viewModel.clearError() },
                    colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                    shape = RoundedCornerShape(t.corner)
                ) {
                    Text("Понятно", fontFamily = t.display)
                }
            }
        )
    }

    // Deposit item dialog
    if (showDepositItemDialog) {
        DepositItemDialog(
            onDismiss = { showDepositItemDialog = false },
            onConfirm = { name, description, qty ->
                viewModel.depositItem(campaignId ?: "", name, description, qty)
                showDepositItemDialog = false
            }
        )
    }

    // Deposit crowns dialog
    if (showDepositCrownsDialog) {
        CrownsInputDialog(
            title = "Сдать монеты",
            confirmLabel = "Сдать",
            onDismiss = { showDepositCrownsDialog = false },
            onConfirm = { amount ->
                viewModel.depositCrowns(campaignId ?: "", amount)
                showDepositCrownsDialog = false
            }
        )
    }

    // Withdraw crowns dialog
    if (showWithdrawCrownsDialog) {
        CrownsInputDialog(
            title = "Взять монеты",
            confirmLabel = "Взять",
            onDismiss = { showWithdrawCrownsDialog = false },
            onConfirm = { amount ->
                viewModel.withdrawCrowns(campaignId ?: "", amount)
                showWithdrawCrownsDialog = false
            }
        )
    }
}

@Composable
private fun CrownsCard(
    bankCrowns: Int,
    onDeposit: () -> Unit,
    onWithdraw: () -> Unit
) {
    val t = LocalXanteya.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        border = BorderStroke(1.dp, t.colors.accent.copy(alpha = 0.6f))
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("👑", fontSize = 20.sp)
                    Text(
                        "Общая казна кампании",
                        color = t.colors.accent,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("🪙", fontSize = 32.sp)
                Column {
                    Text(
                        "$bankCrowns",
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 32.sp
                    )
                    Text("золотых крон в хранилище", color = t.colors.muted, fontFamily = t.body, fontSize = 12.sp)
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = t.colors.accent,
                        contentColor = t.colors.bg
                    )
                ) {
                    Text("+ Пополнить", fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f).height(44.dp),
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = t.colors.accent
                    ),
                    border = BorderStroke(1.dp, t.colors.line)
                ) {
                    Text("− Забрать", fontFamily = t.display, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun SharedInventoryItemCard(
    item: SharedInventoryItem,
    canWithdraw: Boolean,
    onWithdraw: () -> Unit
) {
    val t = LocalXanteya.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(t.corner),
        colors = CardDefaults.cardColors(containerColor = t.colors.panel),
        border = BorderStroke(1.dp, t.colors.line)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        item.name,
                        color = t.colors.text,
                        fontFamily = t.display,
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Surface(
                        shape = RoundedCornerShape(t.corner / 2),
                        color = t.colors.bg,
                        border = BorderStroke(1.dp, t.colors.line)
                    ) {
                        Text(
                            "×${item.quantity}",
                            color = t.colors.accent,
                            fontFamily = t.display,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (item.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        item.description,
                        color = t.colors.muted,
                        fontFamily = t.body,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 16.sp
                    )
                }
            }
            if (canWithdraw) {
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = onWithdraw,
                    shape = RoundedCornerShape(t.corner),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = t.colors.accent.copy(alpha = 0.15f),
                        contentColor = t.colors.accent
                    ),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(Icons.Filled.Download, contentDescription = null, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Взять", fontFamily = t.display, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun DepositItemDialog(
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
        title = { Text("Сдать предмет", color = t.colors.text, fontFamily = t.display) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
                        focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
                        focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)", color = t.colors.muted, fontFamily = t.body) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
                        focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
                        focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted
                    )
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 4) quantityText = it },
                    label = { Text("Количество", color = t.colors.muted, fontFamily = t.body) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
                        focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
                        focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantityText.toIntOrNull() ?: 1
                    if (name.isNotBlank() && qty > 0) onConfirm(name, description, qty)
                },
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
private fun CrownsInputDialog(
    title: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    val t = LocalXanteya.current
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = t.colors.panel,
        shape = RoundedCornerShape(t.corner),
        title = { Text(title, color = t.colors.text, fontFamily = t.display) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { if (it.all { c -> c.isDigit() } && it.length <= 7) amountText = it },
                label = { Text("Количество монет", color = t.colors.muted, fontFamily = t.body) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = t.colors.text, unfocusedTextColor = t.colors.text,
                    focusedBorderColor = t.colors.accent, unfocusedBorderColor = t.colors.line,
                    focusedLabelColor = t.colors.accent, unfocusedLabelColor = t.colors.muted
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) onConfirm(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = t.colors.accent, contentColor = t.colors.bg),
                shape = RoundedCornerShape(t.corner)
            ) { Text(confirmLabel, fontFamily = t.display) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = t.colors.muted, fontFamily = t.body) }
        }
    )
}
