package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Reserved id used to mark data entered while in Offline Guest Mode (no real account). */
const val GUEST_USER_ID = -1L

/**
 * Holds the currently logged-in user for the app process, and derives connectivity
 * straight from *how* the person entered the app: choosing "Continue Offline (Guest
 * Mode)" on the login screen means the whole session is offline and everything written
 * goes to the local database only; logging into a real account is treated as being
 * online, so the pending-sync queue can flush automatically/on demand.
 * Kept in-memory (resets each app launch) since this is a small, single-device-per-staff
 * deployment; back it with EncryptedSharedPreferences later if "stay logged in" is needed.
 */
object SessionManager {
    val GUEST_USER = UserEntity(
        id = GUEST_USER_ID, username = "guest", passwordHash = "",
        fullName = "Guest User", title = "Offline Guest Mode",
        role = UserRole.GUEST, isActive = true, avatarInitials = "GU"
    )

    private val _currentUser = MutableStateFlow<UserEntity?>(null)
    val currentUser: StateFlow<UserEntity?> = _currentUser.asStateFlow()

    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    fun login(user: UserEntity) {
        _currentUser.value = user
        _isOnline.value = true
    }

    fun loginGuest() {
        _currentUser.value = GUEST_USER
        _isOnline.value = false
    }

    fun logout() {
        _currentUser.value = null
        _isOnline.value = false
    }

    fun isAdmin(): Boolean = _currentUser.value?.role == UserRole.ADMIN
    fun isGuest(): Boolean = _currentUser.value?.role == UserRole.GUEST
    fun currentUserId(): Long = _currentUser.value?.id ?: GUEST_USER_ID
    fun currentUserName(): String = _currentUser.value?.fullName ?: "Guest User"

    var last2faTimeMillis: Long = 0L // Store this in DataStore or SharedPreferences in production
}
