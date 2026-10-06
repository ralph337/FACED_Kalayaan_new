package com.mswdo.kalayaan.faced.data.repository

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Represents connectivity for the sync workflow. In this offline-first prototype
 * it is toggled manually via the "Simulate Offline Outage / Restore Network" control
 * in the top bar, mirroring the HTML prototype's demo switch. Swap the backing field
 * for a real ConnectivityManager.NetworkCallback listener to drive this automatically
 * from actual device connectivity once wired to a live backend.
 */
object NetworkStateSimulator {
    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun setOnline(online: Boolean) { _isOnline.value = online }
    fun toggle() { _isOnline.value = !_isOnline.value }
}
