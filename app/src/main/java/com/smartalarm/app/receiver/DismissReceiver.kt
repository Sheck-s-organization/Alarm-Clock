package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.scheduler.AlarmScheduler
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.util.LogBuffer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Handles dismiss action: resets the snooze counter, cancels any pending
 * snooze re-trigger, then stops the foreground [AlarmService].
 */
class DismissReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            LogBuffer.e(TAG, "DismissReceiver: missing alarm id — ignored")
            return
        }

        val repository = (context.applicationContext as AlarmApplication).repository
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = repository.getById(alarmId)
            if (alarm != null) {
                repository.resetSnoozeCount(alarmId)
                scheduler.cancelSnooze(alarm)
            }
            LogBuffer.d(TAG, "Alarm $alarmId dismissed")
        }

        AlarmService.stop(context)
    }

    companion object {
        private const val TAG = "DismissReceiver"
    }
}
