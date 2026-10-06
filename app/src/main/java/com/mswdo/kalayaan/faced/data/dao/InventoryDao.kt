package com.mswdo.kalayaan.faced.data.dao

import androidx.room.*
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items ORDER BY name ASC")
    fun observeAll(): Flow<List<InventoryItemEntity>>

    @Query("SELECT * FROM inventory_items WHERE id = :id")
    suspend fun getById(id: Long): InventoryItemEntity?

    @Query("SELECT COUNT(*) FROM inventory_items")
    suspend fun count(): Int

    @Query("SELECT COALESCE(SUM(qty), 0) FROM inventory_items")
    fun observeTotalStock(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: InventoryItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<InventoryItemEntity>)

    @Update
    suspend fun update(item: InventoryItemEntity)

    @Delete
    suspend fun delete(item: InventoryItemEntity)

    @Query("UPDATE inventory_items SET qty = qty - :amount WHERE id = :id AND qty >= :amount")
    suspend fun deduct(id: Long, amount: Int): Int
}
