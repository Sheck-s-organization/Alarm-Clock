package com.smartalarm.app.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class HolidayType { ONE_TIME, ANNUAL }

/**
 * A day off that causes work-schedule alarms to skip.
 *
 * [month] and [day] use 1-based values (1 = January, 1 = 1st of month).
 * [year] is only set for [HolidayType.ONE_TIME] holidays; null means the
 * holiday repeats every year ([HolidayType.ANNUAL]).
 */
@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val month: Int,
    val day: Int,
    val year: Int? = null,
    val type: HolidayType = HolidayType.ANNUAL
)
