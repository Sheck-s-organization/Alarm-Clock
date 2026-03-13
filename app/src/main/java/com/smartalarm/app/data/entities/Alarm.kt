package com.smartalarm.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.smartalarm.app.data.db.Converters

/**
 * Core alarm entity.
 *
 * Alarm types:
 *  - STANDARD      : A plain one-time or repeating alarm
 *  - WORK_SCHEDULE : Fires only on work days derived from a WorkSchedule
 *  - TIME_OF_MONTH : Fires on specific day-of-month ranges (e.g., "first week")
 *  - LOCATION      : Fires or adjusts based on which saved location the user is at
 *  - CHARGING      : Fires only when the phone's charging state matches the requirement
 */
@Entity(tableName = "alarms")
@TypeConverters(Converters::class)
data class Alarm(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    // Human-readable label
    val label: String = "",

    // Hour (0-23) and minute (0-59) for the alarm time
    val hour: Int,
    val minute: Int,

    // AlarmType enum stored as String
    @ColumnInfo(name = "alarm_type")
    val alarmType: AlarmType = AlarmType.STANDARD,

    // Whether this alarm is currently active
    val enabled: Boolean = true,

    // Days of the week this alarm repeats (stored as Set<Int> where 1=Mon..7=Sun per ISO)
    @ColumnInfo(name = "repeat_days")
    val repeatDays: Set<Int> = emptySet(),

    // ---- Work Schedule link ----
    // Foreign key to WorkSchedule (nullable — only used for WORK_SCHEDULE type)
    @ColumnInfo(name = "work_schedule_id")
    val workScheduleId: Long? = null,

    // How the work-schedule alarm repeats (only used for WORK_SCHEDULE type)
    @ColumnInfo(name = "work_schedule_repeat")
    val workScheduleRepeat: WorkScheduleRepeat = WorkScheduleRepeat.EVERY_WORKDAY,

    // ---- Time of Month config ----
    // JSON-serialised list of MonthPeriod objects indicating which parts of the month
    // Example: [{"startDay":1,"endDay":7,"label":"First week"}]
    @ColumnInfo(name = "month_periods_json")
    val monthPeriodsJson: String? = null,

    // ---- Location config ----
    // JSON-serialised list of AlarmLocation objects (saved location names & radii)
    // When non-null, alarm only fires if user is within one of the saved locations
    @ColumnInfo(name = "location_rule_json")
    val locationRuleJson: String? = null,

    // ---- Charging config ----
    // REQUIRED: phone must be charging; NOT_REQUIRED: charging doesn't matter;
    // NOT_CHARGING: phone must NOT be charging
    @ColumnInfo(name = "charging_requirement")
    val chargingRequirement: ChargingRequirement = ChargingRequirement.NOT_REQUIRED,

    // ---- Snooze config ----
    @ColumnInfo(name = "snooze_duration_minutes")
    val snoozeDurationMinutes: Int = 9,

    @ColumnInfo(name = "snooze_max_count")
    val snoozeMaxCount: Int = 3,

    // ---- Sound & Vibration ----
    @ColumnInfo(name = "ringtone_uri")
    val ringtoneUri: String? = null,

    val vibrate: Boolean = true,

    @ColumnInfo(name = "volume_percent")
    val volumePercent: Int = 80,

    // Gradually increase volume over this many seconds (0 = instant)
    @ColumnInfo(name = "gradual_volume_seconds")
    val gradualVolumeSeconds: Int = 30,

    // ---- Metadata ----
    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "next_trigger_time")
    val nextTriggerTime: Long? = null
)

enum class AlarmType {
    STANDARD,
    WORK_SCHEDULE,
    TIME_OF_MONTH,
    LOCATION,
    CHARGING
}

enum class WorkScheduleRepeat {
    EVERY_WORKDAY,            // Fires Mon–Fri (excluding holidays)
    LAST_WORKDAY_OF_WEEK,     // Fires on the last workday of each week (usually Friday)
    LAST_WORKDAY_OF_MONTH     // Fires on the last workday of each calendar month
}

enum class ChargingRequirement {
    NOT_REQUIRED,   // Alarm fires regardless of charging state
    CHARGING,       // Alarm fires ONLY when phone is charging
    NOT_CHARGING    // Alarm fires ONLY when phone is NOT charging
}
