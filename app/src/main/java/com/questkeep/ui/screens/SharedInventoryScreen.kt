package com.questkeep.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
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
    LaunchedEffect(campaignId) { if (campaignId != null) viewModel.load(campaignId) }

    val items by viewModel.items.collectAsState()
    val bankCrowns by viewModel.bankCrowns.collectAsState()
    val error by viewModel.error.collectAsState()
    val currentUserId = viewModel.currentUserId

    var showDepositItemDialog by remember { mutableStateOf(false) }
    var showDepositCrownsDialog by remember { mutableStateOf(false) }
    var showWithdrawCrownsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🏦 Банк кампании", color = TextPrimary, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, tint = TextPrimary)
                    }
                },
                actions = {
                    IconButton(onClick = { showDepositItemDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Сдать предмет", tint = PrimaryPurpleLight)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkSurface)
            )
        },
        containerColor = DarkBackground
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
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
                    "Предметы в банке",
                    color = TextSecondary,
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
                        Text("Банк пуст", color = TextDisabled, fontSize = 14.sp)
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

    // Error snackbar
    error?.let { msg ->
        AlertDialog(
            onDismissRequest = { viewModel.clearError() },
            containerColor = DarkCard,
            title = { Text("Ошибка", color = AccentRed) },
            text = { Text(msg, color = TextPrimary) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("OK", color = PrimaryPurpleLight)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DarkCard
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("👑 Казна", color = SecondaryGold, fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "$bankCrowns монет",
                color = SecondaryGoldLight,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onDeposit,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentGreen)
                ) {
                    Text("Сдать", color = AccentGreen, fontSize = 13.sp)
                }
                OutlinedButton(
                    onClick = onWithdraw,
                    modifier = Modifier.weight(1f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentRed)
                ) {
                    Text("Взять", color = AccentRed, fontSize = 13.sp)
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
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = DarkCard
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(item.name, color = TextPrimary, fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceVariant
                    ) {
                        Text(
                            "x${item.quantity}",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                if (item.description.isNotBlank()) {
                    Text(item.description, color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(top = 2.dp))
                }
            }
            if (canWithdraw) {
                IconButton(onClick = onWithdraw) {
                    Icon(Icons.Filled.Download, contentDescription = "Забрать в инвентарь", tint = AccentGreen)
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
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var quantityText by remember { mutableStateOf("1") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text("Сдать предмет", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Название") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurpleLight, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание (необязательно)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurpleLight, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextSecondary
                    )
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it.filter { c -> c.isDigit() } },
                    label = { Text("Количество") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                        focusedBorderColor = PrimaryPurpleLight, unfocusedBorderColor = DarkSurfaceVariant,
                        focusedLabelColor = PrimaryPurpleLight, unfocusedLabelColor = TextSecondary
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
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text("Сдать") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
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
    var amountText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        title = { Text(title, color = TextPrimary) },
        text = {
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it.filter { c -> c.isDigit() } },
                label = { Text("Количество монет") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
                    focusedBorderColor = SecondaryGold, unfocusedBorderColor = DarkSurfaceVariant,
                    focusedLabelColor = SecondaryGold, unfocusedLabelColor = TextSecondary
                )
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toIntOrNull() ?: 0
                    if (amount > 0) onConfirm(amount)
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryPurple)
            ) { Text(confirmLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена", color = TextSecondary) }
        }
    )
}
