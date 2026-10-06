package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.EvacCenterDao
import com.mswdo.kalayaan.faced.data.dao.FacedRecordDao
import com.mswdo.kalayaan.faced.data.entity.EvacCenterEntity
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
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
    private val facedRecordDao: FacedRecordDao
) {
    fun observeAll(): Flow<List<EvacCenterEntity>> = centerDao.observeAll()

    /** Live occupancy = heads-of-family + household members currently IN_CENTER at that site. */
    fun observeWithOccupancy(): Flow<List<CenterWithOccupancy>> =
        combine(centerDao.observeAll(), facedRecordDao.observeAll()) { centers, records ->
            centers.map { center ->
                val familiesInside = records.filter {
                    it.evacCenterName == center.name && it.status == EvacuationStatus.IN_CENTER
                }
                // Occupancy now perfectly tracks exact checked-in individuals based on the return dialog.
                CenterWithOccupancy(center, familiesInside.sumOf { it.presentMembersCount })
            }
        }

    suspend fun save(center: EvacCenterEntity) = centerDao.insert(center)
    suspend fun update(center: EvacCenterEntity) = centerDao.update(center)
    suspend fun delete(center: EvacCenterEntity) = centerDao.delete(center)

    suspend fun recordsInCenter(centerName: String): List<FacedRecordEntity> =
        facedRecordDao.getInCenterByCenterName(centerName)
}
