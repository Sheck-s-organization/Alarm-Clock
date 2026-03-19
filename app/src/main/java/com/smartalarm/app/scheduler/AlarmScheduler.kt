package com.smartalarm.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.receiver.AlarmReceiver
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

open class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    open fun schedule(alarm: Alarm) {
        if (!alarmManager.canScheduleExactAlarms()) {
            Log.e(TAG, "schedule: SCHEDULE_EXACT_ALARM permission not granted — alarm ${alarm.id} will NOT fire")
            return
        }
        val triggerAtMillis = nextTriggerTimeMillis(alarm.hour, alarm.minute)
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        Log.d(TAG, "schedule: alarm ${alarm.id} (${alarm.label}) set for ${fmt.format(Date(triggerAtMillis))}")
        val pendingIntent = buildPendingIntent(alarm.id)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
        Log.d(TAG, "schedule: setAlarmClock() called — next alarm clock: ${alarmManager.nextAlarmClock?.let { fmt.format(Date(it.triggerTime)) } ?: "none"}")
    }

    open fun cancel(alarm: Alarm) {
        alarmManager.cancel(buildPendingIntent(alarm.id))
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
