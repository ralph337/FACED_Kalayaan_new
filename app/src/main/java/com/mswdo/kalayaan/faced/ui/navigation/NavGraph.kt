package com.mswdo.kalayaan.faced.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.mswdo.kalayaan.faced.FacedApp
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.data.repository.SessionManager
import com.mswdo.kalayaan.faced.ui.admin.AdminScreen
import com.mswdo.kalayaan.faced.ui.admin.AdminViewModel
import com.mswdo.kalayaan.faced.ui.components.AppScaffold
import com.mswdo.kalayaan.faced.ui.dashboard.DashboardScreen
import com.mswdo.kalayaan.faced.ui.dashboard.DashboardViewModel
import com.mswdo.kalayaan.faced.ui.evac.CentersAndInventoryScreen
import com.mswdo.kalayaan.faced.ui.evac.EvacViewModel
import com.mswdo.kalayaan.faced.ui.faced.*
import com.mswdo.kalayaan.faced.ui.inventory.InventoryViewModel
import com.mswdo.kalayaan.faced.ui.login.LoginScreen
import com.mswdo.kalayaan.faced.ui.login.LoginViewModel
import com.mswdo.kalayaan.faced.ui.reports.ReportsScreen
import com.mswdo.kalayaan.faced.ui.reports.ReportsViewModel
import com.mswdo.kalayaan.faced.util.CsvExporter
import com.mswdo.kalayaan.faced.util.DateUtils
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.net.URLEncoder

private const val ROUTE_LOGIN = "login"
private const val ROUTE_DASHBOARD = "dashboard"
private const val ROUTE_FACED_LIST = "faced"
private const val ROUTE_FACED_NEW = "faced/new"
private const val ROUTE_FACED_NEW_FOR_FAMILY = "faced/new/{headName}/{barangay}"
private const val ROUTE_FACED_EDIT = "faced/edit/{serial}"
private const val ROUTE_FACED_DETAIL = "faced/detail/{serial}"
private const val ROUTE_FACED_FAMILY = "faced/family/{headName}/{barangay}"
private const val ROUTE_CENTERS = "centers"
private const val ROUTE_REPORTS = "reports"
private const val ROUTE_ADMIN = "admin"

private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
private fun dec(s: String) = URLDecoder.decode(s, "UTF-8")

