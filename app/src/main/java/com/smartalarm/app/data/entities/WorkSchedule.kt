package com.smartalarm.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smartalarm.app.data.db.Converters

/**
 * Represents a work schedule that drives work-schedule alarms.
 *
 * Supports two scheduling modes:
 *  1. FIXED_DAYS   – the same days every week (e.g., Mon-Fri 9-5)
 *  2. ROTATING     – alternating shift patterns (e.g., 4-on-4-off, 2-2-3)
 *
 * The schedule is anchored to [rotationStartDate] (epoch millis) so the app
 * can compute which shift cycle the current date falls into.
 */
@Entity(tableName = "work_schedules")
@TypeConverters(Converters::class)
data class WorkSchedule(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    // Whether this is the currently active schedule
    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    // Scheduling mode
    @ColumnInfo(name = "schedule_type")
    val scheduleType: ScheduleType = ScheduleType.FIXED_DAYS,

    // ---- FIXED_DAYS config ----
    // Which days of the week are work days (1=Mon .. 7=Sun, ISO-8601)
    @ColumnInfo(name = "work_days")
    val workDays: Set<Int> = setOf(1, 2, 3, 4, 5), // Mon-Fri default

    // Default alarm time for this schedule
    @ColumnInfo(name = "alarm_hour")
    val alarmHour: Int = 7,

    @ColumnInfo(name = "alarm_minute")
    val alarmMinute: Int = 0,

    // Optional: different alarm time for specific days (JSON map of dayOfWeek -> "HH:mm")
    @ColumnInfo(name = "day_overrides_json")
    val dayOverridesJson: String? = null,

    // ---- ROTATING config ----
    // JSON-serialised list of ShiftCycle objects describing the rotation pattern
    // Example: [{"label":"Work","durationDays":4},{"label":"Off","durationDays":4}]
    @ColumnInfo(name = "shift_cycles_json")
    val shiftCyclesJson: String? = null,

    // The date the rotation pattern starts from (epoch millis, day-level precision)
    @ColumnInfo(name = "rotation_start_date")
    val rotationStartDate: Long? = null,

    // ---- Time of Month overrides ----
    // JSON list of MonthlyTimeOverride: if date matches, use different alarm time
    // Example: [{"startDay":1,"endDay":7,"hour":6,"minute":30,"label":"Month start rush"}]
    @ColumnInfo(name = "monthly_overrides_json")
    val monthlyOverridesJson: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)

enum class ScheduleType {
    FIXED_DAYS,
    ROTATING
}

/** A segment in a rotating shift cycle */
data class ShiftCycle(
    val label: String,          // e.g. "Work" or "Off"
    val durationDays: Int,      // how many consecutive days
    val isWorkDay: Boolean = label.lowercase() != "off",
    val alarmHour: Int? = null, // override alarm time for this cycle phase
    val alarmMinute: Int? = null
)

/** Per-day-of-week time override on a fixed schedule */
data class DayTimeOverride(
    val dayOfWeek: Int,  // ISO 1=Mon..7=Sun
    val hour: Int,
    val minute: Int
)

/** Override alarm time for a specific range of days within the month */
data class MonthlyTimeOverride(
    val startDay: Int,   // 1-based day of month
    val endDay: Int,
    val hour: Int,
    val minute: Int,
    val label: String = ""
)
