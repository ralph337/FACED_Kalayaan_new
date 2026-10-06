package com.mswdo.kalayaan.faced.ui.dromic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.data.entity.FacedRecordEntity
import com.mswdo.kalayaan.faced.data.entity.HouseholdMemberEntity
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import com.mswdo.kalayaan.faced.data.repository.*
import kotlinx.coroutines.flow.*

data class DromicUiState(
    val disasterSummaryTitle: String = "Disaster Operations - Kalayaan",
    val totalFamilies: Int = 0,
    val totalIndividuals: Int = 0,
    val vulnerableTotal: Int = 0,
    val seniorCount: Int = 0,
    val pwdCount: Int = 0,
    val pregnantLactatingCount: Int = 0,
    val minorCount: Int = 0,
    val centers: List<CenterWithOccupancy> = emptyList(),
    val inventory: List<InventoryItemEntity> = emptyList(),
    val records: List<FacedRecordEntity> = emptyList()
)

class DromicViewModel(
    private val facedRepo: FacedRepository,
    private val evacRepo: EvacCenterRepository,
    private val inventoryRepo: InventoryRepository,
    private val database: com.mswdo.kalayaan.faced.data.AppDatabase
) : ViewModel() {

    val uiState: StateFlow<DromicUiState> = combine(
        facedRepo.observeAll(),
        database.vulnerableTagDao().observeAll(),
        evacRepo.observeWithOccupancy(),
        inventoryRepo.observeAll()
    ) { records, tags, centers, inventory ->
        DromicUiState(
            totalFamilies = records.size,
            totalIndividuals = records.filter { it.status == EvacuationStatus.IN_CENTER }.size,
            vulnerableTotal = tags.map { it.personName }.distinct().size,
            seniorCount = tags.count { it.vulnerabilityType.contains("Senior") },
            pwdCount = tags.count { it.vulnerabilityType.contains("PWD") },
            pregnantLactatingCount = tags.count { it.vulnerabilityType.contains("Pregnant") || it.vulnerabilityType.contains("Lactating") },
            minorCount = tags.count { it.vulnerabilityType.contains("Minor") },
            centers = centers,
            inventory = inventory,
            records = records
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DromicUiState())
}
