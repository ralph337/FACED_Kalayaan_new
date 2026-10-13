package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface EvacCenterDao {
    @Query("SELECT * FROM evac_centers ORDER BY name ASC")
    fun observeAll(): Flow<List<EvacCenterEntity>>

    @Query("SELECT * FROM evac_centers WHERE isActive = 1 ORDER BY name ASC")
    fun observeActive(): Flow<List<EvacCenterEntity>>

    @Query("SELECT * FROM evac_centers WHERE isSynced = 0")
    suspend fun getUnsynced(): List<EvacCenterEntity>

    @Query("UPDATE evac_centers SET isSynced = 1 WHERE isSynced = 0")
    suspend fun markAllSynced()

    @Query("SELECT COUNT(*) FROM evac_centers")
    suspend fun count(): Int

    @Query("SELECT * FROM evac_centers WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): EvacCenterEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(center: EvacCenterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(centers: List<EvacCenterEntity>)

    @Update
    suspend fun update(center: EvacCenterEntity)

    @Query("DELETE FROM evac_centers WHERE id = :id")
    suspend fun hardDelete(id: Long)
}
