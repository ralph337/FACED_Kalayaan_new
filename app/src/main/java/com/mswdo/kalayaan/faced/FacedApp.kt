package com.mswdo.kalayaan.faced

import android.app.Application
import com.mswdo.kalayaan.faced.data.AppDatabase
import com.mswdo.kalayaan.faced.data.SeedData
import com.mswdo.kalayaan.faced.data.repository.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class FacedApp : Application() {

    lateinit var database: AppDatabase
        private set

    lateinit var userRepository: UserRepository
        private set
    lateinit var evacCenterRepository: EvacCenterRepository
        private set
    lateinit var inventoryRepository: InventoryRepository
        private set
    lateinit var facedRepository: FacedRepository
        private set
    lateinit var syncManager: SyncManager
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)

        userRepository = UserRepository(database.userDao(), database.eventLogDao())
        evacCenterRepository = EvacCenterRepository(database.evacCenterDao(), database.facedRecordDao())
        inventoryRepository = InventoryRepository(database.inventoryDao())
        facedRepository = FacedRepository(
            database.facedRecordDao(),
            database.householdMemberDao(),
            database.vulnerableTagDao(),
            database.assistanceRecordDao(),
            database.inventoryDao(),
            database.eventLogDao()
        )
        syncManager = SyncManager(
            database.facedRecordDao(),
            database.householdMemberDao(),
            database.vulnerableTagDao(),
            database.assistanceRecordDao(),
            database.eventLogDao(),
            database.userDao(),
            database.evacCenterDao(),
            database.inventoryDao()
        )

        CoroutineScope(Dispatchers.IO).launch {
            SeedData.populate(database)
        }
    }
}
