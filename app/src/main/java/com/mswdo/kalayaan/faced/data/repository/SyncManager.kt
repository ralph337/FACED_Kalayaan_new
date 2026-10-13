package com.mswdo.kalayaan.faced.data.repository

import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.mswdo.kalayaan.faced.data.dao.*
import com.mswdo.kalayaan.faced.data.entity.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class SyncManager(
    private val facedDao: FacedRecordDao,
    private val memberDao: HouseholdMemberDao,
    private val tagDao: VulnerableTagDao,
    private val assistanceDao: AssistanceRecordDao,
    private val eventLogDao: EventLogDao,
    private val userDao: UserDao,
    private val evacCenterDao: EvacCenterDao,
    private val inventoryDao: InventoryDao
) {
    // If google-services.json is missing the DB URL, FirebaseDatabase.getInstance() defaults to https://<project_id>.firebaseio.com.
    // If your DB is in another region, you may need to pass the URL: FirebaseDatabase.getInstance("https://YOUR-DB-URL.firebasedatabase.app").reference
    private val firebaseDb = FirebaseDatabase.getInstance("https://facedform-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    suspend fun fetchRemoteData() {
        if (!SessionManager.isOnline.value) return

        try {
            val snapshot = firebaseDb.child("faced_records").get().await()
            if (snapshot.exists()) {
                val newRecords = mutableListOf<FacedRecordEntity>()
                val newMembers = mutableListOf<HouseholdMemberEntity>()
                val newTags = mutableListOf<VulnerableTagEntity>()

                snapshot.children.forEach { child ->
                    try {
                        val recordMap = child.value as? Map<*, *> ?: return@forEach
                        val serial = recordMap["serial"] as? String ?: return@forEach
                        
                        // Check if we already have it to avoid overwriting local edits
                        if (facedDao.getBySerial(serial) == null) {
                            val entity = FacedRecordEntity(
                                serial = serial,
                                headName = recordMap["headName"] as? String ?: "",
                                barangay = recordMap["barangay"] as? String ?: "",
                                evacCenterName = recordMap["evacCenterName"] as? String ?: "",
                                disasterEvent = recordMap["disasterEvent"] as? String ?: "",
                                stormName = recordMap["stormName"] as? String ?: "",
                                civilStatus = recordMap["civilStatus"] as? String ?: "",
                                sex = recordMap["sex"] as? String ?: "",
                                birthDate = recordMap["birthDate"] as? String ?: "",
                                age = (recordMap["age"] as? Long)?.toInt() ?: 0,
                                birthPlace = recordMap["birthPlace"] as? String ?: "",
                                occupation = recordMap["occupation"] as? String ?: "",
                                religion = recordMap["religion"] as? String ?: "",
                                monthlyIncome = recordMap["monthlyIncome"]?.toString() ?: "No Income",
                                idType = recordMap["idType"] as? String ?: "",
                                idNumber = recordMap["idNumber"] as? String ?: "",
                                contactNumber = recordMap["contactNumber"] as? String ?: "",
                                altContactNumber = recordMap["altContactNumber"] as? String ?: "",
                                mothersMaidenName = recordMap["mothersMaidenName"] as? String ?: "",
                                permanentAddress = recordMap["permanentAddress"] as? String ?: "",
                                is4Ps = recordMap["is4Ps"] as? Boolean ?: false,
                                shelterDamage = ShelterDamage.valueOf(recordMap["shelterDamage"] as? String ?: "NONE"),
                                status = EvacuationStatus.valueOf(recordMap["status"] as? String ?: "IN_CENTER"),
                                presentMembersCount = (recordMap["presentMembersCount"] as? Long)?.toInt() ?: 1,
                                isSynced = true, // It came from remote, so it's synced
                                createdAt = recordMap["createdAt"] as? Long ?: System.currentTimeMillis(),
                                updatedAt = recordMap["updatedAt"] as? Long ?: System.currentTimeMillis(),
                                filedByUserId = recordMap["filedByUserId"] as? Long ?: 0L
                            )
                            newRecords.add(entity)

                            // Read members if they exist inside the Firebase node
                            val membersList = recordMap["members"] as? List<Map<String, Any>>
                            membersList?.forEach { m ->
                                newMembers.add(
                                    HouseholdMemberEntity(
                                        facedSerial = serial,
                                        name = m["name"] as? String ?: "",
                                        relation = m["relation"] as? String ?: "",
                                        sex = m["sex"] as? String ?: "",
                                        birthDate = m["birthDate"] as? String ?: "",
                                        age = (m["age"] as? Long)?.toInt() ?: 0,
                                        ageCategory = m["ageCategory"] as? String ?: "",
                                        education = m["education"] as? String ?: "",
                                        occupation = m["occupation"] as? String ?: "",
                                        remarks = m["remarks"] as? String ?: ""
                                    )
                                )
                            }

                            // Read tags if they exist inside the Firebase node
                            val tagsList = recordMap["vulnerableTags"] as? List<Map<String, Any>>
                            tagsList?.forEach { t ->
                                newTags.add(
                                    VulnerableTagEntity(
                                        facedSerial = serial,
                                        personName = t["personName"] as? String ?: "",
                                        vulnerabilityType = t["vulnerabilityType"] as? String ?: ""
                                    )
                                )
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to parse a remote record", e)
                    }
                }

                if (newRecords.isNotEmpty()) {
                    facedDao.insertAll(newRecords)
                    if (newMembers.isNotEmpty()) memberDao.insertAll(newMembers)
                    if (newTags.isNotEmpty()) tagDao.insertAll(newTags)
                }
            }

            // 2. Fetch Users
            val userSnapshot = firebaseDb.child("users").get().await()
            if (userSnapshot.exists()) {
                val newUsers = mutableListOf<UserEntity>()
                userSnapshot.children.forEach { child ->
                    try {
                        val map = child.value as? Map<*, *> ?: return@forEach
                        val username = map["username"] as? String ?: return@forEach
                        val role = UserRole.valueOf(map["role"] as? String ?: "STAFF")
                        val isAdmin = role == UserRole.ADMIN || username.lowercase().trim() == "ralphlax337@gmail.com"
                        val activeState = if (isAdmin) true else ((map["active"] as? Boolean) ?: (map["isActive"] as? Boolean) ?: false)

                        val existing = userDao.findByUsername(username)
                        if (existing == null) {
                            val user = UserEntity(
                                id = (map["id"] as? Long) ?: 0L,
                                username = username,
                                passwordHash = map["passwordHash"] as? String ?: "",
                                fullName = map["fullName"] as? String ?: "",
                                title = map["title"] as? String ?: "",
                                role = if (isAdmin) UserRole.ADMIN else role,
                                isActive = activeState,
                                avatarInitials = map["avatarInitials"] as? String ?: "ST"
                            )
                            newUsers.add(user)
                        } else if (existing.isActive != activeState) {
                            userDao.setActive(existing.id, activeState)
                        }
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to parse remote user", e)
                    }
                }
                if (newUsers.isNotEmpty()) {
                    userDao.insertAll(newUsers)
                }
            }

            // 3. Fetch Evac Centers
            val centerSnapshot = firebaseDb.child("evac_centers").get().await()
            if (centerSnapshot.exists()) {
                val newCenters = mutableListOf<EvacCenterEntity>()
                centerSnapshot.children.forEach { child ->
                    try {
                        // Clean up legacy non-numeric string keys from earlier builds
                        if (child.key?.toLongOrNull() == null) {
                            child.ref.removeValue()
                            return@forEach
                        }
                        val map = child.value as? Map<*, *> ?: return@forEach
                        val isActive = map["isActive"] as? Boolean ?: true
                        val id = (map["id"] as? Long) ?: 0L

                        if (!isActive) {
                            if (id > 0) evacCenterDao.hardDelete(id)
                            return@forEach
                        }

                        val name = map["name"] as? String ?: return@forEach
                        val center = EvacCenterEntity(
                            id = id,
                            name = name,
                            location = map["location"] as? String ?: "",
                            capacity = (map["capacity"] as? Long)?.toInt() ?: 100,
                            isActive = true,
                            isSynced = true
                        )
                        newCenters.add(center)
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to parse remote center", e)
                    }
                }
                if (newCenters.isNotEmpty()) {
                    evacCenterDao.insertAll(newCenters)
                }
            }

            // 4. Fetch Inventory Items
            val invSnapshot = firebaseDb.child("inventory_items").get().await()
            if (invSnapshot.exists()) {
                val newItems = mutableListOf<InventoryItemEntity>()
                invSnapshot.children.forEach { child ->
                    try {
                        // Clean up legacy non-numeric string keys from earlier builds
                        if (child.key?.toLongOrNull() == null) {
                            child.ref.removeValue()
                            return@forEach
                        }
                        val map = child.value as? Map<*, *> ?: return@forEach
                        val isActive = map["isActive"] as? Boolean ?: true
                        val id = (map["id"] as? Long) ?: 0L

                        if (!isActive) {
                            if (id > 0) inventoryDao.hardDelete(id)
                            return@forEach
                        }

                        val code = map["code"] as? String ?: return@forEach
                        val item = InventoryItemEntity(
                            id = id,
                            code = code,
                            name = map["name"] as? String ?: "",
                            category = map["category"] as? String ?: "",
                            unit = map["unit"] as? String ?: "",
                            qty = (map["qty"] as? Long)?.toInt() ?: 0,
                            lowStockThreshold = (map["lowStockThreshold"] as? Long)?.toInt() ?: 10,
                            isActive = true,
                            isSynced = true
                        )
                        newItems.add(item)
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to parse remote inventory item", e)
                    }
                }
                if (newItems.isNotEmpty()) {
                    inventoryDao.insertAll(newItems)
                }
            }

            // 5. Fetch Event Logs for Admin Auditing
            val eventSnapshot = firebaseDb.child("event_log").get().await()
            if (eventSnapshot.exists()) {
                val newEvents = mutableListOf<EventLogEntity>()
                eventSnapshot.children.forEach { child ->
                    try {
                        val map = child.value as? Map<*, *> ?: return@forEach
                        val timestamp = map["timestamp"] as? Long ?: return@forEach
                        val entity = EventLogEntity(
                            id = (map["id"] as? Long) ?: 0L,
                            facedSerial = map["facedSerial"] as? String ?: "",
                            headName = map["headName"] as? String ?: "",
                            eventType = EventType.valueOf(map["eventType"] as? String ?: "FORM_FILED"),
                            description = map["description"] as? String ?: "",
                            timestamp = timestamp,
                            performedByUserId = map["performedByUserId"] as? Long ?: 0L,
                            performedByName = map["performedByName"] as? String ?: "",
                            isSynced = true
                        )
                        newEvents.add(entity)
                    } catch (e: Exception) {
                        Log.e("SyncManager", "Failed to parse remote event log", e)
                    }
                }
                if (newEvents.isNotEmpty()) {
                    eventLogDao.insertAll(newEvents)
                }
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to fetch remote records", e)
        }
    }

    suspend fun syncPending(): Int {
        if (!SessionManager.isOnline.value) return 0
        fetchRemoteData() // Automatically download any new records from Firebase Realtime DB first!
        
        val unsynced = facedDao.getUnsynced()
        val unsyncedEvents2 = eventLogDao.getUnsynced()
        val unsyncedCenters = evacCenterDao.getUnsynced()
        val unsyncedItems = inventoryDao.getUnsynced()
        
        if (unsynced.isEmpty() && unsyncedEvents2.isEmpty() && unsyncedCenters.isEmpty() && unsyncedItems.isEmpty()) return 0

        _isSyncing.value = true
        try {
            // Push to Firebase Realtime Database automatically with FULL child nodes (members & tags)
            val updates = mutableMapOf<String, Any?>()
            unsynced.forEach { record ->
                val members = memberDao.getForRecord(record.serial)
                val tags = tagDao.getForRecord(record.serial)

                val recordMap = mutableMapOf<String, Any?>(
                    "serial" to record.serial,
                    "headName" to record.headName,
                    "barangay" to record.barangay,
                    "evacCenterName" to record.evacCenterName,
                    "disasterEvent" to record.disasterEvent,
                    "stormName" to record.stormName,
                    "civilStatus" to record.civilStatus,
                    "sex" to record.sex,
                    "birthDate" to record.birthDate,
                    "age" to record.age,
                    "birthPlace" to record.birthPlace,
                    "occupation" to record.occupation,
                    "religion" to record.religion,
                    "monthlyIncome" to record.monthlyIncome,
                    "idType" to record.idType,
                    "idNumber" to record.idNumber,
                    "contactNumber" to record.contactNumber,
                    "altContactNumber" to record.altContactNumber,
                    "mothersMaidenName" to record.mothersMaidenName,
                    "permanentAddress" to record.permanentAddress,
                    "is4Ps" to record.is4Ps,
                    "shelterDamage" to record.shelterDamage.name,
                    "status" to record.status.name,
                    "presentMembersCount" to record.presentMembersCount,
                    "createdAt" to record.createdAt,
                    "updatedAt" to record.updatedAt,
                    "filedByUserId" to record.filedByUserId,
                    "members" to members,
                    "vulnerableTags" to tags
                )

                updates["faced_records/${record.serial}"] = recordMap
            }
            
            val unsyncedEvents2 = eventLogDao.getUnsynced()
            
            if (updates.isNotEmpty() || unsyncedEvents2.isNotEmpty() || unsyncedCenters.isNotEmpty() || unsyncedItems.isNotEmpty()) {
                // Also push all local users, evac centers, and inventory items so they sync across devices
                val allUsers = userDao.observeAll().first()
                allUsers.forEach { u ->
                    updates["users/${u.username.replace(".", "_")}"] = u
                }

                unsyncedCenters.forEach { c ->
                    updates["evac_centers/${c.id}"] = c
                }

                unsyncedItems.forEach { i ->
                    updates["inventory_items/${i.id}"] = i
                }

                // Push Audit Event Logs that haven't been synced yet
                unsyncedEvents2.forEach { ev ->
                    updates["event_log/${ev.timestamp}_${ev.performedByUserId}"] = ev.copy(isSynced = true)
                }

                // Add a 10-second timeout so it doesn't hang forever if DB is unreachable or rules deny access
                withTimeout(10000) {
                    firebaseDb.updateChildren(updates).await()
                }
                
                if (unsyncedEvents2.isNotEmpty()) {
                    eventLogDao.markAllSynced()
                }
                if (unsyncedCenters.isNotEmpty()) {
                    evacCenterDao.markAllSynced()
                }
                if (unsyncedItems.isNotEmpty()) {
                    inventoryDao.markAllSynced()
                }
            }

            // Mark as synced locally once confirmed uploaded
            unsynced.forEach { facedDao.markSynced(it.serial) }
            assistanceDao.markAllSynced()
            _isSyncing.value = false
            return unsynced.size
        } catch (e: Exception) {
            Log.e("SyncManager", "Firebase sync failed", e)
            _isSyncing.value = false
            return 0
        }
    }

    suspend fun deleteEvacCenterRemote(center: EvacCenterEntity) {
        if (!SessionManager.isOnline.value) return
        try {
            if (center.id > 0) {
                firebaseDb.child("evac_centers").child(center.id.toString()).removeValue().await()
            }
            if (center.name.isNotBlank()) {
                firebaseDb.child("evac_centers").child(center.name.replace(".", "_")).removeValue().await()
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to remove remote evac center", e)
        }
    }

    suspend fun deleteInventoryItemRemote(item: InventoryItemEntity) {
        if (!SessionManager.isOnline.value) return
        try {
            if (item.id > 0) {
                firebaseDb.child("inventory_items").child(item.id.toString()).removeValue().await()
            }
            if (item.code.isNotBlank()) {
                firebaseDb.child("inventory_items").child(item.code.replace(".", "_")).removeValue().await()
            }
            if (item.name.isNotBlank()) {
                firebaseDb.child("inventory_items").child(item.name.replace(".", "_")).removeValue().await()
            }
        } catch (e: Exception) {
            Log.e("SyncManager", "Failed to remove remote inventory item", e)
        }
    }

    suspend fun hasGuestPendingData(): Boolean =
        facedDao.countByFiledBy(GUEST_USER_ID) > 0 || eventLogDao.countByUser(GUEST_USER_ID) > 0

    suspend fun syncGuestDataToUser(newUserId: Long) {
        facedDao.reassignOwner(GUEST_USER_ID, newUserId)
        assistanceDao.reassignOwner(GUEST_USER_ID, newUserId)
        eventLogDao.reassignOwner(GUEST_USER_ID, newUserId)
        syncPending() // Auto-sync immediately after reassigning
    }
}
