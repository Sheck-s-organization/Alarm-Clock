package com.smartalarm.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.smartalarm.app.data.entities.Holiday
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.data.repository.HolidayRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HolidayViewModel(
    application: Application,
    private val repository: HolidayRepository
) : AndroidViewModel(application) {

    val allHolidays: StateFlow<List<Holiday>> = repository.allHolidays
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    fun addHoliday(name: String, month: Int, day: Int, year: Int?, type: HolidayType) {
        viewModelScope.launch {
            repository.insert(Holiday(name = name, month = month, day = day, year = year, type = type))
        }
    }

    fun deleteHoliday(holiday: Holiday) {
        viewModelScope.launch {
            repository.delete(holiday)
        }
    }

    fun importUsHolidays() {
        viewModelScope.launch {
            repository.importUsHolidays()
        }
    }
}

class HolidayViewModelFactory(
    private val application: Application,
    private val repository: HolidayRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HolidayViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HolidayViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
