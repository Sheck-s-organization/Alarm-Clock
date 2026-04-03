package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.util.LogBuffer
import com.smartalarm.app.util.WorkDayChecker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val rawId = intent?.getLongExtra(EXTRA_ALARM_ID, INVALID_ID) ?: INVALID_ID
        val alarmId = alarmIdFromIntent(rawId)
        if (alarmId == null) {
            LogBuffer.e(TAG, "Received intent with invalid alarm id ($rawId) — dropped")
            return
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AlarmApplication
                val alarm = app.repository.getById(alarmId)
                if (alarm == null) {
                    LogBuffer.e(TAG, "Alarm $alarmId not found in DB — dropped")
                    return@launch
                }

                val workSchedule = alarm.workScheduleId?.let {
                    app.workScheduleRepository.getById(it)
                }
                val holidays = app.holidayRepository.getAllNow()

                if (!WorkDayChecker.shouldFire(workSchedule, holidays)) {
                    LogBuffer.d(TAG, "Alarm $alarmId skipped — not a work day or is a holiday")
                    return@launch
                }

                LogBuffer.d(TAG, "Alarm $alarmId triggered — starting AlarmService")
                AlarmService.start(context, alarmId)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val INVALID_ID = -1L
        private const val TAG = "AlarmReceiver"

        /**
         * Validates and returns the alarm ID extracted from an intent extra, or null if invalid.
         * Extracted as a pure function for unit-testability without Android framework dependencies.
         */
        fun alarmIdFromIntent(alarmId: Long): Long? = if (alarmId == INVALID_ID) null else alarmId
    }
}
