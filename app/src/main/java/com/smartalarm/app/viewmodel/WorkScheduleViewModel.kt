package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.*
import kotlinx.coroutines.launch

class WorkScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmartAlarmApplication
    private val repo = app.workScheduleRepository
    private val alarmManager = app.smartAlarmManager
    private val gson = Gson()

    val allSchedules: LiveData<List<WorkSchedule>> = repo.allSchedulesFlow.asLiveData()
    val activeSchedule: LiveData<WorkSchedule?> = repo.activeScheduleFlow.asLiveData()

    private val _result = MutableLiveData<OperationResult>()
    val result: LiveData<OperationResult> = _result

    fun saveSchedule(schedule: WorkSchedule) = viewModelScope.launch {
        if (schedule.id == 0L) {
            repo.insertSchedule(schedule)
        } else {
            repo.updateSchedule(schedule)
        }
        // Reschedule all alarms tied to this schedule
        alarmManager.scheduleAllAlarms()
        _result.value = OperationResult.Success("Schedule saved")
    }

    fun deleteSchedule(schedule: WorkSchedule) = viewModelScope.launch {
        repo.deleteSchedule(schedule)
        alarmManager.scheduleAllAlarms()
        _result.value = OperationResult.Success("Schedule deleted")
    }

    fun setActiveSchedule(id: Long) = viewModelScope.launch {
        repo.setActiveSchedule(id)
        alarmManager.scheduleAllAlarms()
    }

    /** Build a fixed-days work schedule */
    fun buildFixedSchedule(
        name: String,
        workDays: Set<Int>,
        alarmHour: Int,
        alarmMinute: Int,
        monthlyOverrides: List<MonthlyTimeOverride> = emptyList(),
        dayOverrides: List<DayTimeOverride> = emptyList()
    ) = WorkSchedule(
        name = name,
        scheduleType = ScheduleType.FIXED_DAYS,
        workDays = workDays,
        alarmHour = alarmHour,
        alarmMinute = alarmMinute,
        monthlyOverridesJson = if (monthlyOverrides.isNotEmpty()) gson.toJson(monthlyOverrides) else null,
        dayOverridesJson = if (dayOverrides.isNotEmpty()) gson.toJson(dayOverrides) else null
    )

    /** Build a rotating shift schedule (e.g., 4-on-4-off) */
    fun buildRotatingSchedule(
        name: String,
        cycles: List<ShiftCycle>,
        rotationStartDate: Long,
        alarmHour: Int,
        alarmMinute: Int
    ) = WorkSchedule(
        name = name,
        scheduleType = ScheduleType.ROTATING,
        alarmHour = alarmHour,
        alarmMinute = alarmMinute,
        shiftCyclesJson = gson.toJson(cycles),
        rotationStartDate = rotationStartDate
    )
}
