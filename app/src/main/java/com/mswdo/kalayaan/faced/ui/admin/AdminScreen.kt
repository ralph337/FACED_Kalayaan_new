package com.mswdo.kalayaan.faced.ui.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.ui.components.*
import com.mswdo.kalayaan.faced.ui.theme.*
import com.mswdo.kalayaan.faced.util.DateUtils

/**
 * Admin-only account management (Functional Requirement #4/#5): the Head MSWDO
 * can create staff accounts and activate/deactivate them. Reached from the
 * Dashboard via a settings icon when the logged-in user is an Admin.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(viewModel: AdminViewModel, onBack: () -> Unit) {
    val users by viewModel.users.collectAsState()
    val auditLogs by viewModel.auditLogs.collectAsState()
    var tab by remember { mutableIntStateOf(0) }
    var showNew by remember { mutableStateOf(false) }
    var selectedUser by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Control Center") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, contentDescription = "Back") } },
                actions = {
                    if (tab == 0) {
                        IconButton(onClick = { showNew = true }) { Icon(Icons.Filled.PersonAdd, contentDescription = "Add Staff") }
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Staff Accounts") })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("Audit Logs") })
            }

            if (tab == 0) {
                val pendingUsers = users.filter { !it.isActive && it.role != UserRole.ADMIN }
                val approvedUsers = users.filter { it.isActive || it.role == UserRole.ADMIN }

                LazyColumn(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (pendingUsers.isNotEmpty()) {
                        item {
                            SectionHeader("Pending Admin Approval (${pendingUsers.size})", "New user registrations requiring verification")
                        }
                        items(pendingUsers, key = { it.id }) { user ->
                            Card(
                                onClick = { selectedUser = user },
                                colors = CardDefaults.cardColors(containerColor = Amber100)
                            ) {
                                Row(
                                    Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.fullName, style = MaterialTheme.typography.titleSmall, color = Slate800)
                                            Spacer(Modifier.width(6.dp))
                                            StatusPill("PENDING", Amber100, Amber700)
                                        }
                                        Text("@${user.username} · ${user.title}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = "View Details", tint = Slate400)
                                }
                            }
                        }
                        item { Spacer(Modifier.height(8.dp)) }
                    }

                    item {
                        SectionHeader("Approved Staff Accounts (${approvedUsers.size})", "Active system administrators and field staff")
                    }
                    if (approvedUsers.isEmpty()) {
                        item { EmptyState(Icons.Filled.People, "No approved accounts found.") }
                    } else {
                        items(approvedUsers, key = { it.id }) { user ->
                            Card(
                                onClick = { selectedUser = user },
                                colors = CardDefaults.cardColors(containerColor = Color.White)
                            ) {
                                Row(
                                    Modifier.padding(14.dp).fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(user.fullName, style = MaterialTheme.typography.titleSmall, color = Slate800)
                                            Spacer(Modifier.width(6.dp))
                                            RoleBadge(isAdmin = user.role == UserRole.ADMIN)
                                        }
                                        Text("@${user.username} · ${user.title}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                                    }
                                    Icon(Icons.Filled.ChevronRight, contentDescription = "View Details", tint = Slate400)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            } else {
                val query by viewModel.auditQuery.collectAsState()
                
                LazyColumn(Modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    item {
                        SectionHeader("System Audit Trail", "${auditLogs.size} recorded action(s) across all users")
                    }
                    item {
                        OutlinedTextField(
                            value = query,
                            onValueChange = viewModel::onAuditQueryChange,
                            placeholder = { Text("Search logs by name, serial, action...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                        )
                    }
                    if (auditLogs.isEmpty()) {
                        item { EmptyState(Icons.Filled.History, "No activity logged yet.") }
                    } else {
                        items(auditLogs, key = { it.id }) { log ->
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                Column(Modifier.padding(12.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(DateUtils.formatTimestamp(log.timestamp), style = MaterialTheme.typography.bodySmall, color = Slate400)
                                        StatusPill(log.eventType.name.replace("_", " "), Blue50, Blue700)
                                    }
                                    Text(log.description, style = MaterialTheme.typography.bodyMedium, color = Slate800)
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        if (log.headName.isNotBlank()) {
                                            Text("Target: ${log.headName} (${log.facedSerial})", style = MaterialTheme.typography.labelSmall, color = Slate500)
                                        }
                                        Text("By: ${log.performedByName.ifBlank { "Staff #" + log.performedByUserId }}", style = MaterialTheme.typography.labelSmall, color = Blue600)
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(40.dp)) }
                }
            }
        }
    }

    if (showNew) {
        NewStaffDialog(onDismiss = { showNew = false }, onCreate = { u, p, n, t, r -> viewModel.createStaff(u, p, n, t, r); showNew = false })
    }

    selectedUser?.let { user ->
        UserDetailDialog(
            user = user,
            onDismiss = { selectedUser = null },
            onToggleActive = { active ->
                viewModel.setActive(user.id, active)
                selectedUser = null
            },
            onDelete = {
                viewModel.delete(user.id)
                selectedUser = null
            }
        )
    }
}

@Composable
private fun UserDetailDialog(
    user: UserEntity,
    onDismiss: () -> Unit,
    onToggleActive: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = Blue600, modifier = Modifier.size(32.dp))
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(user.fullName, style = MaterialTheme.typography.titleMedium)
                    Text("@${user.username}", style = MaterialTheme.typography.bodySmall, color = Slate500)
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Divider(color = Slate200)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Role:", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    RoleBadge(isAdmin = user.role == UserRole.ADMIN)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Position / Title:", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    Text(user.title, style = MaterialTheme.typography.bodyMedium, color = Slate800)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Status:", style = MaterialTheme.typography.bodySmall, color = Slate500)
                    StatusPill(
                        text = if (user.isActive) "APPROVED / ACTIVE" else "PENDING APPROVAL",
                        bg = if (user.isActive) Emerald100 else Amber100,
                        fg = if (user.isActive) Emerald700 else Amber700
                    )
                }
                Divider(color = Slate200)
                Spacer(Modifier.height(4.dp))

                // Action buttons placed safely inside content area to prevent layout overlap
                if (!user.isActive) {
                    Button(
                        onClick = { onToggleActive(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Emerald600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Approve Account")
                    }
                } else if (user.role != UserRole.ADMIN) {
                    OutlinedButton(
                        onClick = { onToggleActive(false) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Deactivate Account", color = Amber700)
                    }
                }

                if (user.role != UserRole.ADMIN) {
                    OutlinedButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Red600),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Delete Account")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.fillMaxWidth()) {
                Text("Close", color = Slate500)
            }
        }
    )
}

@Composable
private fun NewStaffDialog(onDismiss: () -> Unit, onCreate: (String, String, String, String, UserRole) -> Unit) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("Disaster Response Staff") }
    var isAdmin by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Staff Account") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name") }, singleLine = true)
                OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username") }, singleLine = true)
                OutlinedTextField(value = password, onValueChange = { password = it }, label = { Text("Temporary Password") }, singleLine = true)
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title / Position") }, singleLine = true)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isAdmin, onCheckedChange = { isAdmin = it })
                    Text("Grant Admin privileges")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (username.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()) {
                    onCreate(username, password, fullName, title, if (isAdmin) UserRole.ADMIN else UserRole.STAFF)
                }
            }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
