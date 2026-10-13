package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.EvacCenterDao
import com.mswdo.kalayaan.faced.data.dao.EventLogDao
import com.mswdo.kalayaan.faced.data.dao.FacedRecordDao
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.data.entity.EventLogEntity
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.data.entity.FacedRecordEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class CenterWithOccupancy(
    val center: EvacCenterEntity,
    val occupied: Int
) {
    val percentFull: Int get() = if (center.capacity == 0) 0 else ((occupied * 100) / center.capacity).coerceAtMost(100)
}

class EvacCenterRepository(
    private val centerDao: EvacCenterDao,
    private val facedRecordDao: FacedRecordDao,
    private val eventLogDao: EventLogDao
) {
    fun observeAll(): Flow<List<EvacCenterEntity>> = centerDao.observeActive()

    /** Live occupancy = heads-of-family + household members currently IN_CENTER at that site. */
    fun observeWithOccupancy(): Flow<List<CenterWithOccupancy>> =
        combine(centerDao.observeActive(), facedRecordDao.observeAll()) { centers, records ->
            centers.map { center ->
                val familiesInside = records.filter {
                    it.evacCenterName == center.name && it.status == EvacuationStatus.IN_CENTER
                }
                // Occupancy now perfectly tracks exact checked-in individuals based on the return dialog.
                CenterWithOccupancy(center, familiesInside.sumOf { it.presentMembersCount })
            }
        }

    suspend fun save(center: EvacCenterEntity) {
        centerDao.insert(center.copy(isSynced = false))
        logEvent("Added new Evacuation Center: ${center.name}")
    }

    suspend fun update(center: EvacCenterEntity) {
        centerDao.update(center.copy(isSynced = false))
        logEvent("Updated Evacuation Center: ${center.name}")
    }

    suspend fun delete(center: EvacCenterEntity) {
        centerDao.hardDelete(center.id)
        logEvent("Deleted Evacuation Center: ${center.name}")
    }

    suspend fun recordsInCenter(centerName: String): List<FacedRecordEntity> =
        facedRecordDao.getInCenterByCenterName(centerName)

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
