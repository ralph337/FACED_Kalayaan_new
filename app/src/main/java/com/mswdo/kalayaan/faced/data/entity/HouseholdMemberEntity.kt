package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A single household/family member listed under a FACED record (item #25 roster).
 */
@Entity(
    tableName = "household_members",
    foreignKeys = [ForeignKey(
        entity = FacedRecordEntity::class,
        parentColumns = ["serial"],
        childColumns = ["facedSerial"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("facedSerial")]
)
data class HouseholdMemberEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val facedSerial: String,
    val name: String,
    val relation: String,
    val sex: String,
    val birthDate: String,
    val age: Int,
    val ageCategory: String, // Infant (0-2) / Child-Minor / Adult / Senior Citizen (60+)
    val education: String,
    val occupation: String,
    val remarks: String = ""
)
