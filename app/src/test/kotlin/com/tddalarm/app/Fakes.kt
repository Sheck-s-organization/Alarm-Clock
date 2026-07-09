package com.tddalarm.app

import com.tddalarm.app.data.HolidayEntry
import com.tddalarm.app.data.Place
import com.tddalarm.app.data.PtoEntry
import com.tddalarm.app.data.repo.AlarmSchedulerPort
import com.tddalarm.app.data.repo.AlarmStore
import com.tddalarm.app.data.repo.PlaceStore
import com.tddalarm.app.data.repo.WorkCalendarStore
import com.tddalarm.app.location.AddressResolver
import com.tddalarm.app.location.LocationProvider
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkCalendar
import com.tddalarm.core.schedule.WorkSchedule
import java.time.DayOfWeek
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

class FakeAlarmStore : AlarmStore {
    private val state = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override val alarms = state

    override suspend fun getById(id: Long): Alarm? = state.value.find { it.id == id }

    override suspend fun save(alarm: Alarm): Long {
        val stored = if (alarm.id == 0L) alarm.copy(id = nextId++) else alarm
        state.value = state.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filter { it.id != id }
    }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        state.value = state.value.map { if (it.id == id) it.copy(enabled = enabled) else it }
    }

    override suspend fun enabledAlarms(): List<Alarm> = state.value.filter { it.enabled }
}

class FakeWorkCalendarStore(
    initialDays: Set<DayOfWeek> = setOf(
        DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
        DayOfWeek.THURSDAY, DayOfWeek.FRIDAY,
    ),
) : WorkCalendarStore {
    private val days = MutableStateFlow(initialDays)
    private val pto = MutableStateFlow<List<PtoEntry>>(emptyList())
    private val hols = MutableStateFlow<List<HolidayEntry>>(emptyList())
    private var nextId = 1L

    override val workingDays = days
    override val ptoEntries = pto
    override val holidayEntries = hols
    override val calendar = days.map { buildCalendar() }

    private fun buildCalendar() = WorkCalendar(
        schedule = WorkSchedule(days.value),
        ptoPeriods = pto.value.map { it.range },
        holidays = hols.value.map { it.holiday },
    )

    override suspend fun currentCalendar(): WorkCalendar = buildCalendar()

    override suspend fun setWorkingDays(days: Set<DayOfWeek>) {
        this.days.value = days
    }

    override suspend fun addPto(range: DateRange) {
        pto.value = pto.value + PtoEntry(nextId++, range)
    }

    override suspend fun removePto(id: Long) {
        pto.value = pto.value.filter { it.id != id }
    }

    override suspend fun addHoliday(holiday: Holiday) {
        hols.value = hols.value + HolidayEntry(nextId++, holiday)
    }

    override suspend fun removeHoliday(id: Long) {
        hols.value = hols.value.filter { it.id != id }
    }
}

class FakeScheduler : AlarmSchedulerPort {
    val scheduled = mutableListOf<Alarm>()
    val cancelled = mutableListOf<Long>()

    override fun scheduleNext(alarm: Alarm) {
        scheduled += alarm
    }

    override fun cancel(alarmId: Long) {
        cancelled += alarmId
    }
}

class FakePlaceStore : PlaceStore {
    private val state = MutableStateFlow<List<Place>>(emptyList())
    private var nextId = 1L

    override val places = state

    override suspend fun getById(id: Long): Place? = state.value.find { it.id == id }

    override suspend fun save(place: Place): Long {
        val stored = if (place.id == 0L) place.copy(id = nextId++) else place
        state.value = state.value.filter { it.id != stored.id } + stored
        return stored.id
    }

    override suspend fun delete(id: Long) {
        state.value = state.value.filter { it.id != id }
    }
}

class FakeAddressResolver(
    private val known: Map<String, GeoPoint> = emptyMap(),
) : AddressResolver {
    override suspend fun resolve(query: String): GeoPoint? = known[query]
}

class FakeLocationProvider(var location: GeoPoint? = null) : LocationProvider {
    var queried = false
        private set

    override suspend fun lastKnown(): GeoPoint? {
        queried = true
        return location
    }
}
