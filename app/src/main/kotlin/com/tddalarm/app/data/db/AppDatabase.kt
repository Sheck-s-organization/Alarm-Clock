package com.tddalarm.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.tddalarm.app.data.dao.AlarmDao
import com.tddalarm.app.data.dao.SavedLocationDao
import com.tddalarm.app.data.dao.WorkCalendarDao
import com.tddalarm.app.data.entity.AlarmEntity
import com.tddalarm.app.data.entity.HolidayEntity
import com.tddalarm.app.data.entity.PtoPeriodEntity
import com.tddalarm.app.data.entity.SavedLocationEntity
import com.tddalarm.app.data.entity.WorkScheduleEntity

@Database(
    entities = [
        AlarmEntity::class,
        SavedLocationEntity::class,
        WorkScheduleEntity::class,
        PtoPeriodEntity::class,
        HolidayEntity::class,
    ],
    version = 2,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao

    abstract fun savedLocationDao(): SavedLocationDao

    abstract fun workCalendarDao(): WorkCalendarDao

    companion object {
        @Volatile
        private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tdd-alarm-clock.db",
                )
                    // Pre-1.0: schema changes wipe rather than migrate.
                    .fallbackToDestructiveMigration()
                    .build().also { instance = it }
            }
    }
}
