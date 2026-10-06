package com.mswdo.kalayaan.faced.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.repository.CenterWithOccupancy
import com.mswdo.kalayaan.faced.data.repository.EvacCenterRepository
import com.mswdo.kalayaan.faced.data.repository.FacedRepository
import com.mswdo.kalayaan.faced.data.repository.InventoryRepository
import kotlinx.coroutines.flow.*

data class DashboardUiState(
    val totalFamilies: Int = 0,
    val totalIndividuals: Int = 0,
    val vulnerableCount: Int = 0,
    val totalStock: Int = 0,
    val lowStockItemCount: Int = 0,
    val centers: List<CenterWithOccupancy> = emptyList()
)

class DashboardViewModel(
    private val facedRepository: FacedRepository,
    private val evacCenterRepository: EvacCenterRepository,
    private val inventoryRepository: InventoryRepository,
    private val database: com.mswdo.kalayaan.faced.data.AppDatabase
) : ViewModel() {

    val uiState: StateFlow<DashboardUiState> = combine(
        facedRepository.observeAll(),
        database.vulnerableTagDao().observeAll(),
        inventoryRepository.observeAll(),
        evacCenterRepository.observeWithOccupancy()
    ) { records, tags, inventory, centers ->
        DashboardUiState(
            totalFamilies = records.size,
            totalIndividuals = records.filter { it.status.name == "IN_CENTER" }.sumOf { it.presentMembersCount },
            vulnerableCount = tags.map { it.personName }.distinct().size,
            totalStock = inventory.sumOf { it.qty },
            lowStockItemCount = inventory.count { it.qty <= it.lowStockThreshold },
            centers = centers
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
