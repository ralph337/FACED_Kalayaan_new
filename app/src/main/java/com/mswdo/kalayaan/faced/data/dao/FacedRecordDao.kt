package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.FacedRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FacedRecordDao {
    @Query("SELECT * FROM faced_records ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<FacedRecordEntity>>

    @Query("SELECT * FROM faced_records WHERE serial = :serial LIMIT 1")
    fun observeBySerial(serial: String): Flow<FacedRecordEntity?>

    @Query("SELECT * FROM faced_records WHERE serial = :serial LIMIT 1")
    suspend fun getBySerial(serial: String): FacedRecordEntity?

    @Query("""
        SELECT * FROM faced_records
        WHERE LOWER(headName) = LOWER(:headName) AND LOWER(barangay) = LOWER(:barangay)
        LIMIT 1
    """)
    suspend fun findPossibleDuplicate(headName: String, barangay: String): FacedRecordEntity?

    @Query("SELECT COUNT(*) FROM faced_records")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM faced_records WHERE isSynced = 0")
    fun observePendingSyncCount(): Flow<Int>

    @Query("SELECT * FROM faced_records WHERE isSynced = 0")
    suspend fun getUnsynced(): List<FacedRecordEntity>

    @Query("UPDATE faced_records SET isSynced = 1 WHERE serial = :serial")
    suspend fun markSynced(serial: String)

    @Query("SELECT * FROM faced_records WHERE evacCenterName = :centerName AND status = 'IN_CENTER'")
    suspend fun getInCenterByCenterName(centerName: String): List<FacedRecordEntity>

    @Query("SELECT * FROM faced_records WHERE LOWER(headName) = LOWER(:headName) AND LOWER(barangay) = LOWER(:barangay) ORDER BY createdAt DESC")
    fun observeForFamily(headName: String, barangay: String): Flow<List<FacedRecordEntity>>

    @Query("SELECT COUNT(*) FROM faced_records WHERE LOWER(headName) = LOWER(:headName) AND LOWER(barangay) = LOWER(:barangay)")
    suspend fun countForFamily(headName: String, barangay: String): Int

    @Query("SELECT COUNT(*) FROM faced_records WHERE filedByUserId = :userId")
    suspend fun countByFiledBy(userId: Long): Int

    /** Re-homes every guest-authored record to the real account and marks it synced (used by the post-login sync popup). */
    @Query("UPDATE faced_records SET filedByUserId = :newUserId, isSynced = 1 WHERE filedByUserId = :oldUserId")
    suspend fun reassignOwner(oldUserId: Long, newUserId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: FacedRecordEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<FacedRecordEntity>)

    @Update
    suspend fun update(record: FacedRecordEntity)

    @Delete
    suspend fun delete(record: FacedRecordEntity)
}
