package com.smartalarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import com.smartalarm.app.data.db.SmartAlarmDatabase
import com.smartalarm.app.data.repository.AlarmRepository
import com.smartalarm.app.data.repository.HolidayRepository
import com.smartalarm.app.data.repository.LocationRepository
import com.smartalarm.app.data.repository.WorkScheduleRepository
import com.smartalarm.app.manager.SmartAlarmManager
import com.smartalarm.app.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class SmartAlarmApplication : Application() {

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Database
    val database by lazy { SmartAlarmDatabase.getInstance(this) }

    // Repositories
    val alarmRepository by lazy {
        AlarmRepository(database.alarmDao(), database.alarmLogDao())
    }
    val workScheduleRepository by lazy {
        WorkScheduleRepository(database.workScheduleDao())
    }
    val holidayRepository by lazy {
        HolidayRepository(database.holidayDao())
    }
    val locationRepository by lazy {
        LocationRepository(database.savedLocationDao(), this)
    }

    // Smart Alarm Manager
    val smartAlarmManager by lazy {
        SmartAlarmManager(
            context = this,
            alarmRepository = alarmRepository,
            workScheduleRepository = workScheduleRepository,
            holidayRepository = holidayRepository,
            locationRepository = locationRepository
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Alarm channel — high importance, uses alarm ringtone
        val alarmChannel = NotificationChannel(
            Constants.CHANNEL_ID_ALARM,
            "Alarms",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Alarm notifications"
            enableVibration(true)
            setShowBadge(false)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            setSound(
                Uri.parse("android.resource://$packageName/raw/alarm_default"),
                audioAttributes
            )
        }

        // Upcoming alarm reminder channel
        val upcomingChannel = NotificationChannel(
            Constants.CHANNEL_ID_UPCOMING,
            "Upcoming Alarms",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminders about upcoming alarms"
            setShowBadge(true)
        }

        // General app channel
        val generalChannel = NotificationChannel(
            Constants.CHANNEL_ID_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "General app notifications"
            setShowBadge(false)
        }

        notificationManager.createNotificationChannels(
            listOf(alarmChannel, upcomingChannel, generalChannel)
        )
    }
}
