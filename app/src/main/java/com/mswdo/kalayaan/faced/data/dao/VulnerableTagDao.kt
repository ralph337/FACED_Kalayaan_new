package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.VulnerableTagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface VulnerableTagDao {
    @Query("SELECT * FROM vulnerable_tags WHERE facedSerial = :serial")
    fun observeForRecord(serial: String): Flow<List<VulnerableTagEntity>>

    @Query("SELECT * FROM vulnerable_tags WHERE facedSerial = :serial")
    suspend fun getForRecord(serial: String): List<VulnerableTagEntity>

    @Query("SELECT * FROM vulnerable_tags")
    fun observeAll(): Flow<List<VulnerableTagEntity>>

    @Query("SELECT COUNT(*) FROM vulnerable_tags")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<VulnerableTagEntity>)

    @Query("DELETE FROM vulnerable_tags WHERE facedSerial = :serial")
    suspend fun deleteForRecord(serial: String)
}
