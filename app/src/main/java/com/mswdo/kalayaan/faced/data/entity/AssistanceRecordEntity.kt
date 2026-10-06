package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single relief/assistance distribution event tied to a FACED card.
 * Saving one of these atomically deducts stock from InventoryItemEntity (Feature #17).
 */
@Entity(
    tableName = "assistance_records",
    foreignKeys = [ForeignKey(
        entity = FacedRecordEntity::class,
        parentColumns = ["serial"],
        childColumns = ["facedSerial"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("facedSerial")]
)
data class AssistanceRecordEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val facedSerial: String,
    val date: String,
    val receivingMember: String,
    val disaster: String,
    val itemName: String,
    val unit: String,
    val qty: Int,
    val estimatedCost: Double,
    val provider: String,
    val authorizingOfficer: String,
    val recordedByUserId: Long = 0, // -1 = entered while in Offline Guest Mode; used to re-home guest data on login-sync
    val isSynced: Boolean = false
)
