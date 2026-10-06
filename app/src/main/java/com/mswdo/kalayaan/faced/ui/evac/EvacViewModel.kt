package com.mswdo.kalayaan.faced.ui.evac

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.repository.CenterWithOccupancy
import com.mswdo.kalayaan.faced.data.repository.EvacCenterRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class EvacViewModel(private val repo: EvacCenterRepository) : ViewModel() {
    val centers: StateFlow<List<CenterWithOccupancy>> =
        repo.observeWithOccupancy().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun save(center: EvacCenterEntity) = viewModelScope.launch { repo.save(center) }
    fun update(center: EvacCenterEntity) = viewModelScope.launch { repo.update(center) }
    fun delete(center: EvacCenterEntity) = viewModelScope.launch { repo.delete(center) }
}
