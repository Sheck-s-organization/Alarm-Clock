package com.smartalarm.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeDurationMinutes INTEGER NOT NULL DEFAULT 9")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeMaxCount INTEGER NOT NULL DEFAULT 3")
                db.execSQL("ALTER TABLE alarms ADD COLUMN snoozeCount INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
}
