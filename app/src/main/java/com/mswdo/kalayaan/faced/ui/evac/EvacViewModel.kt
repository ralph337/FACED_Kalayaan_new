package com.mswdo.kalayaan.faced.ui.evac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.repository.CenterWithOccupancy
import com.mswdo.kalayaan.faced.data.repository.EvacCenterRepository
import com.mswdo.kalayaan.faced.data.repository.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EvacViewModel(
    private val repo: EvacCenterRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    private val _barangayFilter = MutableStateFlow<String?>(null)
    val barangayFilter = _barangayFilter.asStateFlow()

    val centers: StateFlow<List<CenterWithOccupancy>> = combine(
        repo.observeWithOccupancy(), _query, _barangayFilter
    ) { list, q, b ->
        var filtered = list
        if (!b.isNullOrBlank()) {
            filtered = filtered.filter { it.center.location.equals(b, ignoreCase = true) }
        }
        if (q.isNotBlank()) {
            filtered = filtered.filter {
                it.center.name.contains(q, ignoreCase = true) || it.center.location.contains(q, ignoreCase = true)
            }
        }
        filtered
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }
    fun onBarangayFilterChange(b: String?) { _barangayFilter.value = b }

    fun save(center: EvacCenterEntity) = viewModelScope.launch {
        repo.save(center)
        syncManager.syncPending()
    }
    fun update(center: EvacCenterEntity) = viewModelScope.launch {
        repo.update(center)
        syncManager.syncPending()
    }
    fun delete(center: EvacCenterEntity) = viewModelScope.launch {
        repo.delete(center)
        syncManager.deleteEvacCenterRemote(center)
        syncManager.syncPending()
    }
}
