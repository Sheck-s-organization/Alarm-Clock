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

class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Constants.ACTION_DISMISS_ALARM) return

        val alarmId = intent.getLongExtra(Constants.EXTRA_ALARM_ID, -1L)
        val snoozeCount = intent.getIntExtra(Constants.EXTRA_SNOOZE_COUNT, 0)
        if (alarmId == -1L) return

        context.stopService(Intent(context, AlarmService::class.java))

        val app = context.applicationContext as SmartAlarmApplication
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                app.alarmRepository.logEvent(
                    alarmId = alarmId,
                    event = AlarmEvent.DISMISSED,
                    snoozeCount = snoozeCount
                )
            } finally {
                pendingResult.finish()
            }
        }
    }
}
