package com.tddalarm.app.data.repo

import com.tddalarm.app.data.HolidayEntry
import com.tddalarm.app.data.PtoEntry
import com.tddalarm.app.data.dao.WorkCalendarDao
import com.tddalarm.app.data.entity.WorkScheduleEntity
import com.tddalarm.app.data.toDomain
import com.tddalarm.app.data.toEntity
import com.tddalarm.app.data.toEntry
import com.tddalarm.app.data.toStorageString
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkCalendar
import com.tddalarm.core.schedule.WorkSchedule
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class WorkCalendarRepository(private val dao: WorkCalendarDao) : WorkCalendarStore {

    override val calendar: Flow<WorkCalendar> = combine(
        dao.schedule(),
        dao.ptoPeriods(),
        dao.holidays(),
    ) { schedule, pto, holidays ->
        WorkCalendar(
            schedule = schedule?.toDomain() ?: DEFAULT_SCHEDULE,
            ptoPeriods = pto.map { it.toDomain() },
            holidays = holidays.map { it.toDomain() },
        )
    }

    override val workingDays: Flow<Set<DayOfWeek>> =
        dao.schedule().map { it?.toDomain()?.workingDays ?: DEFAULT_SCHEDULE.workingDays }

    override val ptoEntries: Flow<List<PtoEntry>> =
        dao.ptoPeriods().map { entities -> entities.map { it.toEntry() } }

    override val holidayEntries: Flow<List<HolidayEntry>> =
        dao.holidays().map { entities -> entities.map { it.toEntry() } }

    override suspend fun currentCalendar(): WorkCalendar = WorkCalendar(
        schedule = dao.scheduleOnce()?.toDomain() ?: DEFAULT_SCHEDULE,
        ptoPeriods = dao.ptoPeriodsOnce().map { it.toDomain() },
        holidays = dao.holidaysOnce().map { it.toDomain() },
    )

    override suspend fun setWorkingDays(days: Set<DayOfWeek>) {
        dao.upsertSchedule(WorkScheduleEntity(workingDays = days.toStorageString()))
    }

    override suspend fun addPto(range: DateRange) {
        dao.insertPto(range.toEntity())
    }

    override suspend fun removePto(id: Long) = dao.deletePto(id)

    override suspend fun addHoliday(holiday: Holiday) {
        dao.insertHoliday(holiday.toEntity())
    }

    override suspend fun removeHoliday(id: Long) = dao.deleteHoliday(id)

    companion object {
        /** Until the user configures a schedule, assume a Mon–Fri work week. */
        val DEFAULT_SCHEDULE = WorkSchedule(
            setOf(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
            )
        )
    }
}
