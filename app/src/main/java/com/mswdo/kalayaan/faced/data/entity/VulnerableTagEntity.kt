package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Tags a specific person (head or member) in a FACED record as belonging to a
 * vulnerable group (item #26): senior citizen, PWD, pregnant/lactating woman, minor, etc.
 */
@Entity(
    tableName = "vulnerable_tags",
    foreignKeys = [ForeignKey(
        entity = FacedRecordEntity::class,
        parentColumns = ["serial"],
        childColumns = ["facedSerial"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("facedSerial")]
)
data class VulnerableTagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val facedSerial: String,
    val personName: String,
    val vulnerabilityType: String
)
