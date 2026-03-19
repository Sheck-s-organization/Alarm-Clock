package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.smartalarm.app.service.AlarmService

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive: intent=$intent")
        context ?: return
        val rawId = intent?.getLongExtra(EXTRA_ALARM_ID, INVALID_ID) ?: INVALID_ID
        val alarmId = alarmIdFromIntent(rawId)
        if (alarmId == null) {
            Log.e(TAG, "onReceive: invalid alarm id ($rawId) — dropping")
            return
        }
        Log.d(TAG, "onReceive: starting AlarmService for alarm $alarmId")
        AlarmService.start(context, alarmId)
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
