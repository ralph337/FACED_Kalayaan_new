package com.mswdo.kalayaan.faced.ui.faced

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.entity.ShelterDamage
import com.mswdo.kalayaan.faced.ui.components.FormSectionCard
import com.mswdo.kalayaan.faced.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BARANGAYS = listOf("San Juan", "Longos", "San Antonio")
private val DISASTERS = listOf("Typhoon / Severe Storm", "Flash Flood / Inundation", "Earthquake", "Fire Incident", "Landslide", "Others")
private val VULNERABILITY_TYPES = listOf("Senior Citizen (60+)", "Person with Disability (PWD)", "Pregnant Woman", "Lactating Mother", "Child/Minor", "Solo Parent")
private val EDUCATION_LEVELS = listOf("None", "Day Care / Pre-School", "Elementary Level", "Elementary Grad", "High School Level", "High School Grad", "Vocational", "College Level", "College Grad", "Post-Graduate")
private val INCOME_BRACKETS = listOf("No Income", "Below ₱5,000", "₱5,000 - ₱10,000", "₱10,000 - ₱20,000", "₱20,000 - ₱50,000", "Above ₱50,000")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FacedFormScreen(
    viewModel: FacedFormViewModel,
    centers: List<EvacCenterEntity>,
    onCancel: () -> Unit,
    onSaved: (String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    var currentStep by remember { mutableIntStateOf(0) }

    LaunchedEffect(state.savedSerial) {
        state.savedSerial?.let { onSaved(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditing) "Edit FACED Form (Step ${currentStep+1}/4)" else "File New FACED Form (Step ${currentStep+1}/4)") },
                navigationIcon = {
                    IconButton(onClick = onCancel) { Icon(Icons.Filled.Close, contentDescription = "Cancel") }
                }
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (currentStep == 0) {
                        OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    } else {
                        OutlinedButton(onClick = { currentStep-- }, modifier = Modifier.weight(1f)) { Text("Back") }
                    }

                    if (currentStep < 3) {
                        Button(
                            onClick = {
                                val stepError = viewModel.validateStep(currentStep)
                                if (stepError == null) {
                                    currentStep++
                                    viewModel.clearError()
                                } else {
                                    viewModel.showError(stepError)
                                }
                            }, 
                            modifier = Modifier.weight(1f)
                        ) { Text("Next") }
                    } else {
                        Button(onClick = { viewModel.save() }, enabled = !state.isSaving, modifier = Modifier.weight(1f)) {
                            if (state.isSaving) CircularProgressIndicator(Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            else Text(if (state.isEditing) "Save Changes" else "File Form")
                        }
                    }
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Text("Serial: ${state.serial}", style = MaterialTheme.typography.labelMedium, color = Slate500)
            }

            if (state.error != null) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Red100)) {
                        Text(state.error!!, Modifier.padding(12.dp), color = Red600, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            if (currentStep == 0) {
                item {
                    FormSectionCard("Filing Details") {
                        LabeledDropdown("Barangay *", BARANGAYS, state.barangay, enabled = !state.isHeadNameLocked) { viewModel.update { s -> s.copy(barangay = it) } }
                        LabeledDropdown("Evacuation Center *", centers.map { it.name }, state.evacCenterName) { viewModel.update { s -> s.copy(evacCenterName = it) } }
                        LabeledDropdown("Disaster / Emergency Event *", DISASTERS, state.disasterEvent) { viewModel.update { s -> s.copy(disasterEvent = it) } }
                        if (state.disasterEvent == "Typhoon / Severe Storm") {
                            LabeledField("Storm Name", state.stormName) { viewModel.update { s -> s.copy(stormName = it) } }
                        }
                        LabeledSwitch("Registered 4Ps Beneficiary", state.is4Ps) { viewModel.update { s -> s.copy(is4Ps = it) } }
                        LabeledDropdown(
                            "Shelter Assessment", ShelterDamage.values().map { it.name.replace("_", " ") },
                            state.shelterDamage.name.replace("_", " ")
                        ) { v -> viewModel.update { s -> s.copy(shelterDamage = ShelterDamage.valueOf(v.replace(" ", "_"))) } }
                    }
                }
            }

            if (currentStep == 1) {
                item {
                    FormSectionCard("Head of the Family Profile") {
                        LabeledField("Full Name *", state.headName, enabled = !state.isHeadNameLocked) { viewModel.update { s -> s.copy(headName = it) } }
                        if (state.isHeadNameLocked) {
                            Text(
                                "Head of family name is locked to keep this filing grouped under the same family.",
                                style = MaterialTheme.typography.labelSmall, color = Slate400
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { LabeledDropdown("Sex", listOf("Male", "Female"), state.sex) { viewModel.update { s -> s.copy(sex = it) } } }
                            Box(Modifier.weight(1f)) { LabeledDropdown("Civil Status", listOf("Single", "Married", "Widowed", "Separated"), state.civilStatus) { viewModel.update { s -> s.copy(civilStatus = it) } } }
                        }
                        DateField("Birthdate", state.birthDate) { viewModel.update { s -> s.copy(birthDate = it) } }
                        LabeledField("Birthplace", state.birthPlace) { viewModel.update { s -> s.copy(birthPlace = it) } }
                        LabeledField("Occupation", state.occupation) { viewModel.update { s -> s.copy(occupation = it) } }
                        LabeledField("Religion", state.religion) { viewModel.update { s -> s.copy(religion = it) } }
                        LabeledDropdown("Net Monthly Income", INCOME_BRACKETS, state.monthlyIncome) { viewModel.update { s -> s.copy(monthlyIncome = it) } }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(Modifier.weight(1f)) { LabeledDropdown("ID Presented", listOf("PhilID", "Voter's ID", "Barangay ID", "None"), state.idType) { viewModel.update { s -> s.copy(idType = it) } } }
                            Box(Modifier.weight(1f)) { LabeledField("ID Number", state.idNumber) { viewModel.update { s -> s.copy(idNumber = it) } } }
                        }
                        LabeledField("Primary Contact Number *", state.contactNumber, KeyboardType.Phone) { viewModel.update { s -> s.copy(contactNumber = it) } }
                        LabeledField("Alternate Contact Number", state.altContactNumber, KeyboardType.Phone) { viewModel.update { s -> s.copy(altContactNumber = it) } }
                        LabeledField("Mother's Maiden Name", state.mothersMaidenName) { viewModel.update { s -> s.copy(mothersMaidenName = it) } }
                        LabeledField("Permanent Address", state.permanentAddress) { viewModel.update { s -> s.copy(permanentAddress = it) } }
                    }
                }
            }

            if (currentStep == 2) {
                item {
                    FormSectionCard("Household Members Roster") {
                        state.members.forEachIndexed { index, member ->
                            MemberEditor(
                                member = member,
                                onChange = { viewModel.updateMember(index, it) },
                                onRemove = { viewModel.removeMember(index) }
                            )
                            Divider(color = Slate200)
                        }
                        OutlinedButton(onClick = { viewModel.addMember() }) {
                            Icon(Icons.Filled.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Add Household Member")
                        }
                    }
                }
            }

            if (currentStep == 3) {
                item {
                    FormSectionCard("Tagged Vulnerable Family Members") {
                        val allNames = listOf(state.headName.ifBlank { "(Head of Family)" }) + state.members.mapNotNull { it.name.ifBlank { null } }
                        allNames.forEach { person ->
                            Text(person, style = MaterialTheme.typography.labelMedium, color = Slate700)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState())
                            ) {
                                VULNERABILITY_TYPES.forEach { type ->
                                    val checked = state.vulnerableTags.any { it.personName == person && it.vulnerabilityType == type }
                                    FilterChip(
                                        selected = checked,
                                        onClick = { viewModel.toggleVulnerable(person, type) },
                                        label = { Text(type, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(60.dp)) }
        }
    }

    if (state.duplicateSerial != null) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDuplicateDialog() },
            icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = Red600) },
            title = { Text("Family Already Registered") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "The family head \"${state.duplicateHeadName}\" is already registered in this barangay " +
                        "(Serial: ${state.duplicateSerial})."
                    )
                    Text(
                        "You cannot create duplicate forms for the same Head of the Family. " +
                        "If you need to add household members, please update the existing record instead.",
                        style = MaterialTheme.typography.labelMedium, color = Slate700
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.updateExistingInstead() }) {
                    Text("Update Existing Record")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDuplicateDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun MemberEditor(member: MemberDraft, onChange: (MemberDraft) -> Unit, onRemove: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Member", style = MaterialTheme.typography.labelMedium, color = Slate500)
            IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove", tint = Red500) }
        }
        LabeledField("Full Name", member.name) { onChange(member.copy(name = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { LabeledField("Relation to Head", member.relation) { onChange(member.copy(relation = it)) } }
            Box(Modifier.weight(1f)) { LabeledDropdown("Sex", listOf("Male", "Female"), member.sex) { onChange(member.copy(sex = it)) } }
        }
        DateField("Birthdate", member.birthDate) { onChange(member.copy(birthDate = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f)) { LabeledDropdown("Education", EDUCATION_LEVELS, member.education) { onChange(member.copy(education = it)) } }
            Box(Modifier.weight(1f)) { LabeledField("Occupation", member.occupation) { onChange(member.copy(occupation = it)) } }
        }
        LabeledField("Remarks", member.remarks) { onChange(member.copy(remarks = it)) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(label: String, value: String, onChange: (String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    
    OutlinedTextField(
        value = value,
        onValueChange = {},
        label = { Text(label) },
        readOnly = true,
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showDialog = true }) {
                Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
            }
        },
        modifier = Modifier.fillMaxWidth()
    )

    if (showDialog) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // Convert millis to YYYY-MM-DD
                        val dateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(
                            Date(millis)
                        )
                        onChange(dateString)
                    }
                    showDialog = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
private fun LabeledField(label: String, value: String, keyboardType: KeyboardType = KeyboardType.Text, enabled: Boolean = true, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label) },
        singleLine = true,
        enabled = enabled,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun LabeledSwitch(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = Slate700)
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LabeledDropdown(label: String, options: List<String>, selected: String, enabled: Boolean = true, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded && enabled, onExpandedChange = { if (enabled) expanded = it }) {
        OutlinedTextField(
            value = selected,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(label) },
            trailingIcon = { if (enabled) ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded && enabled, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}
