package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.EventLogDao
import com.mswdo.kalayaan.faced.data.dao.InventoryDao
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.data.entity.InventoryItemEntity
import kotlinx.coroutines.flow.Flow

class InventoryRepository(
    private val inventoryDao: InventoryDao,
    private val eventLogDao: EventLogDao
) {
    fun observeAll(): Flow<List<InventoryItemEntity>> = inventoryDao.observeActive()
    fun observeTotalStock(): Flow<Int> = inventoryDao.observeTotalStock()

    suspend fun save(item: InventoryItemEntity) {
        inventoryDao.insert(item.copy(isSynced = false))
        logEvent("Added new inventory item: ${item.name} (${item.code})")
    }

    suspend fun update(item: InventoryItemEntity) {
        inventoryDao.update(item.copy(isSynced = false))
        logEvent("Updated inventory item: ${item.name} (${item.code})")
    }

    suspend fun delete(item: InventoryItemEntity) {
        inventoryDao.hardDelete(item.id)
        logEvent("Deleted inventory item: ${item.name} (${item.code})")
    }

    /** Returns true if stock was sufficient and deducted. */
    suspend fun deduct(itemId: Long, amount: Int): Boolean = inventoryDao.deduct(itemId, amount) > 0

    suspend fun restock(itemId: Long, amount: Int) = inventoryDao.restock(itemId, amount)

    private suspend fun logEvent(action: String) {
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = "",
                headName = "SYSTEM",
                eventType = EventType.FORM_UPDATED,
                description = action,
                performedByUserId = SessionManager.currentUserId(),
                performedByName = SessionManager.currentUserName()
            )
        )
    }
}
