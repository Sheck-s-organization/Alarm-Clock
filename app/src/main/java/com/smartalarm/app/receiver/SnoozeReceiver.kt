package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.AlarmEvent
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_SNOOZE_ALARM) return

        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        val snoozeCount = intent.getIntExtra(Constants.EXTRA_SNOOZE_COUNT, 0)
        if (alarmId == -1L) return

        // Stop the currently ringing alarm service
        context.stopService(Intent(context, AlarmService::class.java))

        val app = context.applicationContext as SmartAlarmApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = app.alarmRepository.getAlarmById(alarmId) ?: return@launch
                val snoozeDurationMs = alarm.snoozeDurationMinutes * 60 * 1000L
                val snoozeTime = System.currentTimeMillis() + snoozeDurationMs

                // Schedule a one-shot snooze alarm using AlarmManager
                val snoozeIntent = Intent(context, AlarmReceiver::class.java).apply {
                    action = Constants.ACTION_FIRE_ALARM
                    putExtra(Constants.EXTRA_ALARM_ID, alarmId)
                    putExtra(Constants.EXTRA_IS_SNOOZE, true)
                    putExtra(Constants.EXTRA_SNOOZE_COUNT, snoozeCount + 1)
                }

                val pendingIntent = android.app.PendingIntent.getBroadcast(
                    context,
                    // Use a unique request code to avoid collision with the main alarm PI
                    (alarmId * 10 + 1).toInt(),
                    snoozeIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or
                            android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val alarmManager = context.getSystemService(Context.ALARM_SERVICE)
                        as android.app.AlarmManager
                alarmManager.setAlarmClock(
                    android.app.AlarmManager.AlarmClockInfo(snoozeTime, pendingIntent),
                    pendingIntent
                )

                app.alarmRepository.logEvent(
                    alarmId = alarmId,
                    event = AlarmEvent.SNOOZED,
                    snoozeCount = snoozeCount + 1
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
