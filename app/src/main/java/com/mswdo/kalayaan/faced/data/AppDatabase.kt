package com.mswdo.kalayaan.faced.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.mswdo.kalayaan.faced.data.dao.*
import com.mswdo.kalayaan.faced.data.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * The single local database. This app is offline-first: every screen reads and
 * writes here first; the "Sync to Kalayaan DB" action (see SyncManager) is what
 * later pushes unsynced rows to a real backend once one exists / connectivity
 * returns (Functional Req #1 and #10). Room + SQLite gives local persistence
 * with no server dependency at all, which matches the client's requirement that
 * the app must be fully usable offline.
 *
 * NOTE on local encryption (Non-Functional Req: Security): to keep this project
 * buildable without extra native binaries, this file uses plain Room/SQLite.
 * For production, swap this for SQLCipher's `net.zetetic:android-database-sqlcipher`
 * SupportFactory (drop-in - only this file changes) to get AES-256 at-rest encryption
 * of the local database file.
 */
@Database(
    entities = [
        UserEntity::class,
        EvacCenterEntity::class,
        InventoryItemEntity::class,
        FacedRecordEntity::class,
        HouseholdMemberEntity::class,
        VulnerableTagEntity::class,
        AssistanceRecordEntity::class,
        EventLogEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun evacCenterDao(): EvacCenterDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun facedRecordDao(): FacedRecordDao
    abstract fun householdMemberDao(): HouseholdMemberDao
    abstract fun vulnerableTagDao(): VulnerableTagDao
    abstract fun assistanceRecordDao(): AssistanceRecordDao
    abstract fun eventLogDao(): EventLogDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: run {
                    lateinit var built: AppDatabase
                    built = Room.databaseBuilder(
                        context.applicationContext,
                        AppDatabase::class.java,
                        "faced_kalayaan.db"
                    ).addCallback(object : RoomDatabase.Callback() {
                        override fun onCreate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onCreate(db)
                            CoroutineScope(Dispatchers.IO).launch {
                                SeedData.populate(built)
                            }
                        }
                    }).fallbackToDestructiveMigration().build()
                    INSTANCE = built
                    built
                }
            }
    }
}
