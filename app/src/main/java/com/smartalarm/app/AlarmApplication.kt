package com.smartalarm.app

import android.app.Application
import androidx.room.Room
import com.smartalarm.app.data.db.AppDatabase
import com.smartalarm.app.data.repository.AlarmRepository
import com.smartalarm.app.data.repository.HolidayRepository
import com.smartalarm.app.data.repository.SavedLocationRepository
import com.smartalarm.app.data.repository.WorkScheduleRepository

class AlarmApplication : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "alarm_database")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4)
            .build()
    }

    val repository: AlarmRepository by lazy {
        AlarmRepository(database.alarmDao())
    }

    val workScheduleRepository: WorkScheduleRepository by lazy {
        WorkScheduleRepository(database.workScheduleDao())
    }

    val holidayRepository: HolidayRepository by lazy {
        HolidayRepository(database.holidayDao())
    }

    val savedLocationRepository: SavedLocationRepository by lazy {
        SavedLocationRepository(database.savedLocationDao())
    }
}
