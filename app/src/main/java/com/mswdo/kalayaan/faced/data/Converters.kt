package com.mswdo.kalayaan.faced.data

import androidx.room.TypeConverter
import com.mswdo.kalayaan.faced.data.entity.EventType
import com.mswdo.kalayaan.faced.data.entity.EvacuationStatus
import com.mswdo.kalayaan.faced.data.entity.ShelterDamage
import com.mswdo.kalayaan.faced.data.entity.UserRole

class Converters {
    @TypeConverter
    fun fromUserRole(role: UserRole): String = role.name
    @TypeConverter
    fun toUserRole(value: String): UserRole = UserRole.valueOf(value)

    @TypeConverter
    fun fromEvacStatus(status: EvacuationStatus): String = status.name
    @TypeConverter
    fun toEvacStatus(value: String): EvacuationStatus = EvacuationStatus.valueOf(value)

    @TypeConverter
    fun fromShelterDamage(d: ShelterDamage): String = d.name
    @TypeConverter
    fun toShelterDamage(value: String): ShelterDamage = ShelterDamage.valueOf(value)

    @TypeConverter
    fun fromEventType(t: EventType): String = t.name
    @TypeConverter
    fun toEventType(value: String): EventType = EventType.valueOf(value)
}
