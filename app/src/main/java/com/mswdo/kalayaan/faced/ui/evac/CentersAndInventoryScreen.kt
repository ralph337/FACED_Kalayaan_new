package com.mswdo.kalayaan.faced.ui.evac

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mswdo.kalayaan.faced.ui.inventory.InventoryScreen
import com.mswdo.kalayaan.faced.ui.inventory.InventoryViewModel

/**
 * Change #5: Evacuation Centers and Relief Inventory now live under one bottom-nav
 * destination, since staff manage both while running the same operation. Each tab
 * keeps its own header/add-button/list exactly as before — only the entry point changed.
 */
@Composable
fun CentersAndInventoryScreen(
    evacViewModel: EvacViewModel,
    inventoryViewModel: InventoryViewModel,
    isAdmin: Boolean
) {
    var tab by remember { mutableStateOf(0) }

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Evacuation Centers") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Relief Inventory") })
        }
        when (tab) {
            0 -> EvacCentersScreen(evacViewModel, isAdmin)
            else -> InventoryScreen(inventoryViewModel, isAdmin)
        }
    }
}
