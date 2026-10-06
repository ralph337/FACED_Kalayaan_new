package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole { ADMIN, STAFF, GUEST }

/**
 * MSWDO / LGU staff account.
 * Only the Admin (Head MSWDO) can create or deactivate accounts (Functional Req. #4/#5).
 * Passwords are stored as SHA-256 hashes (see util/PasswordHasher.kt) - never plain text.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val username: String,
    val passwordHash: String,
    val fullName: String,
    val title: String,
    val role: UserRole,
    val isActive: Boolean = true,
    val avatarInitials: String
)
