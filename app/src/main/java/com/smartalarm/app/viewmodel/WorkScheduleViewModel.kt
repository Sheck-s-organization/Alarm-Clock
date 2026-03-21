package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.data.entities.WorkSchedule
import com.smartalarm.app.data.repository.WorkScheduleRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class WorkScheduleViewModel(
    application: Application,
    private val repository: WorkScheduleRepository
) : AndroidViewModel(application) {

    val allSchedules: StateFlow<List<WorkSchedule>> = repository.allSchedules
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addSchedule(name: String, workDays: Set<Int>) {
        viewModelScope.launch {
            repository.insert(WorkSchedule(name = name, workDays = workDays))
        }
    }

    fun updateSchedule(schedule: WorkSchedule) {
        viewModelScope.launch {
            repository.insert(schedule)
        }
    }

    fun deleteSchedule(schedule: WorkSchedule) {
        viewModelScope.launch {
            repository.delete(schedule)
        }
    }
}

class WorkScheduleViewModelFactory(
    private val application: Application,
    private val repository: WorkScheduleRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WorkScheduleViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WorkScheduleViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
