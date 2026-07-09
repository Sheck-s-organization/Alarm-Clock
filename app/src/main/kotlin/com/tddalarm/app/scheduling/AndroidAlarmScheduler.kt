package com.tddalarm.app.scheduling

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.tddalarm.app.data.repo.AlarmSchedulerPort
import com.tddalarm.app.ui.MainActivity
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.NextTriggerCalculator
import java.time.LocalDateTime
import java.time.ZoneId

class AndroidAlarmScheduler(private val context: Context) : AlarmSchedulerPort {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    override fun scheduleNext(alarm: Alarm) {
        if (!alarm.enabled) {
            cancel(alarm.id)
            return
        }
        val next = NextTriggerCalculator.nextTrigger(alarm, LocalDateTime.now())
        scheduleAt(alarm.id, next)
    }

    override fun cancel(alarmId: Long) {
        alarmManager.cancel(triggerPendingIntent(alarmId))
    }

    /** Also used for snooze: fire [alarmId]'s trigger at an explicit time. */
    fun scheduleAt(alarmId: Long, at: LocalDateTime) {
        val triggerAtMillis = at.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val showIntent = PendingIntent.getActivity(
            context,
            alarmId.toInt(),
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        try {
            alarmManager.setAlarmClock(
                AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent),
                triggerPendingIntent(alarmId),
            )
        } catch (_: SecurityException) {
            // Exact-alarm permission revoked: fall back to an inexact alarm
            // rather than not ringing at all.
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, triggerAtMillis, triggerPendingIntent(alarmId),
            )
        }
    }

    private fun triggerPendingIntent(alarmId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            alarmId.toInt(),
            AlarmReceiver.intent(context, alarmId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
