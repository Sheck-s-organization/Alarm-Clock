package com.smartalarm.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar

/**
 * A named set of weekdays that defines when an alarm should fire.
 * [workDays] stores [Calendar] day-of-week constants (e.g. [Calendar.MONDAY] = 2).
 */
@Entity(tableName = "work_schedules")
data class WorkSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    /** Calendar.SUNDAY=1, MONDAY=2, …, SATURDAY=7 */
    val workDays: Set<Int> = setOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
        Calendar.THURSDAY, Calendar.FRIDAY
    )
)
