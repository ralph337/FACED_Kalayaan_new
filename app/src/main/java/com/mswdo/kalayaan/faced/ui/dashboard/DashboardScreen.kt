package com.mswdo.kalayaan.faced.ui.dashboard

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.ui.components.BarangayEvacueeBarChart
import com.mswdo.kalayaan.faced.ui.components.CenterByBarangayBarChart
import com.mswdo.kalayaan.faced.ui.components.EmptyState
import com.mswdo.kalayaan.faced.ui.components.ProgressBarRow
import com.mswdo.kalayaan.faced.ui.components.ReliefStockBarChart
import com.mswdo.kalayaan.faced.ui.components.SectionHeader
import com.mswdo.kalayaan.faced.ui.components.StatCard
import com.mswdo.kalayaan.faced.ui.components.VulnerabilityDonutChart
import com.mswdo.kalayaan.faced.ui.theme.*

@Composable
fun DashboardScreen(viewModel: DashboardViewModel, isAdmin: Boolean = false, onManageStaff: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.weight(1f)) {
                    SectionHeader(
                        "Emergency Operations Dashboard",
                        "Municipality of Kalayaan, Laguna — Evacuation & Relief Overview"
                    )
                }
                if (isAdmin) {
                    IconButton(onClick = onManageStaff) {
                        Icon(Icons.Filled.AdminPanelSettings, contentDescription = "Manage Staff Accounts", tint = Blue600)
                    }
                }
            }
        }

        item {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.height(200.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    StatCard(Modifier.fillMaxWidth(), Icons.Filled.Groups, "Registered Families", state.totalFamilies.toString(), Blue600, Blue50)
                }
                item {
                    StatCard(Modifier.fillMaxWidth(), Icons.Filled.Person, "Families In Centers", state.totalIndividuals.toString(), Indigo600, Indigo50)
                }
                item {
                    StatCard(Modifier.fillMaxWidth(), Icons.Filled.Accessible, "Vulnerable Individuals", state.vulnerableCount.toString(), Amber600, Amber100)
                }
                item {
                    StatCard(Modifier.fillMaxWidth(), Icons.Filled.Inventory2, "Total Items in Stock", state.totalStock.toString(), Emerald600, Emerald100)
                }
            }
        }

        if (state.lowStockItemCount > 0) {
            item {
                Card(colors = CardDefaults.cardColors(containerColor = Amber100)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber700)
                        Spacer(Modifier.width(8.dp))
                        Text("${state.lowStockItemCount} inventory item(s) are at low stock.", color = Amber700, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        if (state.evacueesPerBarangay.isNotEmpty()) {
            item {
                BarangayEvacueeBarChart(data = state.evacueesPerBarangay)
            }
        }

        listOf("San Juan", "Longos", "San Antonio").forEach { b ->
            val centerList = state.centersByBarangay[b] ?: emptyList()
            if (centerList.isNotEmpty()) {
                item {
                    CenterByBarangayBarChart(barangayName = b, centers = centerList)
                }
            }
        }

        if (state.inventory.isNotEmpty()) {
            item {
                ReliefStockBarChart(inventory = state.inventory)
            }
        }

        if (state.vulnerabilityBreakdown.any { it.second > 0 }) {
            item {
                VulnerabilityDonutChart(breakdown = state.vulnerabilityBreakdown)
            }
        }

        item {
            Text("Evacuation Centers Real-Time Capacity", style = MaterialTheme.typography.titleMedium, color = Slate800)
        }

        if (state.centers.isEmpty()) {
            item { EmptyState(Icons.Filled.Home, "No evacuation centers registered yet.") }
        } else {
            items(state.centers) { c ->
                Card(colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(c.center.name, style = MaterialTheme.typography.titleSmall, color = Slate800)
                        Text(c.center.location, style = MaterialTheme.typography.labelSmall, color = Slate400)
                        ProgressBarRow(c.center.name, c.percentFull, c.occupied, c.center.capacity)
                    }
                }
            }
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}
