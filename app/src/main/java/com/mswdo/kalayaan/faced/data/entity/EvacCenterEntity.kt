package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Evacuation center masterlist. Admin-managed (register/edit/deactivate centers).
 * Occupancy is derived live from FacedRecordEntity rows with status = IN_CENTER,
 * so it is never stored stale.
 */
@Entity(tableName = "evac_centers")
data class EvacCenterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val location: String,
    val capacity: Int,
    val facilities: String = "",
    val isActive: Boolean = true,
    val isSynced: Boolean = false
)
