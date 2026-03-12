package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.AlarmEvent
import com.smartalarm.app.manager.AlarmDecision
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Receives the alarm trigger broadcast from AlarmManager.
 *
 * Responsibilities:
 *  1. Look up the alarm from the database
 *  2. Ask SmartAlarmManager to evaluate smart conditions
 *  3. Either launch the AlarmService (fire) or skip + reschedule
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_FIRE_ALARM) return

        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val app = context.applicationContext as SmartAlarmApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val alarm = app.alarmRepository.getAlarmById(alarmId) ?: return@launch

                val decision = app.smartAlarmManager.evaluateAlarmAtTrigger(alarm)

                when (decision) {
                    is AlarmDecision.Fire -> {
                        // Launch the foreground alarm service
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
                        }
                        context.startForegroundService(serviceIntent)

                        app.alarmRepository.logEvent(
                            alarmId = alarm.id,
                            event = AlarmEvent.FIRED,
                            wasCharging = app.smartAlarmManager.isPhoneCharging()
                        )
                    }

                    is AlarmDecision.FireWithOverride -> {
                        // The override time applies to the NEXT trigger (already computed);
                        // for the current trigger we fire normally
                        val serviceIntent = Intent(context, AlarmService::class.java).apply {
                            putExtra(Constants.EXTRA_ALARM_ID, alarm.id)
                        }
                        context.startForegroundService(serviceIntent)

                        app.alarmRepository.logEvent(
                            alarmId = alarm.id,
                            event = AlarmEvent.FIRED,
                            wasCharging = app.smartAlarmManager.isPhoneCharging()
                        )
                    }

                    is AlarmDecision.Skip -> {
                        app.alarmRepository.logEvent(
                            alarmId = alarm.id,
                            event = AlarmEvent.SKIPPED,
                            skipReason = decision.reason,
                            wasCharging = app.smartAlarmManager.isPhoneCharging()
                        )
                    }
                }

                // Reschedule for the next occurrence (repeating alarms)
                if (alarm.repeatDays.isNotEmpty() ||
                    alarm.alarmType != com.smartalarm.app.data.entities.AlarmType.STANDARD
                ) {
                    app.smartAlarmManager.scheduleAlarm(alarm)
                } else {
                    // One-time alarm — disable it
                    app.alarmRepository.setAlarmEnabled(alarm.id, false)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
