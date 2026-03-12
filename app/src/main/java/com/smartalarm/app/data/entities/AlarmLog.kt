package com.smartalarm.app.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Audit log of every alarm event (fired, snoozed, dismissed, skipped).
 * Useful for the statistics screen and debugging smart alarm decisions.
 */
@Entity(
    tableName = "alarm_logs",
    foreignKeys = [
        ForeignKey(
            entity = Alarm::class,
            parentColumns = ["id"],
            childColumns = ["alarm_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("alarm_id"), Index("triggered_at")]
)
data class AlarmLog(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "alarm_id")
    val alarmId: Long,

    @ColumnInfo(name = "triggered_at")
    val triggeredAt: Long = System.currentTimeMillis(),

    val event: AlarmEvent,

    // Reason the alarm was skipped (if applicable)
    @ColumnInfo(name = "skip_reason")
    val skipReason: SkipReason? = null,

    // Snapshot of relevant context at trigger time
    @ColumnInfo(name = "was_charging")
    val wasCharging: Boolean? = null,

    @ColumnInfo(name = "location_name")
    val locationName: String? = null,

    @ColumnInfo(name = "is_holiday")
    val isHoliday: Boolean = false,

    @ColumnInfo(name = "is_work_day")
    val isWorkDay: Boolean? = null,

    @ColumnInfo(name = "day_of_month")
    val dayOfMonth: Int? = null,

    // How many times snoozed before this event
    @ColumnInfo(name = "snooze_count")
    val snoozeCount: Int = 0
)

enum class AlarmEvent {
    FIRED,
    SNOOZED,
    DISMISSED,
    SKIPPED,    // Smart condition not met — alarm suppressed
    MISSED      // User didn't interact and timeout elapsed
}

enum class SkipReason {
    HOLIDAY,
    NOT_WORK_DAY,
    WRONG_TIME_OF_MONTH,
    LOCATION_MISMATCH,
    CHARGING_MISMATCH
}
