package com.smartalarm.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Represents a holiday or day-off that disables work-schedule alarms.
 *
 * Holidays can be:
 *  - ONE_TIME    : a single specific date  (e.g., a personal day off)
 *  - ANNUAL      : recurs every year on the same month/day (e.g., Christmas)
 *  - RANGE       : spans multiple consecutive days (e.g., a vacation week)
 *
 * When a holiday matches the current date, any alarm linked to a WorkSchedule
 * will be suppressed for that day.
 */
@Entity(tableName = "holidays")
data class Holiday(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    @ColumnInfo(name = "holiday_type")
    val holidayType: HolidayType = HolidayType.ONE_TIME,

    // For ONE_TIME and RANGE: year (e.g., 2025)
    val year: Int? = null,

    // Month 1-12
    val month: Int,

    // Day of month 1-31
    val day: Int,

    // Only used for RANGE type: the end date
    @ColumnInfo(name = "end_month")
    val endMonth: Int? = null,

    @ColumnInfo(name = "end_day")
    val endDay: Int? = null,

    @ColumnInfo(name = "end_year")
    val endYear: Int? = null,

    // Whether this holiday suppresses ALL alarms, or only work-schedule ones
    @ColumnInfo(name = "suppress_all")
    val suppressAll: Boolean = false,

    // Optional emoji / icon identifier
    val emoji: String = "",

    // Whether this holiday entry was imported from an online source
    @ColumnInfo(name = "is_imported")
    val isImported: Boolean = false,

    // Which country code it was imported for (e.g. "US", "CA")
    @ColumnInfo(name = "country_code")
    val countryCode: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class HolidayType {
    ONE_TIME,   // Specific date (year + month + day)
    ANNUAL,     // Every year (month + day only)
    RANGE       // Date range (start + end)
}
