package com.tddalarm.app.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alarms")
data class AlarmEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    /** Comma-separated [java.time.DayOfWeek] names; empty means every day. */
    val repeatDays: String,
    val enabled: Boolean,
    val skipOnDaysOff: Boolean,
    /** References [SavedLocationEntity.id]; null when the alarm has no location rule. */
    val locationPlaceId: Long?,
    val fireWhenLocationUnknown: Boolean,
)

@Entity(tableName = "saved_locations")
data class SavedLocationEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Double,
)

@Entity(tableName = "work_schedule")
data class WorkScheduleEntity(
    /** Single-row table; the one schedule always has id 1. */
    @PrimaryKey val id: Long = 1,
    /** Comma-separated [java.time.DayOfWeek] names. */
    val workingDays: String,
)

@Entity(tableName = "pto_periods")
data class PtoPeriodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startEpochDay: Long,
    val endEpochDay: Long,
)

@Entity(tableName = "holidays")
data class HolidayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val annual: Boolean,
    /** Set for one-time holidays. */
    val epochDay: Long?,
    /** Set for annual holidays. */
    val month: Int?,
    val dayOfMonth: Int?,
)
