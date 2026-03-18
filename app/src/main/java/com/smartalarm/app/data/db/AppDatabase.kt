package com.smartalarm.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm

@Database(entities = [Alarm::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun alarmDao(): AlarmDao
}
