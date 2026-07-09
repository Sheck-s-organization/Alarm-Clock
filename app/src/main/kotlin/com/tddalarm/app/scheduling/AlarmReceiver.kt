package com.tddalarm.app.scheduling

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tddalarm.app.AlarmApplication
import com.tddalarm.app.firing.AlarmRingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return

        val container = AlarmApplication.from(context).container
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                when (val outcome = container.handleAlarmTrigger(alarmId)) {
                    is HandleAlarmTrigger.Outcome.Ring ->
                        AlarmRingService.start(context, outcome.alarm)
                    is HandleAlarmTrigger.Outcome.Silent ->
                        Log.i(TAG, "Alarm $alarmId skipped: ${outcome.reason}")
                    HandleAlarmTrigger.Outcome.NotFound ->
                        Log.w(TAG, "Alarm $alarmId no longer exists")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AlarmReceiver"
        const val EXTRA_ALARM_ID = "alarm_id"

        fun intent(context: Context, alarmId: Long): Intent =
            Intent(context, AlarmReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
