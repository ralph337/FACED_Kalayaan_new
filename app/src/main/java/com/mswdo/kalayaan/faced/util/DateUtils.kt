package com.mswdo.kalayaan.faced.util

import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtils {
    private val isoFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    /** Computes age in years from an ISO yyyy-MM-dd birth date string. */
    fun ageFromBirthDate(birthDateIso: String): Int = try {
        val bd = LocalDate.parse(birthDateIso, isoFormatter)
        Period.between(bd, LocalDate.now()).years
    } catch (e: Exception) {
        0
    }

    /** Categorizes an age into the roster buckets used on the official FACED form. */
    fun ageCategory(age: Int): String = when {
        age <= 2 -> "Infant (0-2)"
        age < 18 -> "Child/Minor"
        age >= 60 -> "Senior Citizen (60+)"
        else -> "Adult"
    }

    fun today(): String = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())

    fun formatTimestamp(epochMillis: Long): String =
        SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.US).format(java.util.Date(epochMillis))

    fun formatDisplay(iso: String): String = try {
        val bd = LocalDate.parse(iso, isoFormatter)
        bd.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
    } catch (e: Exception) {
        iso
    }
}
