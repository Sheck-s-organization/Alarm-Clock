package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.data.entities.ChargingRequirement
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Listens for charging state changes (plug-in / plug-out).
 *
 * When charging state changes, re-evaluates CHARGING-type alarms.
 * If a CHARGING alarm is due within the next scheduling window and the
 * charging requirement now matches (or stops matching), reschedule accordingly.
 */
class ChargingStateReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val isNowCharging = when (intent.action) {
            Intent.ACTION_POWER_CONNECTED -> true
            Intent.ACTION_POWER_DISCONNECTED -> false
            else -> return
        }

        val app = context.applicationContext as SmartAlarmApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Only rescheduling is needed — at trigger time AlarmReceiver checks charging state
                // Here we reschedule CHARGING alarms to ensure accurate next-trigger time
                val chargingAlarms = app.alarmRepository.getEnabledAlarmsByType(AlarmType.CHARGING)

                for (alarm in chargingAlarms) {
                    // If charging state NOW matches the alarm's requirement, it might fire sooner
                    val relevant = when (alarm.chargingRequirement) {
                        ChargingRequirement.CHARGING -> isNowCharging
                        ChargingRequirement.NOT_CHARGING -> !isNowCharging
                        ChargingRequirement.NOT_REQUIRED -> false
                    }
                    if (relevant) {
                        app.smartAlarmManager.scheduleAlarm(alarm)
                    }
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
