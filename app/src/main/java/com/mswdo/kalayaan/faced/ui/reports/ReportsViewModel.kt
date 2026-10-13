package com.mswdo.kalayaan.faced.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.data.entity.FacedRecordEntity
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import com.mswdo.kalayaan.faced.data.repository.*
import kotlinx.coroutines.flow.*

data class DisasterSummaryUiState(
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

/**
 * Change #6: "DROMIC" (bottom nav) is now "Reports", with two report types:
 *  1. FACED Form Report — a timestamped activity log (check-ins/outs, forms filed or
 *     updated, assistance given), from EventLogEntity.
 *  2. Disaster Summary Report — the original DROMIC-style live summary + CSV export.
 */
class ReportsViewModel(
    private val facedRepo: FacedRepository,
    private val evacRepo: EvacCenterRepository,
    private val inventoryRepo: InventoryRepository,
    private val database: com.mswdo.kalayaan.faced.data.AppDatabase
) : ViewModel() {

    private val _eventQuery = MutableStateFlow("")
    val eventQuery: StateFlow<String> = _eventQuery.asStateFlow()

    val events: StateFlow<List<EventLogEntity>> = combine(
        facedRepo.observeAllEvents(),
        _eventQuery
    ) { allEvents, q ->
        val facedOnly = allEvents.filter { it.facedSerial.isNotBlank() }
        if (q.isBlank()) facedOnly else facedOnly.filter {
            it.headName.contains(q, ignoreCase = true) ||
            it.facedSerial.contains(q, ignoreCase = true) ||
            it.description.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onEventQueryChange(q: String) { _eventQuery.value = q }

    suspend fun getFullRecords(): List<FacedFullRecord> = facedRepo.getAllFullRecords()

    val disasterSummary: StateFlow<DisasterSummaryUiState> = combine(
        facedRepo.observeAll(),
        database.vulnerableTagDao().observeAll(),
        evacRepo.observeWithOccupancy(),
        inventoryRepo.observeAll()
    ) { records, tags, centers, inventory ->
        DisasterSummaryUiState(
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
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisasterSummaryUiState())
}
