package com.tddalarm.app.firing

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.tddalarm.app.AlarmApplication
import java.time.LocalDateTime

class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        AlarmRingService.stop(context)

        val alarmId = intent.getLongExtra(EXTRA_ALARM_ID, -1L)
        if (alarmId == -1L) return
        AlarmApplication.from(context).container.scheduler
            .scheduleAt(alarmId, LocalDateTime.now().plusMinutes(SNOOZE_MINUTES))
    }

    companion object {
        const val EXTRA_ALARM_ID = "alarm_id"
        const val SNOOZE_MINUTES = 9L

        fun intent(context: Context, alarmId: Long): Intent =
            Intent(context, SnoozeReceiver::class.java).putExtra(EXTRA_ALARM_ID, alarmId)
    }
}
