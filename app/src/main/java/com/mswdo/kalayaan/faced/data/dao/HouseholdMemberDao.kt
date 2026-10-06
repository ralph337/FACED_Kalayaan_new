package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.HouseholdMemberEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdMemberDao {
    @Query("SELECT * FROM household_members WHERE facedSerial = :serial")
    fun observeForRecord(serial: String): Flow<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM household_members WHERE facedSerial = :serial")
    suspend fun getForRecord(serial: String): List<HouseholdMemberEntity>

    @Query("SELECT COUNT(*) FROM household_members WHERE facedSerial = :serial")
    suspend fun countForRecord(serial: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<HouseholdMemberEntity>)

    @Query("DELETE FROM household_members WHERE facedSerial = :serial")
    suspend fun deleteForRecord(serial: String)
}
