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
 * Handles snooze action: increments the snooze counter, schedules a one-shot
 * re-trigger via [AlarmReceiver], then stops the foreground [AlarmService].
 */
class SnoozeReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val alarmId = intent?.getLongExtra(AlarmReceiver.EXTRA_ALARM_ID, -1L) ?: -1L
        if (alarmId == -1L) {
            LogBuffer.e(TAG, "SnoozeReceiver: missing alarm id — ignored")
            return
        }

        val repository = (context.applicationContext as AlarmApplication).repository
        val scheduler = AlarmScheduler(context)

        CoroutineScope(Dispatchers.IO).launch {
            val alarm = repository.getById(alarmId)
            if (alarm == null) {
                LogBuffer.e(TAG, "SnoozeReceiver: alarm $alarmId not found — ignored")
                return@launch
            }
            if (alarm.snoozeCount >= alarm.snoozeMaxCount) {
                LogBuffer.d(TAG, "SnoozeReceiver: max snoozes reached for alarm $alarmId — ignored")
                return@launch
            }
            repository.incrementSnoozeCount(alarmId)
            val triggerMillis = AlarmScheduler.snoozeTriggerTimeMillis(alarm.snoozeDurationMinutes)
            scheduler.scheduleSnooze(alarm, triggerMillis)
            LogBuffer.d(TAG, "Alarm $alarmId snoozed (${alarm.snoozeCount + 1}/${alarm.snoozeMaxCount})")
        }

        AlarmService.stop(context)
    }

    companion object {
        private const val TAG = "SnoozeReceiver"
    }
}
