package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.SmartAlarmApplication
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.data.entities.ChargingRequirement
import com.smartalarm.app.manager.MonthPeriod
import com.google.gson.Gson
import kotlinx.coroutines.launch

class AlarmViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as SmartAlarmApplication
    private val repo = app.alarmRepository
    private val manager = app.smartAlarmManager
    private val gson = Gson()

    val allAlarms: LiveData<List<Alarm>> = repo.allAlarmsFlow.asLiveData()

    private val _operationResult = MutableLiveData<OperationResult>()
    val operationResult: LiveData<OperationResult> = _operationResult

    // ---- CRUD ----

    fun saveAlarm(alarm: Alarm) = viewModelScope.launch {
        val id = if (alarm.id == 0L) {
            repo.insertAlarm(alarm)
        } else {
            repo.updateAlarm(alarm)
            alarm.id
        }
        // Schedule in AlarmManager
        val saved = repo.getAlarmById(id) ?: return@launch
        manager.scheduleAlarm(saved)
        _operationResult.value = OperationResult.Success("Alarm saved")
    }

    fun deleteAlarm(alarm: Alarm) = viewModelScope.launch {
        manager.cancelAlarm(alarm)
        repo.deleteAlarm(alarm)
        _operationResult.value = OperationResult.Success("Alarm deleted")
    }

    fun toggleAlarm(alarmId: Long, enabled: Boolean) = viewModelScope.launch {
        repo.setAlarmEnabled(alarmId, enabled)
        val alarm = repo.getAlarmById(alarmId) ?: return@launch
        if (enabled) {
            manager.scheduleAlarm(alarm)
        } else {
            manager.cancelAlarm(alarm)
        }
    }

    // ---- Factory helpers for smart alarm creation ----

    fun buildWorkScheduleAlarm(
        hour: Int,
        minute: Int,
        scheduleId: Long,
        label: String = "",
        chargingReq: ChargingRequirement = ChargingRequirement.NOT_REQUIRED,
        vibrate: Boolean = true
    ) = Alarm(
        hour = hour,
        minute = minute,
        alarmType = AlarmType.WORK_SCHEDULE,
        workScheduleId = scheduleId,
        label = label,
        chargingRequirement = chargingReq,
        vibrate = vibrate
    )

    fun buildTimeOfMonthAlarm(
        hour: Int,
        minute: Int,
        periods: List<MonthPeriod>,
        label: String = ""
    ) = Alarm(
        hour = hour,
        minute = minute,
        alarmType = AlarmType.TIME_OF_MONTH,
        monthPeriodsJson = gson.toJson(periods),
        label = label
    )

    fun buildChargingAlarm(
        hour: Int,
        minute: Int,
        requirement: ChargingRequirement,
        label: String = "",
        repeatDays: Set<Int> = com.smartalarm.app.util.Constants.WEEKDAYS
    ) = Alarm(
        hour = hour,
        minute = minute,
        alarmType = AlarmType.CHARGING,
        chargingRequirement = requirement,
        repeatDays = repeatDays,
        label = label
    )
}

sealed class OperationResult {
    data class Success(val message: String) : OperationResult()
    data class Error(val message: String) : OperationResult()
}
