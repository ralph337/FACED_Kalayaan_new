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

    @Query("SELECT COUNT(*) FROM evac_centers")
    suspend fun count(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(center: EvacCenterEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(centers: List<EvacCenterEntity>)

    @Update
    suspend fun update(center: EvacCenterEntity)

    @Delete
    suspend fun delete(center: EvacCenterEntity)
}
