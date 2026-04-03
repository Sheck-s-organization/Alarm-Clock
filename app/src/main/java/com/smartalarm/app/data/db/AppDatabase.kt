package com.smartalarm.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.dao.HolidayDao
import com.smartalarm.app.data.dao.WorkScheduleDao
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.WorkSchedule

@Database(
    entities = [Alarm::class, WorkSchedule::class, Holiday::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
    abstract fun workScheduleDao(): WorkScheduleDao
    abstract fun holidayDao(): HolidayDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeDurationMinutes INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeMaxCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // New tables
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS work_schedules (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        workDays TEXT NOT NULL
                    )"""
                )
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS holidays (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        name TEXT NOT NULL,
                        month INTEGER NOT NULL,
                        day INTEGER NOT NULL,
                        year INTEGER,
                        type TEXT NOT NULL
                    )"""
                )
                // New nullable column on alarms
                db.execSQL("ALTER TABLE alarms ADD COLUMN workScheduleId INTEGER")
            }
        }
    }
}
