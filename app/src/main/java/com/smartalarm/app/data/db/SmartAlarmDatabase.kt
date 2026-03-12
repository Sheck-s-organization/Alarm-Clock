package com.smartalarm.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.smartalarm.app.data.dao.*
import com.smartalarm.app.data.entities.*

@Database(
    entities = [
        Alarm::class,
        WorkSchedule::class,
        Holiday::class,
        SavedLocation::class,
        AlarmLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class SmartAlarmDatabase : RoomDatabase() {

    abstract fun alarmDao(): AlarmDao
    abstract fun workScheduleDao(): WorkScheduleDao
    abstract fun holidayDao(): HolidayDao
    abstract fun savedLocationDao(): SavedLocationDao
    abstract fun alarmLogDao(): AlarmLogDao

    companion object {
        private const val DATABASE_NAME = "smart_alarm_db"

        @Volatile
        private var INSTANCE: SmartAlarmDatabase? = null

        fun getInstance(context: Context): SmartAlarmDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): SmartAlarmDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                SmartAlarmDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
