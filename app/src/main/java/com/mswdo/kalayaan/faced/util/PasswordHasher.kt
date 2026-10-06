package com.mswdo.kalayaan.faced.util

import java.security.MessageDigest

/**
 * Simple local password hashing for the offline account store (Non-Functional
 * Requirement: Security). For a production rollout this should be replaced with
 * a salted hash (e.g. BCrypt) once a real backend/auth service is wired up.
 */
object PasswordHasher {
    fun hash(raw: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(raw.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it.toInt() and 0xFF) }
    }

    fun verify(raw: String, hash: String): Boolean = hash(raw) == hash
}
