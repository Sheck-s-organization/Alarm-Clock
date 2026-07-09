package com.tddalarm.app.data.repo

import com.tddalarm.app.data.HolidayEntry
import com.tddalarm.app.data.PtoEntry
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkCalendar
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

/** Persistence port for alarms; Room-backed in the app, in-memory in tests. */
interface AlarmStore {
    val alarms: Flow<List<Alarm>>
    suspend fun getById(id: Long): Alarm?
    suspend fun save(alarm: Alarm): Long
    suspend fun delete(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
    suspend fun enabledAlarms(): List<Alarm>
}

/** Persistence port for the work calendar (schedule + PTO + holidays). */
interface WorkCalendarStore {
    val calendar: Flow<WorkCalendar>
    val workingDays: Flow<Set<DayOfWeek>>
    val ptoEntries: Flow<List<PtoEntry>>
    val holidayEntries: Flow<List<HolidayEntry>>
    suspend fun currentCalendar(): WorkCalendar
    suspend fun setWorkingDays(days: Set<DayOfWeek>)
    suspend fun addPto(range: DateRange)
    suspend fun removePto(id: Long)
    suspend fun addHoliday(holiday: Holiday)
    suspend fun removeHoliday(id: Long)
}

/** Scheduling port so ViewModels and receivers don't touch AlarmManager directly. */
interface AlarmSchedulerPort {
    /** (Re)schedule the alarm's next trigger; cancels instead if it can never fire. */
    fun scheduleNext(alarm: Alarm)
    fun cancel(alarmId: Long)
}
