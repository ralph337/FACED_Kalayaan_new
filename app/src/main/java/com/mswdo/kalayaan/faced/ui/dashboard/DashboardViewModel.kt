package com.mswdo.kalayaan.faced.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
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
    val centers: List<CenterWithOccupancy> = emptyList(),
    val inventory: List<InventoryItemEntity> = emptyList(),
    val vulnerabilityBreakdown: List<Pair<String, Int>> = emptyList(),
    val evacueesPerBarangay: List<Pair<String, Int>> = emptyList(),
    val centersByBarangay: Map<String, List<CenterWithOccupancy>> = emptyMap()
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
        val breakdown = listOf(
            "Senior (60+)" to tags.count { it.vulnerabilityType.contains("Senior") },
            "PWD" to tags.count { it.vulnerabilityType.contains("PWD") },
            "Pregnant/Lactating" to tags.count { it.vulnerabilityType.contains("Pregnant") || it.vulnerabilityType.contains("Lactating") },
            "Child/Minor" to tags.count { it.vulnerabilityType.contains("Minor") || it.vulnerabilityType.contains("Child") },
            "Solo Parent" to tags.count { it.vulnerabilityType.contains("Solo") }
        )

        val inCenterRecords = records.filter { it.status.name == "IN_CENTER" }
        val barangays = listOf("San Juan", "Longos", "San Antonio")

        val perBarangay = barangays.map { b ->
            val count = inCenterRecords.filter { it.barangay.equals(b, ignoreCase = true) }.sumOf { it.presentMembersCount }
            b to count
        }

        val centerMap = barangays.associateWith { b ->
            centers.filter { it.center.location.equals(b, ignoreCase = true) }
        }

        DashboardUiState(
            totalFamilies = records.size,
            totalIndividuals = inCenterRecords.sumOf { it.presentMembersCount },
            vulnerableCount = tags.map { it.personName }.distinct().size,
            totalStock = inventory.sumOf { it.qty },
            lowStockItemCount = inventory.count { it.qty <= it.lowStockThreshold },
            centers = centers,
            inventory = inventory,
            vulnerabilityBreakdown = breakdown,
            evacueesPerBarangay = perBarangay,
            centersByBarangay = centerMap
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())
}
