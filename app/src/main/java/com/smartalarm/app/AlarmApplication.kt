package com.smartalarm.app

import android.app.Application
import androidx.room.Room
import com.smartalarm.app.data.db.AppDatabase
import com.smartalarm.app.data.repository.AlarmRepository

class AlarmApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "alarm_database").build()
    }

    val repository: AlarmRepository by lazy {
        AlarmRepository(database.alarmDao())
    }
}
