package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.smartalarm.app.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val rawId = intent?.getLongExtra(EXTRA_ALARM_ID, INVALID_ID) ?: INVALID_ID
        val alarmId = alarmIdFromIntent(rawId) ?: return
        AlarmService.start(context, alarmId)
    }

    companion object {
        const val EXTRA_ALARM_ID = "extra_alarm_id"
        private const val INVALID_ID = -1L

        /**
         * Validates and returns the alarm ID extracted from an intent extra, or null if invalid.
         * Extracted as a pure function for unit-testability without Android framework dependencies.
         */
        fun alarmIdFromIntent(alarmId: Long): Long? = if (alarmId == INVALID_ID) null else alarmId
    }
}
