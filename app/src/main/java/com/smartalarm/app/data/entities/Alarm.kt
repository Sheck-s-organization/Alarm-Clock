package com.smartalarm.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.smartalarm.app.data.entities.LocationTimeOverride

@Entity(tableName = "alarms")
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val label: String,
    val hour: Int,
    val minute: Int,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val snoozeDurationMinutes: Int = 9,
    val snoozeMaxCount: Int = 3,
    val snoozeCount: Int = 0,
    /** Links to a [WorkSchedule]; null means no schedule filter (always fires). */
    val workScheduleId: Long? = null,
    /**
     * Per-location time overrides. When the device is within a saved location's radius,
     * the alarm fires at that override's hour/minute instead of the default [hour]/[minute].
     * Stored as a JSON array via [com.smartalarm.app.data.db.Converters].
     */
    val locationOverrides: List<LocationTimeOverride> = emptyList()
)
