package com.smartalarm.app.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.receiver.AlarmReceiver
import java.util.Calendar

open class AlarmScheduler(private val context: Context) {

    private val alarmManager: AlarmManager by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    }

    open fun schedule(alarm: Alarm) {
        val triggerAtMillis = nextTriggerTimeMillis(alarm.hour, alarm.minute)
        val pendingIntent = buildPendingIntent(alarm.id)
        val alarmClockInfo = AlarmManager.AlarmClockInfo(triggerAtMillis, pendingIntent)
        alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
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
