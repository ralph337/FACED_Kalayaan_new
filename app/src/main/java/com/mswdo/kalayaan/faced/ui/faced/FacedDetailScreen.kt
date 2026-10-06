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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.ui.components.FormSectionCard
import com.mswdo.kalayaan.faced.ui.components.StatusPill
import com.mswdo.kalayaan.faced.ui.theme.*
import com.mswdo.kalayaan.faced.util.CsvExporter
import com.mswdo.kalayaan.faced.util.FacedPdfGenerator
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacedDetailScreen(
    viewModel: FacedDetailViewModel,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onEdit: () -> Unit
) {
    val full by viewModel.fullRecord.collectAsState()
    val inventory by viewModel.inventory.collectAsState()
    val assistanceForm by viewModel.assistanceForm.collectAsState()
    var tab by remember { mutableStateOf(0) }
    var showAssistanceForm by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var showReturnDialog by remember { mutableStateOf(false) }
    var selectedReturningMembers by remember { mutableStateOf(setOf<String>()) }
    val context = LocalContext.current

    val record = full?.record

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(record?.serial ?: "FACED Record") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.padding(16.dp).size(24.dp), strokeWidth = 2.dp)
                    } else {
                        IconButton(onClick = {
                            full?.let { f ->
                                isExporting = true
                                FacedPdfGenerator.printHtmlPdf(context, f) {
                                    isExporting = false
                                }
                            }
                        }) { Icon(Icons.Filled.FileDownload, contentDescription = "Export Profile as PDF") }
                    }
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "Edit") }
                }
            )
        }
    ) { padding ->
        if (full == null || record == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Profile") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Assistance") })
            }

            if (tab == 0) {
                LazyColumn(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            StatusPill(
                                text = if (record.status == EvacuationStatus.IN_CENTER) "In Center" else "Left / Returned",
                                bg = if (record.status == EvacuationStatus.IN_CENTER) Emerald100 else Slate200,
                                fg = if (record.status == EvacuationStatus.IN_CENTER) Emerald700 else Slate700
                            )
                            OutlinedButton(onClick = {
                                if (record.status == EvacuationStatus.LEFT_CENTER) {
                                    selectedReturningMembers = setOf(record.headName) + full!!.members.map { it.name }
                                    showReturnDialog = true
                                } else {
                                    viewModel.toggleStatus(record.status)
                                }
                            }) {
                                Text(if (record.status == EvacuationStatus.IN_CENTER) "Mark as Left Center" else "Mark as Returned to Center")
                            }
                        }
                    }
                    item {
                        FormSectionCard("Family Filing Info") {
                            InfoRow("Barangay", record.barangay)
                            InfoRow("Evacuation Site", record.evacCenterName)
                            val disasterText = if (record.stormName.isNotBlank()) "${record.disasterEvent} (${record.stormName})" else record.disasterEvent
                            InfoRow("Disaster / Event", disasterText)
                            InfoRow("Synced", if (record.isSynced) "Yes" else "Pending upload")
                        }
                    }
                    item {
                        FormSectionCard("Head of the Family Profile") {
                            InfoRow("Name", record.headName)
                            InfoRow("Sex", record.sex)
                            InfoRow("Civil Status", record.civilStatus)
                            InfoRow("Birthdate", record.birthDate)
                            InfoRow("Age", record.age.toString())
                            InfoRow("Birthplace", record.birthPlace)
                            InfoRow("Occupation", record.occupation)
                            InfoRow("Religion", record.religion)
                            InfoRow("Net Monthly Income", "₱${record.monthlyIncome}")
                            InfoRow("ID Presented", "${record.idType} - ${record.idNumber}")
                            InfoRow("Primary Contact", record.contactNumber)
                            InfoRow("Alt Contact", record.altContactNumber.ifBlank { "N/A" })
                            InfoRow("Mother's Maiden Name", record.mothersMaidenName)
                            InfoRow("Permanent Address", record.permanentAddress)
                            InfoRow("4Ps Program", if (record.is4Ps) "YES" else "NO")
                            InfoRow("Shelter Assessment", record.shelterDamage.name.replace("_", " "))
                        }
                    }
                    item {
                        FormSectionCard("Household Members Roster (${full!!.members.size})") {
                            if (full!!.members.isEmpty()) Text("No additional members recorded.", color = Slate400, style = MaterialTheme.typography.bodySmall)
                            full!!.members.forEach { m ->
                                Column(Modifier.padding(vertical = 4.dp)) {
                                    Text("${m.name} — ${m.relation}", style = MaterialTheme.typography.labelMedium, color = Slate800)
                                    Text("${m.sex} · Age ${m.age} (${m.ageCategory}) · ${m.education} · ${m.occupation}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    if (m.remarks.isNotBlank()) Text("Remarks: ${m.remarks}", style = MaterialTheme.typography.bodySmall, color = Amber700)
                                }
                                Divider(color = Slate200)
                            }
                        }
                    }
                    item {
                        FormSectionCard("Tagged Vulnerable Family Members (${full!!.vulnerableTags.size})") {
                            if (full!!.vulnerableTags.isEmpty()) Text("None tagged.", color = Slate400, style = MaterialTheme.typography.bodySmall)
                            full!!.vulnerableTags.forEach { t ->
                                StatusPill("${t.personName} — ${t.vulnerabilityType}", Amber100, Amber700)
                            }
                        }
                    }
                    item { Spacer(Modifier.height(30.dp)) }
                }
            } else {
                LazyColumn(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Assistance / Relief History", style = MaterialTheme.typography.titleMedium, color = Slate800)
                            Button(onClick = { showAssistanceForm = !showAssistanceForm }) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Record")
                            }
                        }
                    }
                    if (full!!.assistanceHistory.isEmpty()) {
                        item { Text("No assistance recorded yet for this family.", color = Slate400, style = MaterialTheme.typography.bodySmall) }
                    }
                    items(full!!.assistanceHistory) { a ->
                        Card(colors = CardDefaults.cardColors(containerColor = Purple50)) {
                            Column(Modifier.padding(12.dp)) {
                                Text("${a.itemName} × ${a.qty} ${a.unit}", style = MaterialTheme.typography.labelMedium, color = Purple900)
                                Text("${a.date} · ${a.receivingMember} · ${a.disaster}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                Text("Cost: ₱${a.estimatedCost} · Provider: ${a.provider}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                Text("Authorized by: ${a.authorizingOfficer}", style = MaterialTheme.typography.bodySmall, color = Slate400)
                            }
                        }
                    }

                    if (showAssistanceForm) {
                        item {
                            AssistanceRecordForm(
                                formState = assistanceForm,
                                inventory = inventory,
                                defaultReceiver = record.headName,
                                onUpdate = viewModel::updateAssistanceForm,
                                onSubmit = { viewModel.submitAssistance(record.headName) { showAssistanceForm = false } }
                            )
                        }
                    }
                    item { Spacer(Modifier.height(30.dp)) }
                }
            }
        }

        if (showReturnDialog) {
            AlertDialog(
                onDismissRequest = { showReturnDialog = false },
                title = { Text("Returning Family Members") },
                text = {
                    Column(Modifier.fillMaxWidth()) {
                        Text("Select which family members are returning to the Evacuation Center:", style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.padding(bottom = 8.dp))
                        LazyColumn(Modifier.weight(1f, fill = false)) {
                            val allNames = listOf(record.headName) + full!!.members.map { it.name }
                            items(allNames) { name ->
                                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(
                                        checked = selectedReturningMembers.contains(name),
                                        onCheckedChange = { checked ->
                                            selectedReturningMembers = if (checked) selectedReturningMembers + name else selectedReturningMembers - name
                                        }
                                    )
                                    Text(name, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        viewModel.toggleStatus(record.status, selectedReturningMembers.size)
                        showReturnDialog = false
                    }) {
                        Text("Check In (${selectedReturningMembers.size})")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
                }
            )
        }
    }

    if (showReturnDialog) {
        AlertDialog(
            onDismissRequest = { showReturnDialog = false },
            title = { Text("Returning Family Members") },
            text = {
                Column(Modifier.fillMaxWidth()) {
                    Text("Select which family members are returning to the Evacuation Center:", style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.padding(bottom = 8.dp))
                    LazyColumn(Modifier.weight(1f, fill = false)) {
                        val allNames = listOf(record!!.headName) + full!!.members.map { it.name }
                        items(allNames) { name ->
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = selectedReturningMembers.contains(name),
                                    onCheckedChange = { checked ->
                                        selectedReturningMembers = if (checked) selectedReturningMembers + name else selectedReturningMembers - name
                                    }
                                )
                                Text(name, style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleStatus(record!!.status, selectedReturningMembers.size)
                    showReturnDialog = false
                }) {
                    Text("Check In (${selectedReturningMembers.size})")
                }
            },
            dismissButton = {
                TextButton(onClick = { showReturnDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Slate500, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.labelMedium, color = Slate800, modifier = Modifier.weight(1.2f))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssistanceRecordForm(
    formState: AssistanceFormState,
    inventory: List<com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity>,
    defaultReceiver: String,
    onUpdate: ((AssistanceFormState) -> AssistanceFormState) -> Unit,
    onSubmit: () -> Unit
) {
    var itemMenuOpen by remember { mutableStateOf(false) }
    var datePickerOpen by remember { mutableStateOf(false) }

    FormSectionCard("Record New Assistance") {
        OutlinedTextField(
            value = formState.date, onValueChange = {}, readOnly = true,
            label = { Text("Date") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            trailingIcon = { IconButton(onClick = { datePickerOpen = true }) { Icon(Icons.Filled.CalendarToday, contentDescription = null) } }
        )
        if (datePickerOpen) {
            val datePickerState = rememberDatePickerState(initialSelectedDateMillis = System.currentTimeMillis())
            DatePickerDialog(
                onDismissRequest = { datePickerOpen = false },
                confirmButton = {
                    TextButton(onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                                Date(millis)
                            )
                            onUpdate { it.copy(date = dateString) }
                        }
                        datePickerOpen = false
                    }) { Text("OK") }
                },
                dismissButton = { TextButton(onClick = { datePickerOpen = false }) { Text("Cancel") } }
            ) { DatePicker(state = datePickerState) }
        }
        OutlinedTextField(
            value = formState.receivingMember.ifBlank { defaultReceiver },
            onValueChange = { r -> onUpdate { it.copy(receivingMember = r) } },
            label = { Text("Receiving Member") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        Box {
            OutlinedTextField(
                value = formState.itemName, onValueChange = {}, readOnly = true,
                label = { Text("Assistance Item *") },
                trailingIcon = { IconButton(onClick = { itemMenuOpen = true }) { Icon(Icons.Filled.ArrowDropDown, contentDescription = null) } },
                modifier = Modifier.fillMaxWidth()
            )
            DropdownMenu(expanded = itemMenuOpen, onDismissRequest = { itemMenuOpen = false }) {
                inventory.forEach { item ->
                    DropdownMenuItem(
                        text = { Text("${item.name} (${item.qty} ${item.unit} left)") },
                        onClick = {
                            onUpdate { it.copy(inventoryItemId = item.id, itemName = item.name, unit = item.unit) }
                            itemMenuOpen = false
                        }
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(
                value = formState.qty, onValueChange = { q -> onUpdate { it.copy(qty = q) } },
                label = { Text("Quantity") }, singleLine = true, modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = formState.cost, onValueChange = { c -> onUpdate { it.copy(cost = c) } },
                label = { Text("Est. Cost (₱)") }, singleLine = true, modifier = Modifier.weight(1f)
            )
        }
        OutlinedTextField(
            value = formState.authorizingOfficer, onValueChange = { a -> onUpdate { it.copy(authorizingOfficer = a) } },
            label = { Text("Authorizing Officer") }, singleLine = true, modifier = Modifier.fillMaxWidth()
        )
        if (formState.insufficientStock) {
            Text("Insufficient stock for this item. Please adjust inventory first.", color = Red600, style = MaterialTheme.typography.bodySmall)
        }
        if (formState.error != null) {
            Text(formState.error, color = Red600, style = MaterialTheme.typography.bodySmall)
        }
        Button(onClick = onSubmit, modifier = Modifier.fillMaxWidth()) { Text("Save Assistance Record") }
    }
}
