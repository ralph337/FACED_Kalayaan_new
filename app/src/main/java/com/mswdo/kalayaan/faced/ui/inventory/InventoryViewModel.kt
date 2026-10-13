package com.mswdo.kalayaan.faced.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.AssistanceRecordEntity
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import com.mswdo.kalayaan.faced.data.repository.FacedRepository
import com.mswdo.kalayaan.faced.data.repository.InventoryRepository
import com.mswdo.kalayaan.faced.data.repository.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repo: InventoryRepository,
    private val facedRepo: FacedRepository,
    private val syncManager: SyncManager
) : ViewModel() {
    private val _query = MutableStateFlow("")
    val query = _query.asStateFlow()

    val items: StateFlow<List<InventoryItemEntity>> = combine(
        repo.observeAll(), _query
    ) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.name.contains(q, ignoreCase = true) || it.code.contains(q, ignoreCase = true) || it.category.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distributionLog: StateFlow<List<AssistanceRecordEntity>> = combine(
        facedRepo.observeAllAssistance(), _query
    ) { list, q ->
        if (q.isBlank()) list else list.filter {
            it.itemName.contains(q, ignoreCase = true) || it.receivingMember.contains(q, ignoreCase = true) || it.facedSerial.contains(q, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(q: String) { _query.value = q }

    fun save(item: InventoryItemEntity) = viewModelScope.launch {
        repo.save(item)
        syncManager.syncPending()
    }
    fun update(item: InventoryItemEntity) = viewModelScope.launch {
        repo.update(item)
        syncManager.syncPending()
    }
    fun delete(item: InventoryItemEntity) = viewModelScope.launch {
        repo.delete(item)
        syncManager.deleteInventoryItemRemote(item)
        syncManager.syncPending()
    }
}
