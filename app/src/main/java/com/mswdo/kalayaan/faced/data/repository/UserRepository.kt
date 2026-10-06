package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.EventLogDao
import com.mswdo.kalayaan.faced.data.dao.UserDao
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.util.PasswordHasher
import kotlinx.coroutines.flow.Flow

class UserRepository(
    private val userDao: UserDao,
    private val eventLogDao: EventLogDao
) {
    fun observeAll(): Flow<List<UserEntity>> = userDao.observeAll()

    /** Returns the authenticated user, or null if credentials are invalid or account is deactivated. */
    suspend fun login(username: String, password: String): UserEntity? {
        val user = userDao.findActiveByUsername(username.trim()) ?: return null
        return if (PasswordHasher.verify(password, user.passwordHash)) user else null
    }

    suspend fun getOrCreateUser(username: String, fullName: String, avatarInitials: String): UserEntity {
        val existing = userDao.findByUsername(username)
        if (existing != null) return existing

        val role = if (username.lowercase().trim() == "ralphlax337@gmail.com") UserRole.ADMIN else UserRole.STAFF
        val title = if (role == UserRole.ADMIN) "Head MSWDO (Admin)" else "Disaster Response Staff"

        val newUser = UserEntity(
            username = username,
            passwordHash = "",
            fullName = fullName,
            title = title,
            role = role,
            avatarInitials = avatarInitials
        )
        val id = userDao.insert(newUser)
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = "",
                headName = fullName,
                eventType = EventType.FORM_UPDATED,
                description = "Registered new account: @$username ($title)",
                performedByUserId = id,
                performedByName = fullName
            )
        )
        return newUser.copy(id = id)
    }

    suspend fun createStaff(username: String, password: String, fullName: String, title: String, role: UserRole, avatarInitials: String) {
        val id = userDao.insert(
            UserEntity(
                username = username.trim(), passwordHash = PasswordHasher.hash(password),
                fullName = fullName.trim(), title = title.trim(), role = role,
                avatarInitials = avatarInitials.uppercase()
            )
        )
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = "",
                headName = fullName,
                eventType = EventType.FORM_UPDATED,
                description = "Created staff account: @$username ($title)",
                performedByUserId = SessionManager.currentUserId(),
                performedByName = SessionManager.currentUserName()
            )
        )
    }

    suspend fun setActive(id: Long, active: Boolean) {
        userDao.setActive(id, active)
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = "",
                headName = "",
                eventType = EventType.FORM_UPDATED,
                description = "Changed account ID #$id status to ${if (active) "ACTIVE" else "INACTIVE"}",
                performedByUserId = SessionManager.currentUserId(),
                performedByName = SessionManager.currentUserName()
            )
        )
    }

    suspend fun delete(id: Long) {
        userDao.delete(id)
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = "",
                headName = "",
                eventType = EventType.FORM_UPDATED,
                description = "Deleted staff account ID #$id",
                performedByUserId = SessionManager.currentUserId(),
                performedByName = SessionManager.currentUserName()
            )
        )
    }
}
