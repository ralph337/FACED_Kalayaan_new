package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Relief goods inventory (Feature #11). Stock is adjusted automatically when
 * an AssistanceRecordEntity is created against this item (Feature #17).
 */
@Entity(tableName = "inventory_items")
data class InventoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val code: String,
    val name: String,
    val category: String, // E.g., Consumables, Non-consumables
    val unit: String,
    val qty: Int,
    val lowStockThreshold: Int = 20,
    val isActive: Boolean = true,
    val isSynced: Boolean = false
)
