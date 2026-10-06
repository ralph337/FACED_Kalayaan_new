package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EventType { FORM_FILED, FORM_UPDATED, CHECK_IN, CHECK_OUT, ASSISTANCE_GIVEN }

/**
 * A timestamped audit/activity entry: a family checked in or out of a center, a FACED
 * form was filed/updated, or assistance was given. This is what the "FACED Form Report"
 * tab under Reports is built from, and it also doubles as the ledger used to detect and
 * re-home data entered in Offline Guest Mode once the staff member logs into their real
 * account (see SyncManager.syncGuestDataToUser).
 */
@Entity(tableName = "event_log")
data class EventLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val facedSerial: String,
    val headName: String,
    val eventType: EventType,
    val description: String,
    val timestamp: Long = System.currentTimeMillis(),
    val performedByUserId: Long = 0, // -1 = Offline Guest Mode
    val performedByName: String = "",
    val isSynced: Boolean = false
)
