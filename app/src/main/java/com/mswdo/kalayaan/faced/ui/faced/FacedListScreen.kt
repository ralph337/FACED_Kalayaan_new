package com.mswdo.kalayaan.faced.ui.faced

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
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.data.entity.FacedRecordEntity
import com.mswdo.kalayaan.faced.ui.components.EmptyState
import com.mswdo.kalayaan.faced.ui.components.SectionHeader
import com.mswdo.kalayaan.faced.ui.components.StatusPill
import com.mswdo.kalayaan.faced.ui.theme.*

/**
 * Change #4: this now lists one card per FAMILY (head name + barangay), compiling every
 * filing that family has on record. Tapping a family opens FamilyRecordsScreen, which
 * lists each individual filing; tapping one of those opens the usual FacedDetailScreen.
 */
@Composable
fun FacedListScreen(
    viewModel: FacedListViewModel,
    onFileNew: () -> Unit,
    onOpenFamily: (headName: String, barangay: String) -> Unit,
    onExportCsv: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    var sortMenuOpen by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("FACED Records", "${state.groups.size} families · ${state.groups.sumOf { it.filingCount }} filings")
                Row {
                    IconButton(onClick = onExportCsv) {
                        Icon(Icons.Filled.FileDownload, contentDescription = "Export CSV", tint = Slate500)
                    }
                    Button(onClick = onFileNew, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("File New")
                    }
                }
            }

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQuery,
                placeholder = { Text("Search head, serial, barangay...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = state.statusFilter == EvacuationStatus.IN_CENTER,
                    onClick = { viewModel.onStatusFilter(if (state.statusFilter == EvacuationStatus.IN_CENTER) null else EvacuationStatus.IN_CENTER) },
                    label = { Text("In Center") }
                )
                FilterChip(
                    selected = state.statusFilter == EvacuationStatus.LEFT_CENTER,
                    onClick = { viewModel.onStatusFilter(if (state.statusFilter == EvacuationStatus.LEFT_CENTER) null else EvacuationStatus.LEFT_CENTER) },
                    label = { Text("Left / Returned") }
                )
                Box {
                    AssistChip(onClick = { sortMenuOpen = true }, label = { Text("Sort") }, leadingIcon = { Icon(Icons.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) })
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        DropdownMenuItem(text = { Text("Newest First") }, onClick = { viewModel.onSort(SortMode.DATE_DESC); sortMenuOpen = false })
                        DropdownMenuItem(text = { Text("Head Name (A-Z)") }, onClick = { viewModel.onSort(SortMode.NAME_ASC); sortMenuOpen = false })
                        DropdownMenuItem(text = { Text("Barangay") }, onClick = { viewModel.onSort(SortMode.BARANGAY); sortMenuOpen = false })
                    }
                }
            }
        }

        if (state.groups.isEmpty()) {
            EmptyState(Icons.Filled.Badge, "No FACED records found. File a new form to get started.")
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.groups, key = { it.groupKey }) { group ->
                    FamilyGroupCard(group, onClick = { onOpenFamily(group.headName, group.barangay) })
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun FamilyGroupCard(group: FamilyGroup, onClick: () -> Unit) {
    val latest = group.latest
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(latest.headName, style = MaterialTheme.typography.titleSmall, color = Slate800)
                if (!latest.isSynced) StatusPill("Pending Sync", Amber100, Amber700)
            }
            val disasterText = if (latest.stormName.isNotBlank()) "${latest.disasterEvent} (${latest.stormName})" else latest.disasterEvent
            Text("Brgy. ${group.barangay} · Latest: ${latest.evacCenterName} · $disasterText", style = MaterialTheme.typography.bodySmall, color = Slate500)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                StatusPill(
                    text = if (latest.status == EvacuationStatus.IN_CENTER) "In Center" else "Left / Returned",
                    bg = if (latest.status == EvacuationStatus.IN_CENTER) Emerald100 else Slate200,
                    fg = if (latest.status == EvacuationStatus.IN_CENTER) Emerald700 else Slate700
                )
                StatusPill(
                    text = if (group.filingCount == 1) "1 filing" else "${group.filingCount} filings",
                    bg = Indigo50, fg = Indigo600
                )
            }
        }
    }
}
