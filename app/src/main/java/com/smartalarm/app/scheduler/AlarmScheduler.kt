package com.smartalarm.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.receiver.AlarmReceiver
import com.smartalarm.app.util.LogBuffer
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

private const val SNOOZE_REQUEST_CODE_OFFSET = 10_000
private const val LOCATION_OVERRIDE_REQUEST_CODE_OFFSET = 20_000

open class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    open fun schedule(alarm: Alarm) {
        if (!alarmManager.canScheduleExactAlarms()) {
            LogBuffer.e(TAG, "SCHEDULE_EXACT_ALARM permission not granted — alarm ${alarm.id} will NOT fire. Grant it in Settings → Apps → Alarms & reminders.")
            return
        }
        val triggerAtMillis = nextTriggerTimeMillis(alarm.hour, alarm.minute)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val triggerStr = fmt.format(Date(triggerAtMillis))
        LogBuffer.d(TAG, "Alarm ${alarm.id} \"${alarm.label}\" scheduled for $triggerStr")
        val pendingIntent = buildPendingIntent(alarm.id)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        val nextStr = alarmManager.nextAlarmClock?.let { fmt.format(Date(it.triggerTime)) } ?: "none"
        LogBuffer.d(TAG, "setAlarmClock() OK — system next alarm: $nextStr")
    }

    open fun cancel(alarm: Alarm) {
        LogBuffer.d(TAG, "Alarm ${alarm.id} cancelled")
        alarmManager.cancel(buildPendingIntent(alarm.id))
    }

    open fun scheduleSnooze(alarm: Alarm, triggerAtMillis: Long) {
        if (!alarmManager.canScheduleExactAlarms()) {
            LogBuffer.e(TAG, "SCHEDULE_EXACT_ALARM permission not granted — snooze for alarm ${alarm.id} will NOT fire.")
            return
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        LogBuffer.d(TAG, "Snooze for alarm ${alarm.id} scheduled at ${fmt.format(Date(triggerAtMillis))}")
        val pendingIntent = buildSnoozePendingIntent(alarm.id)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    open fun cancelSnooze(alarm: Alarm) {
        LogBuffer.d(TAG, "Snooze for alarm ${alarm.id} cancelled")
        alarmManager.cancel(buildSnoozePendingIntent(alarm.id))
    }

    /**
     * Schedules a one-shot location-override alarm for [alarm] to fire at [hour]:[minute] today
     * (or tomorrow if that time has already passed). The fired intent will carry
     * [AlarmReceiver.EXTRA_IS_LOCATION_OVERRIDE] = true so the receiver skips re-evaluating
     * location and fires immediately.
     */
    open fun scheduleLocationOverride(alarm: Alarm, hour: Int, minute: Int) {
        if (!alarmManager.canScheduleExactAlarms()) {
            LogBuffer.e(TAG, "SCHEDULE_EXACT_ALARM permission not granted — location override for alarm ${alarm.id} will NOT fire.")
            return
        }
        val triggerAtMillis = nextTriggerTimeMillis(hour, minute)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        LogBuffer.d(TAG, "Location override for alarm ${alarm.id} scheduled at ${fmt.format(Date(triggerAtMillis))}")
        val pendingIntent = buildLocationOverridePendingIntent(alarm.id)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    open fun cancelLocationOverride(alarm: Alarm) {
        LogBuffer.d(TAG, "Location override for alarm ${alarm.id} cancelled")
        alarmManager.cancel(buildLocationOverridePendingIntent(alarm.id))
    }

    private fun buildLocationOverridePendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
            putExtra(AlarmReceiver.EXTRA_IS_LOCATION_OVERRIDE, true)
        }
        return PendingIntent.getBroadcast(
            context,
            (alarmId + LOCATION_OVERRIDE_REQUEST_CODE_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildSnoozePendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            (alarmId + SNOOZE_REQUEST_CODE_OFFSET).toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun buildPendingIntent(alarmId: Long): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AlarmScheduler"

        /**
         * Calculates the snooze trigger time: [nowMillis] + [snoozeDurationMinutes] in milliseconds.
         */
        fun snoozeTriggerTimeMillis(
            snoozeDurationMinutes: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long = nowMillis + snoozeDurationMinutes * 60_000L

        /**
         * Calculates the next trigger time in milliseconds for an alarm at [hour]:[minute].
         * If that time has already passed today, schedules for tomorrow.
         */
        fun nextTriggerTimeMillis(
            hour: Int,
            minute: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
            val target = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            if (!target.after(now)) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }
    }
}
