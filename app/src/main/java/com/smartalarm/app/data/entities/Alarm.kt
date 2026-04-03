package com.smartalarm.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

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
    val workScheduleId: Long? = null
)
