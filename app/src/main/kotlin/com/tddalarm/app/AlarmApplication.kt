package com.tddalarm.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.tddalarm.app.data.db.AppDatabase
import com.tddalarm.app.data.repo.AlarmRepository
import com.tddalarm.app.data.repo.AlarmStore
import com.tddalarm.app.data.repo.PlaceRepository
import com.tddalarm.app.data.repo.PlaceStore
import com.tddalarm.app.data.repo.WorkCalendarRepository
import com.tddalarm.app.data.repo.WorkCalendarStore
import com.tddalarm.app.location.AddressResolver
import com.tddalarm.app.location.FusedLocationProvider
import com.tddalarm.app.location.GeocoderAddressResolver
import com.tddalarm.app.location.LocationProvider
import com.tddalarm.app.scheduling.AndroidAlarmScheduler
import com.tddalarm.app.scheduling.HandleAlarmTrigger

class AlarmApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createAlarmChannel()
    }

    private fun createAlarmChannel() {
        val channel = NotificationChannel(
            ALARM_CHANNEL_ID,
            getString(R.string.alarm_notification_channel),
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            setSound(null, null) // AlarmRingService plays the sound itself.
            enableVibration(false)
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_firing"

        fun from(context: Context): AlarmApplication =
            context.applicationContext as AlarmApplication
    }
}

/** Hand-rolled dependency container — small enough not to need a DI framework. */
class AppContainer(context: Context) {
    private val db = AppDatabase.get(context)

    val alarmStore: AlarmStore = AlarmRepository(db.alarmDao(), db.savedLocationDao())
    val placeStore: PlaceStore = PlaceRepository(db.savedLocationDao())
    val calendarStore: WorkCalendarStore = WorkCalendarRepository(db.workCalendarDao())
    val scheduler: AndroidAlarmScheduler = AndroidAlarmScheduler(context)
    val locationProvider: LocationProvider = FusedLocationProvider(context)
    val addressResolver: AddressResolver = GeocoderAddressResolver(context)
    val handleAlarmTrigger: HandleAlarmTrigger =
        HandleAlarmTrigger(alarmStore, calendarStore, locationProvider, scheduler)
}