@Composable
fun AppNavGraph(app: FacedApp) {
    val navController = rememberNavController()
    var currentUser by remember { mutableStateOf<UserEntity?>(null) }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_LOGIN

    NavHost(navController = navController, startDestination = ROUTE_LOGIN) {

        composable(ROUTE_LOGIN) {
            val vm: LoginViewModel = viewModel(factory = AppViewModelFactory(app))
            val scope = rememberCoroutineScope()
            var pendingRealUser by remember { mutableStateOf<UserEntity?>(null) }

            fun goToApp(user: UserEntity) {
                currentUser = user
                navController.navigate(ROUTE_DASHBOARD) { popUpTo(ROUTE_LOGIN) { inclusive = true } }
            }

            LoginScreen(vm) { user ->
                if (user.role == UserRole.GUEST) {
                    goToApp(user)
                } else {
                    // Change #2: a real, online login checks for data entered earlier in
                    // Offline Guest Mode and offers to sync it into this account.
                    scope.launch {
                        if (app.syncManager.hasGuestPendingData()) {
                            pendingRealUser = user
                        } else {
                            goToApp(user)
                        }
                    }
                }
            }

            pendingRealUser?.let { realUser ->
                AlertDialog(
                    onDismissRequest = {},
                    title = { Text("Sync Offline Data?") },
                    text = {
                        Text(
                            "This device has FACED forms, check-ins/check-outs, or assistance records entered " +
                                "in Offline Guest Mode before you logged in. Sync them into ${realUser.fullName}'s account now?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                app.syncManager.syncGuestDataToUser(realUser.id)
                                pendingRealUser = null
                                goToApp(realUser)
                            }
                        }) { Text("Sync Now") }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            pendingRealUser = null
                            goToApp(realUser)
                        }) { Text("Not Now") }
                    }
                )
            }
        }

        composable(ROUTE_DASHBOARD) {
            RequireUser(currentUser, navController) { user ->
                ShellWithBottomNav(app, user, ROUTE_DASHBOARD, navController) {
                    val vm: DashboardViewModel = viewModel(factory = AppViewModelFactory(app))
                    DashboardScreen(
                        vm,
                        isAdmin = user.role == UserRole.ADMIN,
                        onManageStaff = { navController.navigate(ROUTE_ADMIN) }
                    )
                }
            }
        }

        // ---------------- FACED: grouped-by-family list ----------------

        composable(ROUTE_FACED_LIST) {
            RequireUser(currentUser, navController) { user ->
                ShellWithBottomNav(app, user, ROUTE_FACED_LIST, navController) {
                    val vm: FacedListViewModel = viewModel(factory = AppViewModelFactory(app))
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val scope = rememberCoroutineScope()
                    FacedListScreen(
                        viewModel = vm,
                        onFileNew = { navController.navigate(ROUTE_FACED_NEW) },
                        onOpenFamily = { headName, barangay -> navController.navigate("faced/family/${enc(headName)}/${enc(barangay)}") },
                        onExportCsv = {
                            scope.launch {
                                val fullRecords = app.facedRepository.getAllFullRecords()
                                val header = listOf(
                                    "Serial", "Head of Family", "Barangay", "Evacuation Center", "Disaster Event",
                                    "Status", "Civil Status", "Sex", "Birth Date", "Age", "Birth Place",
                                    "Occupation", "Religion", "Monthly Income", "ID Type", "ID Number",
                                    "Primary Contact", "Alt Contact", "Mother's Maiden Name",
                                    "Permanent Address", "4Ps Beneficiary", "Shelter Assessment", "Synced",
                                    "Household Members (Name - Relation - Age)", "Vulnerable Tags", "Assistance History"
                                )
                                val rows = fullRecords.map { full ->
                                    val r = full.record
                                    val membersStr = full.members.joinToString(" | ") { "${it.name} - ${it.relation} - ${it.age}" }
                                    val tagsStr = full.vulnerableTags.joinToString(" | ") { "${it.personName} (${it.vulnerabilityType})" }
                                    val assistStr = full.assistanceHistory.joinToString(" | ") { "${it.date}: ${it.itemName} x${it.qty} to ${it.receivingMember}" }
                                    
                                    listOf(
                                        r.serial, r.headName, r.barangay, r.evacCenterName, r.disasterEvent,
                                        r.status.name, r.civilStatus, r.sex, r.birthDate, r.age.toString(), r.birthPlace,
                                        r.occupation, r.religion, r.monthlyIncome.toString(), r.idType, r.idNumber,
                                        r.contactNumber, r.altContactNumber, r.mothersMaidenName,
                                        r.permanentAddress, if (r.is4Ps) "Yes" else "No", r.shelterDamage.name,
                                        if (r.isSynced) "Yes" else "No",
                                        membersStr, tagsStr, assistStr
                                    )
                                }
                                val uri = CsvExporter.export(context, "FACED_Records_${DateUtils.today()}.csv", header, rows)
                                context.startActivity(CsvExporter.shareIntent(context, uri))
                            }
                        }
                    )
                }
            }
        }

        composable(
            ROUTE_FACED_FAMILY,
            arguments = listOf(navArgument("headName") { type = NavType.StringType }, navArgument("barangay") { type = NavType.StringType })
        ) { backStack ->
            RequireUser(currentUser, navController) {
                val headName = dec(backStack.arguments?.getString("headName") ?: "")
                val barangay = dec(backStack.arguments?.getString("barangay") ?: "")
                val vm: FacedFamilyViewModel = viewModel(
                    key = "family-$headName-$barangay",
                    factory = AppViewModelFactory(app, familyHeadName = headName, familyBarangay = barangay)
                )
                FamilyRecordsScreen(
                    headName = headName, barangay = barangay, viewModel = vm,
                    onBack = { navController.popBackStack() },
                    onOpenFiling = { serial -> navController.navigate("faced/detail/$serial") }
                )
            }
        }

        composable(ROUTE_FACED_NEW) {
            RequireUser(currentUser, navController) { user ->
                val vm: FacedFormViewModel = viewModel(factory = AppViewModelFactory(app, currentUserId = user.id))
                val centers by app.evacCenterRepository.observeAll().collectAsState(initial = emptyList())
                LaunchedEffect(Unit) {
                    val count = app.database.facedRecordDao().count()
                    vm.initNewSerial(app.facedRepository.nextSerial(count))
                }
                FacedFormScreen(
                    viewModel = vm, centers = centers,
                    onCancel = { navController.popBackStack() },
                    onSaved = { serial -> navController.navigate("faced/detail/$serial") { popUpTo(ROUTE_FACED_LIST) } }
                )
            }
        }

        composable(
            ROUTE_FACED_NEW_FOR_FAMILY,
            arguments = listOf(navArgument("headName") { type = NavType.StringType }, navArgument("barangay") { type = NavType.StringType })
        ) { backStack ->
            RequireUser(currentUser, navController) { user ->
                val headName = dec(backStack.arguments?.getString("headName") ?: "")
                val barangay = dec(backStack.arguments?.getString("barangay") ?: "")
                val vm: FacedFormViewModel = viewModel(factory = AppViewModelFactory(app, currentUserId = user.id))
                val centers by app.evacCenterRepository.observeAll().collectAsState(initial = emptyList())
                LaunchedEffect(Unit) {
                    val count = app.database.facedRecordDao().count()
                    vm.prefillForExistingFamily(headName, barangay, app.facedRepository.nextSerial(count))
                }
                FacedFormScreen(
                    viewModel = vm, centers = centers,
                    onCancel = { navController.popBackStack() },
                    onSaved = { serial -> navController.navigate("faced/detail/$serial") { popUpTo(ROUTE_FACED_LIST) } }
                )
            }
        }

        composable(ROUTE_FACED_EDIT) { backStack ->
            RequireUser(currentUser, navController) { user ->
                val serial = backStack.arguments?.getString("serial") ?: ""
                val vm: FacedFormViewModel = viewModel(factory = AppViewModelFactory(app, currentUserId = user.id))
                val detailVm: FacedDetailViewModel = viewModel(
                    key = "edit-$serial",
                    factory = AppViewModelFactory(app, facedSerial = serial)
                )
                val full by detailVm.fullRecord.collectAsState()
                val centers by app.evacCenterRepository.observeAll().collectAsState(initial = emptyList())
                LaunchedEffect(full) { full?.let { vm.loadForEdit(it) } }
                FacedFormScreen(
                    viewModel = vm, centers = centers,
                    onCancel = { navController.popBackStack() },
                    onSaved = { navController.popBackStack() }
                )
            }
        }

        composable(ROUTE_FACED_DETAIL) { backStack ->
            RequireUser(currentUser, navController) { user ->
                val serial = backStack.arguments?.getString("serial") ?: ""
                val vm: FacedDetailViewModel = viewModel(
                    key = "detail-$serial",
                    factory = AppViewModelFactory(app, facedSerial = serial)
                )
                FacedDetailScreen(
                    viewModel = vm,
                    isAdmin = user.role == UserRole.ADMIN,
                    onBack = { navController.popBackStack() },
                    onEdit = { navController.navigate("faced/edit/$serial") }
                )
            }
        }

        // ---------------- Centers & Inventory (combined, Change #5) ----------------

        composable(ROUTE_CENTERS) {
            RequireUser(currentUser, navController) { user ->
                ShellWithBottomNav(app, user, ROUTE_CENTERS, navController) {
                    val evacVm: EvacViewModel = viewModel(factory = AppViewModelFactory(app))
                    val invVm: InventoryViewModel = viewModel(factory = AppViewModelFactory(app))
                    CentersAndInventoryScreen(evacVm, invVm, isAdmin = user.role == UserRole.ADMIN)
                }
            }
        }

        // ---------------- Reports (renamed from DROMIC, Change #6) ----------------

        composable(ROUTE_REPORTS) {
            RequireUser(currentUser, navController) { user ->
                ShellWithBottomNav(app, user, ROUTE_REPORTS, navController) {
                    val vm: ReportsViewModel = viewModel(factory = AppViewModelFactory(app))
                    ReportsScreen(vm)
                }
            }
        }

        composable(ROUTE_ADMIN) {
            RequireUser(currentUser, navController) { _ ->
                val vm: AdminViewModel = viewModel(factory = AppViewModelFactory(app))
                AdminScreen(vm, onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun RequireUser(user: UserEntity?, navController: NavHostController, content: @Composable (UserEntity) -> Unit) {
    if (user == null) {
        LaunchedEffect(Unit) { navController.navigate(ROUTE_LOGIN) { popUpTo(0) } }
    } else {
        content(user)
    }
}

@Composable
private fun ShellWithBottomNav(
    app: FacedApp,
    user: UserEntity,
    route: String,
    navController: NavHostController,
    content: @Composable () -> Unit
) {
    val pendingCount by app.facedRepository.observePendingSyncCount().collectAsState(initial = 0)
    val isOnline by SessionManager.isOnline.collectAsState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(isOnline, user) {
        if (isOnline && user.role != UserRole.GUEST) {
            app.syncManager.syncPending()
        }
    }

    AppScaffold(
        app = app,
        currentUser = user,
        currentRoute = route,
        pendingSyncCount = pendingCount,
        onNavigate = { dest ->
            navController.navigate(dest) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
        onSwitchAccount = {
            SessionManager.logout()
            navController.navigate(ROUTE_LOGIN) { popUpTo(0) }
        }
    ) { padding ->
        Column(androidx.compose.ui.Modifier.padding(padding).fillMaxSize()) {
            content()
        }
    }
}
