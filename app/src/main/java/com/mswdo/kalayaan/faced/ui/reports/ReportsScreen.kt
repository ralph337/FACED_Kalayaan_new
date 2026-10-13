package com.mswdo.kalayaan.faced.ui.reports

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.ui.components.EmptyState
import com.mswdo.kalayaan.faced.ui.components.ProgressBarRow
import com.mswdo.kalayaan.faced.ui.components.SectionHeader
import com.mswdo.kalayaan.faced.ui.theme.*
import com.mswdo.kalayaan.faced.util.CsvExporter
import com.mswdo.kalayaan.faced.util.DateUtils
import com.mswdo.kalayaan.faced.util.ExcelExporter
import kotlinx.coroutines.launch

/** Change #6: bottom-nav "Reports" — two report types, switched with an inner tab row. */
@Composable
fun ReportsScreen(viewModel: ReportsViewModel) {
    var tab by remember { mutableStateOf(0) }
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("FACED Form Report") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Disaster Summary") })
        }
        when (tab) {
            0 -> FacedEventReportTab(viewModel)
            else -> DisasterSummaryTab(viewModel)
        }
    }
}

@Composable
private fun FacedEventReportTab(viewModel: ReportsViewModel) {
    val events by viewModel.events.collectAsState()
    val query by viewModel.eventQuery.collectAsState()
    val context = LocalContext.current

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("FACED Form Report", "Date & time log of check-ins, check-outs, filings, and assistance given")
                Button(onClick = {
                    val header = listOf("Timestamp", "Serial", "Head of Family", "Event Type", "Description")
                    val rows = events.map { listOf(DateUtils.formatTimestamp(it.timestamp), it.facedSerial, it.headName, it.eventType.name, it.description) }
                    val uri = CsvExporter.export(context, "FACED_Activity_Report_${DateUtils.today()}.csv", header, rows)
                    context.startActivity(CsvExporter.shareIntent(context, uri))
                }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export CSV")
                }
            }
        }
        item {
            OutlinedTextField(
                value = query,
                onValueChange = viewModel::onEventQueryChange,
                placeholder = { Text("Search logs by name, serial, action...") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
        if (events.isEmpty()) {
            item { EmptyState(Icons.Filled.History, "No activity recorded yet.") }
        } else {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp, bottomStart = 0.dp, bottomEnd = 0.dp),
                    colors = CardDefaults.cardColors(containerColor = Slate100)
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Time", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.weight(1f))
                        Text("Serial / Head", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.weight(1.5f))
                        Text("Action", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.weight(1f))
                        Text("Details", style = MaterialTheme.typography.labelSmall, color = Slate500, modifier = Modifier.weight(2f))
                    }
                }
            }
            items(events, key = { it.id }) { event -> EventTableRow(event) }
        }
        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun EventTableRow(event: EventLogEntity) {
    val tint = when (event.eventType) {
        EventType.CHECK_IN -> Emerald600
        EventType.CHECK_OUT -> Slate500
        EventType.FORM_FILED -> Blue600
        EventType.FORM_UPDATED -> Amber600
        EventType.ASSISTANCE_GIVEN -> Purple700
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(DateUtils.formatTimestamp(event.timestamp), style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.weight(1f))
            Column(Modifier.weight(1.5f)) {
                Text(event.facedSerial, style = MaterialTheme.typography.labelSmall, color = Slate800)
                Text(event.headName, style = MaterialTheme.typography.bodySmall, color = Slate500)
            }
            Text(event.eventType.name.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = tint, modifier = Modifier.weight(1f))
            Text(event.description, style = MaterialTheme.typography.bodySmall, color = Slate800, modifier = Modifier.weight(2f))
        }
        Divider(color = Slate200)
    }
}

@Composable
private fun DisasterSummaryTab(viewModel: ReportsViewModel) {
    val state by viewModel.disasterSummary.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                SectionHeader("Disaster Summary", state.disasterSummaryTitle)
                Button(onClick = {
                    scope.launch {
                        val fullRecords = viewModel.getFullRecords()
                        val uri = ExcelExporter.exportDromicReport(
                            context,
                            "DROMIC_Disaster_Report_${DateUtils.today()}.xls",
                            fullRecords,
                            "All Evacuation Centers",
                            state.disasterSummaryTitle,
                            DateUtils.today()
                        )
                        context.startActivity(ExcelExporter.shareIntent(context, uri))
                    }
                }) {
                    Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export Excel (DROMIC)")
                }
            }
        }

        item {
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Summary", style = MaterialTheme.typography.titleSmall, color = Blue700)
                    Divider(color = Slate200)
                    ReportRow("Registered Families", state.totalFamilies.toString())
                    ReportRow("Individuals Currently In Centers (heads)", state.totalIndividuals.toString())
                    ReportRow("Total Tagged Vulnerable Persons", state.vulnerableTotal.toString())
                    ReportRow("Senior Citizens (60+)", state.seniorCount.toString())
                    ReportRow("Persons with Disability (PWD)", state.pwdCount.toString())
                    ReportRow("Pregnant / Lactating Women", state.pregnantLactatingCount.toString())
                    ReportRow("Minors Flagged", state.minorCount.toString())
                }
            }
        }

        item { Text("Evacuation Center Occupancy", style = MaterialTheme.typography.titleMedium, color = Slate800) }
        items(state.centers) { c ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(Modifier.padding(14.dp)) {
                    Text(c.center.name, style = MaterialTheme.typography.titleSmall, color = Slate800)
                    ProgressBarRow(c.center.name, c.percentFull, c.occupied, c.center.capacity)
                }
            }
        }

        item { Text("Relief Inventory", style = MaterialTheme.typography.titleMedium, color = Slate800) }
        items(state.inventory) { i ->
            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(12.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${i.name} (${i.category})", style = MaterialTheme.typography.bodyMedium, color = Slate700)
                    Text("${i.qty} ${i.unit}", style = MaterialTheme.typography.labelMedium, color = Slate800)
                }
            }
        }

        item { Spacer(Modifier.height(80.dp)) }
    }
}

@Composable
private fun ReportRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500)
        Text(value, style = MaterialTheme.typography.labelMedium, color = Slate800)
    }
}
