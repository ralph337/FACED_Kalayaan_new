package com.mswdo.kalayaan.faced.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class EvacuationStatus { IN_CENTER, LEFT_CENTER }
enum class ShelterDamage { NONE, PARTIALLY_DAMAGED, TOTALLY_DAMAGED }

/**
 * The FACED form itself (Family Assistance Card in Emergencies and Disasters).
 * `serial` is the human-readable card number (e.g. KL-2026-001) and doubles as the
 * primary key so duplicate-serial filing is impossible at the DB layer; a secondary
 * name+barangay+disaster duplicate check is done in the repository (Functional Req #8).
 *
 * `isSynced` + `updatedAt` drive the offline-first sync queue (Functional Req #1/#10):
 * a record created or edited while offline is written locally immediately and marked
 * isSynced = false; SyncManager flips it to true once "uploaded".
 */
@Entity(tableName = "faced_records")
data class FacedRecordEntity(
    @PrimaryKey val serial: String,
    val headName: String,
    val barangay: String,
    val evacCenterName: String,
    val disasterEvent: String,
    val stormName: String = "",

    // Head of family profile (items 7-24 of the official FACED form)
    val civilStatus: String,
    val sex: String,
    val birthDate: String,
    val age: Int,
    val birthPlace: String,
    val occupation: String,
    val religion: String,
    val monthlyIncome: String,
    val idType: String,
    val idNumber: String,
    val contactNumber: String,
    val altContactNumber: String,
    val mothersMaidenName: String,
    val permanentAddress: String,
    val is4Ps: Boolean,
    val shelterDamage: ShelterDamage,

    val status: EvacuationStatus = EvacuationStatus.IN_CENTER,
    val presentMembersCount: Int = 1,
    val isSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val filedByUserId: Long = 0
)
