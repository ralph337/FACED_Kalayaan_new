package com.mswdo.kalayaan.faced.data.repository

import com.mswdo.kalayaan.faced.data.dao.*
import com.mswdo.kalayaan.faced.data.entity.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

/** Everything needed to render a FACED card's full profile + roster + tags + assistance history. */
data class FacedFullRecord(
    val record: FacedRecordEntity,
    val members: List<HouseholdMemberEntity>,
    val vulnerableTags: List<VulnerableTagEntity>,
    val assistanceHistory: List<AssistanceRecordEntity>
) {
    val totalPersons: Int get() = 1 + members.size
}

sealed class SaveFacedResult {
    data class Success(val serial: String) : SaveFacedResult()
    data class DuplicateWarning(val existingSerial: String, val existingHeadName: String, val filingCount: Int) : SaveFacedResult()
}

class FacedRepository(
    private val facedDao: FacedRecordDao,
    private val memberDao: HouseholdMemberDao,
    private val tagDao: VulnerableTagDao,
    private val assistanceDao: AssistanceRecordDao,
    private val inventoryDao: InventoryDao,
    private val eventLogDao: EventLogDao
) {
    fun observeAll(): Flow<List<FacedRecordEntity>> = facedDao.observeAll()
    fun observePendingSyncCount(): Flow<Int> = facedDao.observePendingSyncCount()

    /** All filings on record for one family (grouped by head name + barangay), newest first. */
    fun observeForFamily(headName: String, barangay: String): Flow<List<FacedRecordEntity>> =
        facedDao.observeForFamily(headName, barangay)

    fun observeFullRecord(serial: String): Flow<FacedFullRecord?> =
        combine(
            facedDao.observeBySerial(serial),
            memberDao.observeForRecord(serial),
            tagDao.observeForRecord(serial),
            assistanceDao.observeForRecord(serial)
        ) { record, members, tags, history ->
            if (record == null) null else FacedFullRecord(record, members, tags, history)
        }

    fun nextSerial(existingCount: Int): String {
        val year = java.time.LocalDate.now().year
        return "KL-$year-${(existingCount + 1).toString().padStart(3, '0')}"
    }

    /**
     * Saves a FACED card. If [allowDuplicate] is false and a filing already exists for
     * this exact head name + barangay, returns [SaveFacedResult.DuplicateWarning] instead
     * of writing anything, so the UI can offer "Update that existing record" or
     * "File this as a new record for that family" (Change #4).
     */
    suspend fun trySave(
        record: FacedRecordEntity,
        members: List<HouseholdMemberEntity>,
        tags: List<VulnerableTagEntity>,
        allowDuplicate: Boolean
    ): SaveFacedResult {
        if (!allowDuplicate) {
            val dup = facedDao.findPossibleDuplicate(record.headName, record.barangay)
            if (dup != null && dup.serial != record.serial) {
                return SaveFacedResult.DuplicateWarning(
                    existingSerial = dup.serial,
                    existingHeadName = dup.headName,
                    filingCount = facedDao.countForFamily(dup.headName, dup.barangay)
                )
            }
        }
        val isUpdate = facedDao.getBySerial(record.serial) != null
        forceSave(record, members, tags)
        logEvent(record.serial, record.headName, if (isUpdate) EventType.FORM_UPDATED else EventType.FORM_FILED,
            if (isUpdate) "FACED form updated for ${record.headName} (${record.disasterEvent})"
            else "New FACED form filed for ${record.headName} at ${record.evacCenterName} (${record.disasterEvent})"
        )
        return SaveFacedResult.Success(record.serial)
    }

    suspend fun forceSave(
        record: FacedRecordEntity,
        members: List<HouseholdMemberEntity>,
        tags: List<VulnerableTagEntity>
    ) {
        // Local-first write: always succeeds immediately regardless of connectivity.
        // isSynced stays false until SyncManager "uploads" it.
        facedDao.insert(record.copy(isSynced = false, updatedAt = System.currentTimeMillis()))
        memberDao.deleteForRecord(record.serial)
        memberDao.insertAll(members.map { it.copy(facedSerial = record.serial) })
        tagDao.deleteForRecord(record.serial)
        tagDao.insertAll(tags.map { it.copy(facedSerial = record.serial) })
    }

    suspend fun updateStatus(serial: String, status: EvacuationStatus, presentCount: Int? = null) {
        val existing = facedDao.getBySerial(serial) ?: return
        val count = presentCount ?: if (status == EvacuationStatus.IN_CENTER) existing.presentMembersCount else 0
        facedDao.update(existing.copy(status = status, presentMembersCount = count, isSynced = false, updatedAt = System.currentTimeMillis()))
        logEvent(
            serial, existing.headName,
            if (status == EvacuationStatus.IN_CENTER) EventType.CHECK_IN else EventType.CHECK_OUT,
            if (status == EvacuationStatus.IN_CENTER) "${existing.headName}'s family checked in ($count members) to ${existing.evacCenterName}"
            else "${existing.headName}'s family left / returned home from ${existing.evacCenterName}"
        )
    }

    suspend fun returnNonConsumableItems(serial: String, headName: String, returnedItemNames: List<String>) {
        val history = assistanceDao.observeForRecord(serial).first()
        var totalItemsReturned = 0
        
        returnedItemNames.forEach { itemName ->
            // Find how many of this item were given to this family
            val totalQtyGiven = history.filter { it.itemName == itemName }.sumOf { it.qty }
            if (totalQtyGiven > 0) {
                val inventoryItem = inventoryDao.observeAll().first().find { it.name == itemName }
                if (inventoryItem != null) {
                    inventoryDao.restock(inventoryItem.id, totalQtyGiven)
                    totalItemsReturned += totalQtyGiven
                }
            }
        }
        
        if (totalItemsReturned > 0) {
            logEvent(
                serial, headName, EventType.ASSISTANCE_GIVEN,
                "$totalItemsReturned non-consumable item(s) (${returnedItemNames.joinToString(", ")}) returned to inventory stock."
            )
        }
    }

    suspend fun delete(record: FacedRecordEntity) = facedDao.delete(record)

    /**
     * Records a relief distribution against this FACED card and atomically deducts the
     * matching inventory item's stock. Returns false if stock was insufficient.
     */
    suspend fun recordAssistance(assistance: AssistanceRecordEntity, inventoryItemId: Long): Boolean {
        val deducted = inventoryDao.deduct(inventoryItemId, assistance.qty)
        if (deducted == 0) return false
        assistanceDao.insert(assistance.copy(isSynced = false, recordedByUserId = SessionManager.currentUserId()))
        logEvent(
            assistance.facedSerial, assistance.receivingMember, EventType.ASSISTANCE_GIVEN,
            "${assistance.itemName} x${assistance.qty} ${assistance.unit} given to ${assistance.receivingMember} (${assistance.disaster})"
        )
        return true
    }

    fun observeAllAssistance(): Flow<List<AssistanceRecordEntity>> = assistanceDao.observeAll()

    fun observeAllEvents(): Flow<List<EventLogEntity>> = eventLogDao.observeAll()

    suspend fun getAllFullRecords(): List<FacedFullRecord> {
        val allRecords = facedDao.observeAll().first()
        return allRecords.map { r ->
            FacedFullRecord(
                record = r,
                members = memberDao.observeForRecord(r.serial).first(),
                vulnerableTags = tagDao.observeForRecord(r.serial).first(),
                assistanceHistory = assistanceDao.observeForRecord(r.serial).first()
            )
        }
    }

    private suspend fun logEvent(serial: String, headName: String, type: EventType, description: String) {
        eventLogDao.insert(
            EventLogEntity(
                facedSerial = serial, headName = headName, eventType = type,
                description = description, performedByUserId = SessionManager.currentUserId(),
                performedByName = SessionManager.currentUserName()
            )
        )
    }
}
