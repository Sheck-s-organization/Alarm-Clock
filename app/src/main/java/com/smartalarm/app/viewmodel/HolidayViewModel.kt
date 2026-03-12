package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import kotlinx.coroutines.launch
import java.util.Calendar

class HolidayViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmartAlarmApplication
    private val repo = app.holidayRepository
    private val alarmManager = app.smartAlarmManager

    val allHolidays: LiveData<List<Holiday>> = repo.allHolidaysFlow.asLiveData()

    private val _result = MutableLiveData<OperationResult>()
    val result: LiveData<OperationResult> = _result

    fun saveHoliday(holiday: Holiday) = viewModelScope.launch {
        if (holiday.id == 0L) {
            repo.insertHoliday(holiday)
        } else {
            repo.updateHoliday(holiday)
        }
        alarmManager.scheduleAllAlarms()
        _result.value = OperationResult.Success("Holiday saved")
    }

    fun deleteHoliday(holiday: Holiday) = viewModelScope.launch {
        repo.deleteHoliday(holiday)
        alarmManager.scheduleAllAlarms()
        _result.value = OperationResult.Success("Holiday deleted")
    }

    /** Add a one-time personal day off */
    fun addPersonalDayOff(year: Int, month: Int, day: Int, name: String = "Day Off") =
        viewModelScope.launch {
            val holiday = Holiday(
                name = name,
                holidayType = HolidayType.ONE_TIME,
                year = year,
                month = month,
                day = day
            )
            repo.insertHoliday(holiday)
            alarmManager.scheduleAllAlarms()
        }

    /** Add an annual recurring holiday */
    fun addAnnualHoliday(month: Int, day: Int, name: String, emoji: String = "") =
        viewModelScope.launch {
            val holiday = Holiday(
                name = name,
                holidayType = HolidayType.ANNUAL,
                month = month,
                day = day,
                emoji = emoji
            )
            repo.insertHoliday(holiday)
            alarmManager.scheduleAllAlarms()
        }

    /** Add a vacation / multi-day range */
    fun addVacationRange(
        startYear: Int, startMonth: Int, startDay: Int,
        endYear: Int, endMonth: Int, endDay: Int,
        name: String = "Vacation"
    ) = viewModelScope.launch {
        val holiday = Holiday(
            name = name,
            holidayType = HolidayType.RANGE,
            year = startYear,
            month = startMonth,
            day = startDay,
            endYear = endYear,
            endMonth = endMonth,
            endDay = endDay
        )
        repo.insertHoliday(holiday)
        alarmManager.scheduleAllAlarms()
    }

    /** Seed common US federal holidays for the current and next year */
    fun seedUsHolidays() = viewModelScope.launch {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val holidays = buildUsHolidays(currentYear) + buildUsHolidays(currentYear + 1)
        repo.replaceImportedHolidays("US", holidays)
        alarmManager.scheduleAllAlarms()
        _result.value = OperationResult.Success("US holidays imported")
    }

    private fun buildUsHolidays(year: Int): List<Holiday> = listOf(
        Holiday(name = "New Year's Day",      holidayType = HolidayType.ANNUAL, month = 1,  day = 1,  emoji = "🎉", isImported = true, countryCode = "US"),
        Holiday(name = "Independence Day",    holidayType = HolidayType.ANNUAL, month = 7,  day = 4,  emoji = "🇺🇸", isImported = true, countryCode = "US"),
        Holiday(name = "Veterans Day",        holidayType = HolidayType.ANNUAL, month = 11, day = 11, emoji = "🎖️", isImported = true, countryCode = "US"),
        Holiday(name = "Christmas Day",       holidayType = HolidayType.ANNUAL, month = 12, day = 25, emoji = "🎄", isImported = true, countryCode = "US"),
        // MLK Day — 3rd Monday of January
        nthWeekdayHoliday(year, 1, Calendar.MONDAY, 3, "Martin Luther King Jr. Day", "✊", "US"),
        // Presidents Day — 3rd Monday of February
        nthWeekdayHoliday(year, 2, Calendar.MONDAY, 3, "Presidents' Day", "🏛️", "US"),
        // Memorial Day — last Monday of May
        lastWeekdayHoliday(year, 5, Calendar.MONDAY, "Memorial Day", "🇺🇸", "US"),
        // Labor Day — 1st Monday of September
        nthWeekdayHoliday(year, 9, Calendar.MONDAY, 1, "Labor Day", "👷", "US"),
        // Thanksgiving — 4th Thursday of November
        nthWeekdayHoliday(year, 11, Calendar.THURSDAY, 4, "Thanksgiving", "🦃", "US"),
    )

    private fun nthWeekdayHoliday(
        year: Int, month: Int, dayOfWeek: Int, n: Int,
        name: String, emoji: String, countryCode: String
    ): Holiday {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, 1)
            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) add(Calendar.DAY_OF_MONTH, 1)
            add(Calendar.DAY_OF_MONTH, (n - 1) * 7)
        }
        return Holiday(
            name = name,
            holidayType = HolidayType.ONE_TIME,
            year = year,
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            emoji = emoji,
            isImported = true,
            countryCode = countryCode
        )
    }

    private fun lastWeekdayHoliday(
        year: Int, month: Int, dayOfWeek: Int,
        name: String, emoji: String, countryCode: String
    ): Holiday {
        val cal = Calendar.getInstance().apply {
            set(year, month - 1, getActualMaximum(Calendar.DAY_OF_MONTH))
            while (get(Calendar.DAY_OF_WEEK) != dayOfWeek) add(Calendar.DAY_OF_MONTH, -1)
        }
        return Holiday(
            name = name,
            holidayType = HolidayType.ONE_TIME,
            year = year,
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            emoji = emoji,
            isImported = true,
            countryCode = countryCode
        )
    }
}
