package com.tddalarm.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tddalarm.app.AppContainer

class ViewModelFactory(private val container: AppContainer) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = when {
        modelClass.isAssignableFrom(AlarmListViewModel::class.java) ->
            AlarmListViewModel(
                container.alarmStore, container.calendarStore,
                container.placeStore, container.scheduler,
            ) as T
        modelClass.isAssignableFrom(WorkScheduleViewModel::class.java) ->
            WorkScheduleViewModel(container.calendarStore) as T
        modelClass.isAssignableFrom(LocationsViewModel::class.java) ->
            LocationsViewModel(
                container.placeStore, container.addressResolver, container.locationProvider,
            ) as T
        else -> throw IllegalArgumentException("Unknown ViewModel: ${modelClass.name}")
    }
}
