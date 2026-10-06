package com.mswdo.kalayaan.faced.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EventLogDao {
    @Query("SELECT * FROM event_log ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<EventLogEntity>>

    @Query("SELECT * FROM event_log WHERE facedSerial = :serial ORDER BY timestamp DESC")
    fun observeForRecord(serial: String): Flow<List<EventLogEntity>>

    @Insert
    suspend fun insert(event: EventLogEntity): Long

    @Query("SELECT COUNT(*) FROM event_log WHERE performedByUserId = :userId")
    suspend fun countByUser(userId: Long): Int

    /** Re-homes every guest-authored event to the real account and marks it synced (used by the post-login sync popup). */
    @Query("UPDATE event_log SET performedByUserId = :newUserId, isSynced = 1 WHERE performedByUserId = :oldUserId")
    suspend fun reassignOwner(oldUserId: Long, newUserId: Long)
}
