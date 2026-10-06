package com.mswdo.kalayaan.faced.ui.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.AssistanceRecordEntity
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import com.mswdo.kalayaan.faced.data.repository.FacedRepository
import com.mswdo.kalayaan.faced.data.repository.InventoryRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class InventoryViewModel(
    private val repo: InventoryRepository,
    private val facedRepo: FacedRepository
) : ViewModel() {
    val items: StateFlow<List<InventoryItemEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val distributionLog: StateFlow<List<AssistanceRecordEntity>> =
        facedRepo.observeAllAssistance().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(item: InventoryItemEntity) = viewModelScope.launch { repo.save(item) }
    fun update(item: InventoryItemEntity) = viewModelScope.launch { repo.update(item) }
    fun delete(item: InventoryItemEntity) = viewModelScope.launch { repo.delete(item) }
}
