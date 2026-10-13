package com.mswdo.kalayaan.faced.ui.evac

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
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.repository.CenterWithOccupancy
import com.mswdo.kalayaan.faced.ui.components.EmptyState
import com.mswdo.kalayaan.faced.ui.components.ProgressBarRow
import com.mswdo.kalayaan.faced.ui.components.SectionHeader
import com.mswdo.kalayaan.faced.ui.theme.*

@Composable
fun EvacCentersScreen(viewModel: EvacViewModel, isAdmin: Boolean) {
    val centers by viewModel.centers.collectAsState()
    val query by viewModel.query.collectAsState()
    var showDialog by remember { mutableStateOf<EvacCenterEntity?>(null) }
    var showNewDialog by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionHeader("Evacuation Centers", "Real-time occupancy & capacity")
            if (isAdmin) {
                Button(onClick = { showNewDialog = true }) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Add")
                }
            }
        }
        
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text("Search by name or location...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val selectedBarangay by viewModel.barangayFilter.collectAsState()
            val barangays = listOf("All", "San Juan", "Longos", "San Antonio")

            barangays.forEach { b ->
                val isSelected = (b == "All" && selectedBarangay == null) || selectedBarangay == b
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.onBarangayFilterChange(if (b == "All") null else b) },
                    label = { Text(b, style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        if (centers.isEmpty()) {
            EmptyState(Icons.Filled.Home, "No evacuation centers registered.")
        } else {
            LazyColumn(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(centers, key = { it.center.id }) { cw ->
                    CenterCard(cw, isAdmin, onEdit = { showDialog = cw.center }, onDelete = { viewModel.delete(cw.center) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }

    if (showNewDialog) {
        CenterEditDialog(
            initial = null,
            onDismiss = { showNewDialog = false },
            onSave = { viewModel.save(it); showNewDialog = false }
        )
    }
    if (showDialog != null) {
        CenterEditDialog(
            initial = showDialog,
            onDismiss = { showDialog = null },
            onSave = { viewModel.update(it); showDialog = null }
        )
    }
}

@Composable
private fun CenterCard(cw: CenterWithOccupancy, isAdmin: Boolean, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(cw.center.name, style = MaterialTheme.typography.titleSmall, color = Slate800)
                    Text(cw.center.location, style = MaterialTheme.typography.bodySmall, color = Slate400)
                }
                if (isAdmin) {
                    Row {
                        IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit", tint = Slate500) }
                        IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = Red500) }
                    }
                }
            }
            ProgressBarRow(cw.center.name, cw.percentFull, cw.occupied, cw.center.capacity)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CenterEditDialog(initial: EvacCenterEntity?, onDismiss: () -> Unit, onSave: (EvacCenterEntity) -> Unit) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var location by remember { mutableStateOf(initial?.location ?: "San Juan") }
    var capacity by remember { mutableStateOf(initial?.capacity?.toString() ?: "") }
    var facilities by remember { mutableStateOf(initial?.facilities ?: "") }
    var locationMenuOpen by remember { mutableStateOf(false) }
    val barangays = listOf("San Juan", "Longos", "San Antonio")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Register New Center" else "Edit Center") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Center Name") }, singleLine = true)
                
                ExposedDropdownMenuBox(expanded = locationMenuOpen, onExpandedChange = { locationMenuOpen = it }) {
                    OutlinedTextField(
                        value = location,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Location / Barangay") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = locationMenuOpen) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = locationMenuOpen, onDismissRequest = { locationMenuOpen = false }) {
                        barangays.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = { location = option; locationMenuOpen = false }
                            )
                        }
                    }
                }
                
                OutlinedTextField(value = capacity, onValueChange = { capacity = it }, label = { Text("Maximum Capacity") }, singleLine = true)
                OutlinedTextField(value = facilities, onValueChange = { facilities = it }, label = { Text("Facilities/Resources") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(
                    (initial ?: EvacCenterEntity(name = "", location = "San Juan", capacity = 0)).copy(
                        name = name.trim(), location = location.trim(),
                        capacity = capacity.toIntOrNull() ?: 0, facilities = facilities.trim()
                    )
                )
            }) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
