package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.AssistanceRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssistanceRecordDao {
    @Query("SELECT * FROM assistance_records WHERE facedSerial = :serial ORDER BY date DESC")
    fun observeForRecord(serial: String): Flow<List<AssistanceRecordEntity>>

    @Query("SELECT * FROM assistance_records ORDER BY date DESC")
    fun observeAll(): Flow<List<AssistanceRecordEntity>>

    @Insert
    suspend fun insert(record: AssistanceRecordEntity): Long

    @Query("SELECT COUNT(*) FROM assistance_records WHERE isSynced = 0")
    fun observePendingSyncCount(): Flow<Int>

    @Query("UPDATE assistance_records SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllSynced()

    /** Re-homes every guest-authored assistance record to the real account and marks it synced. */
    @Query("UPDATE assistance_records SET recordedByUserId = :newUserId, isSynced = 1 WHERE recordedByUserId = :oldUserId")
    suspend fun reassignOwner(oldUserId: Long, newUserId: Long)
}
