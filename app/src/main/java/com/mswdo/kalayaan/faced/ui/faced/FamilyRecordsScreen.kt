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
import com.mswdo.kalayaan.faced.ui.components.StatusPill
import com.mswdo.kalayaan.faced.ui.theme.*
import com.mswdo.kalayaan.faced.util.DateUtils

/**
 * Change #4: every FACED form ever filed for one family, compiled in one place.
 * Reached by tapping a family card on the FACED tab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FamilyRecordsScreen(
    headName: String,
    barangay: String,
    viewModel: FacedFamilyViewModel,
    onBack: () -> Unit,
    onOpenFiling: (String) -> Unit
) {
    val filings by viewModel.filings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(headName) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Text(
                "Brgy. $barangay · ${filings.size} filing(s) on record",
                style = MaterialTheme.typography.bodyMedium, color = Slate500,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
            if (filings.isEmpty()) {
                EmptyState(Icons.Filled.Badge, "No filings found for this family.")
            } else {
                LazyColumn(Modifier.padding(horizontal = 16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(filings, key = { it.serial }) { filing ->
                        FilingCard(filing, onClick = { onOpenFiling(filing.serial) })
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }
}

@Composable
private fun FilingCard(filing: FacedRecordEntity, onClick: () -> Unit) {
    Card(onClick = onClick, colors = CardDefaults.cardColors(containerColor = androidx.compose.ui.graphics.Color.White)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(filing.serial, style = MaterialTheme.typography.labelMedium, color = Blue700)
                if (!filing.isSynced) StatusPill("Pending Sync", Amber100, Amber700)
            }
            Text(filing.disasterEvent, style = MaterialTheme.typography.titleSmall, color = Slate800)
            Text("${filing.evacCenterName} · Filed ${DateUtils.formatTimestamp(filing.createdAt)}", style = MaterialTheme.typography.bodySmall, color = Slate500)
            StatusPill(
                text = if (filing.status == EvacuationStatus.IN_CENTER) "In Center" else "Left / Returned",
                bg = if (filing.status == EvacuationStatus.IN_CENTER) Emerald100 else Slate200,
                fg = if (filing.status == EvacuationStatus.IN_CENTER) Emerald700 else Slate700
            )
        }
    }
}
