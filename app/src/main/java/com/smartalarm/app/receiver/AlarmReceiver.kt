package com.smartalarm.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.google.android.gms.location.LocationServices
import com.smartalarm.app.AlarmApplication
import com.smartalarm.app.scheduler.AlarmScheduler
import com.smartalarm.app.service.AlarmService
import com.smartalarm.app.util.HaversineLocationMatcher
import com.smartalarm.app.util.LogBuffer
import com.smartalarm.app.util.WorkDayChecker
import com.smartalarm.app.util.resolveAlarmTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context?, intent: Intent?) {
        context ?: return
        val rawId = intent?.getLongExtra(EXTRA_ALARM_ID, INVALID_ID) ?: INVALID_ID
        val alarmId = alarmIdFromIntent(rawId)
        if (alarmId == null) {
            LogBuffer.e(TAG, "Received intent with invalid alarm id ($rawId) — dropped")
            return
        }

        val isLocationOverride = intent?.getBooleanExtra(EXTRA_IS_LOCATION_OVERRIDE, false) ?: false

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val app = context.applicationContext as AlarmApplication
                val alarm = app.repository.getById(alarmId)
                if (alarm == null) {
                    LogBuffer.e(TAG, "Alarm $alarmId not found in DB — dropped")
                    return@launch
                }

                // Work-schedule / holiday check (always applied, even for override triggers)
                val workSchedule = alarm.workScheduleId?.let {
                    app.workScheduleRepository.getById(it)
                }
                val holidays = app.holidayRepository.getAllNow()

                if (!WorkDayChecker.shouldFire(workSchedule, holidays)) {
                    LogBuffer.d(TAG, "Alarm $alarmId skipped — not a work day or is a holiday")
                    return@launch
                }

                // Location override check — skip if this IS the override trigger
                if (!isLocationOverride && alarm.locationOverrides.isNotEmpty()) {
                    val savedLocations = app.savedLocationRepository.getAllNow()
                    if (savedLocations.isNotEmpty()) {
                        val deviceLocation = try {
                            LocationServices.getFusedLocationProviderClient(context)
                                .lastLocation.await()
                        } catch (e: SecurityException) {
                            LogBuffer.e(TAG, "Location permission denied — firing at default time")
                            null
                        }

                        if (deviceLocation != null) {
                            val matched = HaversineLocationMatcher.findMatch(
                                deviceLocation.latitude, deviceLocation.longitude, savedLocations
                            )
                            val (effectiveHour, effectiveMinute) = resolveAlarmTime(
                                matched, alarm.locationOverrides, alarm.hour, alarm.minute
                            )

                            if (effectiveHour != alarm.hour || effectiveMinute != alarm.minute) {
                                // Different time required — reschedule at the override time
                                LogBuffer.d(
                                    TAG,
                                    "Alarm $alarmId in location \"${matched?.name}\" — " +
                                        "rescheduling for $effectiveHour:${effectiveMinute.toString().padStart(2, '0')}"
                                )
                                AlarmScheduler(context).scheduleLocationOverride(
                                    alarm, effectiveHour, effectiveMinute
                                )
                                return@launch
                            }
                        }
                    }
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
        const val EXTRA_IS_LOCATION_OVERRIDE = "extra_is_location_override"
        private const val INVALID_ID = -1L
        private const val TAG = "AlarmReceiver"

        /**
         * Validates and returns the alarm ID extracted from an intent extra, or null if invalid.
         * Extracted as a pure function for unit-testability without Android framework dependencies.
         */
        fun alarmIdFromIntent(alarmId: Long): Long? = if (alarmId == INVALID_ID) null else alarmId
    }
}
