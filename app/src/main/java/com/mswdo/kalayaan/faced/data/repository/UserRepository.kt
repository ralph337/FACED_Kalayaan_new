package com.mswdo.kalayaan.faced.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.mswdo.kalayaan.faced.data.dao.EventLogDao
import com.mswdo.kalayaan.faced.data.dao.UserDao
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.data.entity.UserEntity
import com.mswdo.kalayaan.faced.data.entity.UserRole
import com.mswdo.kalayaan.faced.util.PasswordHasher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val userDao: UserDao,
    private val eventLogDao: EventLogDao
) {
    fun observeAll(): Flow<List<UserEntity>> = userDao.observeAll()

    suspend fun findUserByUsername(username: String): UserEntity? = userDao.findByUsername(username.trim())

    private suspend fun syncUserToFirebase(user: UserEntity) {
        try {
            val firebaseDb = FirebaseDatabase.getInstance("https://facedform-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
            val map = mapOf(
                "id" to user.id,
                "username" to user.username,
                "fullName" to user.fullName,
                "title" to user.title,
                "role" to user.role.name,
                "isActive" to user.isActive,
                "active" to user.isActive, // Write both keys to guarantee Firebase JSON compatibility across devices!
                "avatarInitials" to user.avatarInitials
            )
            firebaseDb.child("users").child(user.username.replace(".", "_")).setValue(map).await()
        } catch (e: Exception) {
            Log.e("UserRepository", "Failed to sync user to Firebase", e)
        }
    }

    /** Returns the authenticated user, or null if credentials are invalid. Throws exception if inactive. */
    suspend fun login(username: String, password: String): UserEntity? {
        val user = userDao.findByUsername(username.trim()) ?: return null
        if (user.role != UserRole.ADMIN && !user.isActive) {
            throw Exception("Your account is pending admin approval. Please wait for an administrator to activate your account.")
        }
        return if (PasswordHasher.verify(password, user.passwordHash)) user else null
    }

    suspend fun getOrCreateUser(username: String, fullName: String, avatarInitials: String): UserEntity {
        val role = if (username.lowercase().trim() == "ralphlax337@gmail.com") UserRole.ADMIN else UserRole.STAFF
        val existing = userDao.findByUsername(username)
        if (existing != null) {
            if (role == UserRole.ADMIN || existing.role == UserRole.ADMIN) {
                if (!existing.isActive || existing.role != UserRole.ADMIN) {
                    val activeAdmin = existing.copy(isActive = true, role = UserRole.ADMIN)
                    userDao.update(activeAdmin)
                    return activeAdmin
                }
            }
            return existing
        }

        val title = if (role == UserRole.ADMIN) "Head MSWDO (Admin)" else "Disaster Response Staff"
        val isActive = role == UserRole.ADMIN // Admins auto-approve, staff pend approval

        val newUser = UserEntity(
            username = username,
            passwordHash = "",
            fullName = fullName,
            title = title,
            role = role,
            isActive = isActive,
            avatarInitials = avatarInitials
        )
        val id = userDao.insert(newUser)
        val createdUser = newUser.copy(id = id)
        syncUserToFirebase(createdUser)

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
        return createdUser
    }

    suspend fun createStaff(username: String, password: String, fullName: String, title: String, role: UserRole, avatarInitials: String) {
        val newUser = UserEntity(
            username = username.trim(), passwordHash = PasswordHasher.hash(password),
            fullName = fullName.trim(), title = title.trim(), role = role,
            isActive = true, // Admin created accounts are active immediately
            avatarInitials = avatarInitials.uppercase()
        )
        val id = userDao.insert(newUser)
        val createdUser = newUser.copy(id = id)
        syncUserToFirebase(createdUser)

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
        val user = userDao.observeAll().first().find { it.id == id }
        if (user != null) {
            val updated = user.copy(isActive = active)
            userDao.update(updated)
            syncUserToFirebase(updated)

            eventLogDao.insert(
                EventLogEntity(
                    facedSerial = "",
                    headName = user.fullName,
                    eventType = EventType.FORM_UPDATED,
                    description = "Admin ${if (active) "APPROVED/ACTIVATED" else "DEACTIVATED"} staff account: @${user.username}",
                    performedByUserId = SessionManager.currentUserId(),
                    performedByName = SessionManager.currentUserName()
                )
            )
        } else {
            userDao.setActive(id, active)
        }
    }

    suspend fun delete(id: Long) {
        val user = userDao.observeAll().first().find { it.id == id }
        if (user != null) {
            try {
                val firebaseDb = FirebaseDatabase.getInstance("https://facedform-default-rtdb.asia-southeast1.firebasedatabase.app/").reference
                firebaseDb.child("users").child(user.username.replace(".", "_")).removeValue().await()
            } catch (e: Exception) {
                Log.e("UserRepository", "Failed to delete remote user from Firebase", e)
            }
        }
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
