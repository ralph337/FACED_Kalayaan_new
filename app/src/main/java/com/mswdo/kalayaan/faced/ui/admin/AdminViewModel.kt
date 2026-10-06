package com.mswdo.kalayaan.faced.ui.admin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.data.repository.FacedRepository
import com.mswdo.kalayaan.faced.data.repository.UserRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repo: UserRepository,
    private val facedRepo: FacedRepository
) : ViewModel() {
    val users: StateFlow<List<UserEntity>> =
        repo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val auditLogs: StateFlow<List<EventLogEntity>> =
        facedRepo.observeAllEvents().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createStaff(username: String, password: String, fullName: String, title: String, role: UserRole) {
        viewModelScope.launch {
            val initials = fullName.trim().split(" ").filter { it.isNotBlank() }.take(2).joinToString("") { it.first().uppercase() }
            repo.createStaff(username, password, fullName, title, role, initials.ifBlank { "ST" })
        }
    }

    fun setActive(id: Long, active: Boolean) = viewModelScope.launch { repo.setActive(id, active) }
    fun delete(id: Long) = viewModelScope.launch { repo.delete(id) }
}
