package com.mswdo.kalayaan.faced.ui.inventory

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import com.mswdo.kalayaan.faced.ui.components.EmptyState
import com.mswdo.kalayaan.faced.ui.components.SectionHeader
import com.mswdo.kalayaan.faced.ui.components.StatusPill
import com.mswdo.kalayaan.faced.ui.theme.*

@Composable
fun InventoryScreen(viewModel: InventoryViewModel, isAdmin: Boolean) {
    val items by viewModel.items.collectAsState()
    val log by viewModel.distributionLog.collectAsState()
    var editing by remember { mutableStateOf<InventoryItemEntity?>(null) }
    var showNew by remember { mutableStateOf(false) }
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Relief Inventory", "Stock balances & distribution log")
            if (isAdmin) {
                Button(onClick = { showNew = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add Item")
                }
            }
        }

        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Stock Balances") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Distribution Log") })
        }

        if (tab == 0) {
            if (items.isEmpty()) {
                EmptyState(Icons.Filled.Inventory2, "No inventory items yet.")
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(items, key = { it.id }) { item ->
                        InventoryCard(item, isAdmin, onEdit = { editing = item }, onDelete = { viewModel.delete(item) })
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        } else {
            if (log.isEmpty()) {
                EmptyState(Icons.Filled.History, "No distributions recorded yet.")
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp, vertical = 10.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(log) { a ->
                        Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${a.itemName} × ${a.qty} ${a.unit}", style = MaterialTheme.typography.labelMedium, color = Slate800)
                                Text("${a.date} · ${a.facedSerial} · ${a.receivingMember}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                Text("${a.disaster} · Provider: ${a.provider}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showNew) {
        InventoryEditDialog(initial = null, onDismiss = { showNew = false }, onSave = { viewModel.save(it); showNew = false })
    }
    if (editing != null) {
        InventoryEditDialog(initial = editing, onDismiss = { editing = null }, onSave = { viewModel.update(it); editing = null })
    }
}

@Composable
private fun InventoryCard(item: InventoryItemEntity, isAdmin: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    val isLow = item.qty <= item.lowStockThreshold
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(item.name, style = MaterialTheme.typography.titleSmall, color = Slate800)
                    Text("${item.code} · ${item.category}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Slate500) }
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Red500) }
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${item.qty} ${item.unit} available", style = MaterialTheme.typography.labelMedium, color = Slate700)
                if (isLow) StatusPill("Low Stock", Red100, Red600)
            }
        }
    }
}

@Composable
private fun InventoryEditDialog(initial: InventoryItemEntity?, onDismiss: () -> Unit, onSave: (InventoryItemEntity) -> Unit) {
    var code by remember { mutableStateOf(initial?.code ?: "") }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var category by remember { mutableStateOf(initial?.category ?: "") }
    var unit by remember { mutableStateOf(initial?.unit ?: "") }
    var qty by remember { mutableStateOf(initial?.qty?.toString() ?: "") }
    var threshold by remember { mutableStateOf(initial?.lowStockThreshold?.toString() ?: "20") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add Stock Item" else "Edit Stock Item") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = code, onValueChange = { code = it }, label = { Text("Item Code") }, singleLine = true)
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Item Description") }, singleLine = true)
                OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = unit, onValueChange = { unit = it }, label = { Text("Unit") }, singleLine = true, modifier = Modifier.weight(1f))
                    OutlinedTextField(value = qty, onValueChange = { qty = it }, label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f))
                }
                OutlinedTextField(value = threshold, onValueChange = { threshold = it }, label = { Text("Low Stock Threshold") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    (initial ?: InventoryItemEntity(code = "", name = "", category = "", unit = "", qty = 0)).copy(
                        code = code.trim(), name = name.trim(), category = category.trim(),
                        unit = unit.trim(), qty = qty.toIntOrNull() ?: 0, lowStockThreshold = threshold.toIntOrNull() ?: 20
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
