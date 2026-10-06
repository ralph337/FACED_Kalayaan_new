package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.InventoryDao
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

class InventoryRepository(private val inventoryDao: InventoryDao) {
    fun observeAll(): Flow<List<InventoryItemEntity>> = inventoryDao.observeAll()
    fun observeTotalStock(): Flow<Int> = inventoryDao.observeTotalStock()

    suspend fun save(item: InventoryItemEntity) = inventoryDao.insert(item)
    suspend fun update(item: InventoryItemEntity) = inventoryDao.update(item)
    suspend fun delete(item: InventoryItemEntity) = inventoryDao.delete(item)

    /** Returns true if stock was sufficient and deducted. */
    suspend fun deduct(itemId: Long, amount: Int): Boolean = inventoryDao.deduct(itemId, amount) > 0
}
