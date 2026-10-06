package com.mswdo.kalayaan.faced.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.mswdo.kalayaan.faced.FacedApp
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.data.repository.SessionManager
import com.mswdo.kalayaan.faced.ui.theme.*

data class BottomDest(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)

/** Change #5: Evacuation Centers + Inventory are combined into one "Centers" destination.
 *  Change #6: "DROMIC" is renamed "Reports". */
val bottomDestinations = listOf(
    BottomDest("dashboard", "Dashboard", Icons.Filled.GridView),
    BottomDest("faced", "FACED", Icons.Filled.Badge),
    BottomDest("centers", "Centers", Icons.Filled.Warehouse),
    BottomDest("reports", "Reports", Icons.Filled.Description)
)

/**
 * Shared shell for the authenticated part of the app. Change #1: the online/offline
 * choice is now made up-front on the Login screen (Log In = online, Continue Offline =
 * Guest Mode), so this top bar only *shows* connectivity + the pending-sync queue and a
 * manual "Sync" button — there's no toggle here anymore.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    app: FacedApp,
    currentUser: UserEntity,
    currentRoute: String,
    pendingSyncCount: Int,
    onNavigate: (String) -> Unit,
    onSwitchAccount: () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    val isOnline by SessionManager.isOnline.collectAsState()
    val isSyncing by app.syncManager.isSyncing.collectAsState()
    val isGuest = currentUser.role == UserRole.GUEST

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = {
                        Column {
                            Text("MSWDO Kalayaan", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text("Disaster & FACED System", style = MaterialTheme.typography.labelSmall, color = Slate400)
                        }
                    },
                    actions = {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                            Box(
                                Modifier.size(34.dp).clip(CircleShape).background(if (isGuest) Slate500 else Blue600),
                                contentAlignment = Alignment.Center
                            ) { Text(currentUser.avatarInitials, color = Color.White, style = MaterialTheme.typography.labelMedium) }
                            Spacer(Modifier.width(6.dp))
                            IconButton(onClick = onSwitchAccount) {
                                Icon(Icons.Filled.SwapHoriz, contentDescription = if (isGuest) "Log in" else "Switch account", tint = Slate500)
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        text = if (isGuest) "● Offline Guest Mode" else if (isOnline) "● Online — ${currentUser.fullName}" else "● Working Offline",
                        bg = if (isGuest) Slate200 else if (isOnline) Emerald100 else Amber100,
                        fg = if (isGuest) Slate700 else if (isOnline) Emerald700 else Amber700
                    )
                    
                    if (!isOnline || isGuest) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Pending: $pendingSyncCount", style = MaterialTheme.typography.labelSmall, color = Slate500)
                            Spacer(Modifier.width(8.dp))
                            Button(
                                onClick = { },
                                enabled = false,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Icon(Icons.Filled.WifiOff, contentDescription = null, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Offline", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    } else if (isSyncing) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Syncing to Firebase...", style = MaterialTheme.typography.labelSmall, color = Slate500)
                        }
                    } else if (pendingSyncCount > 0) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, contentDescription = null, tint = Amber600, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Sync Failed. Retrying soon...", style = MaterialTheme.typography.labelSmall, color = Amber700)
                        }
                    }
                }
                Divider(color = Slate200)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                bottomDestinations.forEach { dest ->
                    NavigationBarItem(
                        selected = currentRoute == dest.route,
                        onClick = { onNavigate(dest.route) },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label, style = MaterialTheme.typography.labelSmall) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Blue600,
                            selectedTextColor = Blue600,
                            indicatorColor = Blue50
                        )
                    )
                }
            }
        },
        containerColor = SlateBg
    ) { padding -> content(padding) }
}
