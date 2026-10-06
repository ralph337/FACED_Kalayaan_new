package com.mswdo.kalayaan.faced.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.mswdo.kalayaan.faced.FacedApp
import com.mswdo.kalayaan.faced.ui.admin.AdminViewModel
import com.mswdo.kalayaan.faced.ui.dashboard.DashboardViewModel
import com.mswdo.kalayaan.faced.ui.reports.ReportsViewModel
import com.mswdo.kalayaan.faced.ui.evac.EvacViewModel
import com.mswdo.kalayaan.faced.ui.faced.FacedDetailViewModel
import com.mswdo.kalayaan.faced.ui.faced.FacedFamilyViewModel
import com.mswdo.kalayaan.faced.ui.faced.FacedFormViewModel
import com.mswdo.kalayaan.faced.ui.faced.FacedListViewModel
import com.mswdo.kalayaan.faced.ui.inventory.InventoryViewModel
import com.mswdo.kalayaan.faced.ui.login.LoginViewModel

/** Simple manual DI factory - keeps the project free of extra DI framework dependencies. */
class AppViewModelFactory(
    private val app: FacedApp,
    private val currentUserId: Long = 0,
    private val facedSerial: String = "",
    private val familyHeadName: String = "",
    private val familyBarangay: String = ""
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) ->
                LoginViewModel(app.userRepository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(app.facedRepository, app.evacCenterRepository, app.inventoryRepository, app.database) as T
            modelClass.isAssignableFrom(FacedListViewModel::class.java) ->
                FacedListViewModel(app.facedRepository) as T
            modelClass.isAssignableFrom(FacedFamilyViewModel::class.java) ->
                FacedFamilyViewModel(app.facedRepository, familyHeadName, familyBarangay) as T
            modelClass.isAssignableFrom(FacedFormViewModel::class.java) ->
                FacedFormViewModel(app.facedRepository, currentUserId) as T
            modelClass.isAssignableFrom(FacedDetailViewModel::class.java) ->
                FacedDetailViewModel(facedSerial, app.facedRepository, app.inventoryRepository) as T
            modelClass.isAssignableFrom(EvacViewModel::class.java) ->
                EvacViewModel(app.evacCenterRepository) as T
            modelClass.isAssignableFrom(InventoryViewModel::class.java) ->
                InventoryViewModel(app.inventoryRepository, app.facedRepository) as T
            modelClass.isAssignableFrom(ReportsViewModel::class.java) ->
                ReportsViewModel(app.facedRepository, app.evacCenterRepository, app.inventoryRepository, app.database) as T
            modelClass.isAssignableFrom(AdminViewModel::class.java) ->
                AdminViewModel(app.userRepository, app.facedRepository) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
