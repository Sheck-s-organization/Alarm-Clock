package com.tddalarm.app.data

import com.tddalarm.app.data.entity.AlarmEntity
import com.tddalarm.app.data.entity.HolidayEntity
import com.tddalarm.app.data.entity.PtoPeriodEntity
import com.tddalarm.app.data.entity.SavedLocationEntity
import com.tddalarm.app.data.entity.WorkScheduleEntity
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

class MappersTest {

    private val homeEntity = SavedLocationEntity(
        id = 7, name = "Home",
        latitude = 39.768403, longitude = -86.158068, radiusMeters = 25_000.0,
    )
    private val placesById = mapOf(homeEntity.id to homeEntity)

    @Test
    fun `saved location entity round-trips through its domain place`() {
        val place = homeEntity.toDomain()
        assertEquals("Home", place.name)
        assertEquals(GeoPoint(39.768403, -86.158068), place.fence.center)
        assertEquals(25_000.0, place.fence.radiusMeters, 0.0)
        assertEquals(homeEntity, place.toEntity())
    }

    @Test
    fun `alarm referencing a saved place resolves its fence from the place`() {
        val alarm = Alarm(
            id = 2,
            label = "Church",
            hour = 8,
            minute = 15,
            repeatDays = setOf(DayOfWeek.SUNDAY),
            skipOnDaysOff = false,
            locationRule = LocationRule(
                fence = GeoFence(GeoPoint(39.768403, -86.158068), 25_000.0),
                fireWhenLocationUnknown = false,
                placeId = 7,
            ),
        )
        val entity = alarm.toEntity()
        assertEquals(7L, entity.locationPlaceId)
        assertEquals(alarm, entity.toDomain(placesById))
    }

    @Test
    fun `alarm whose saved place was deleted loses its location rule`() {
        val entity = AlarmEntity(
            id = 3, label = "Church", hour = 8, minute = 0, repeatDays = "SUNDAY",
            enabled = true, skipOnDaysOff = false,
            locationPlaceId = 99, fireWhenLocationUnknown = true,
        )
        assertNull(entity.toDomain(placesById).locationRule)
    }

    @Test
    fun `alarm without a place reference has no location rule`() {
        val entity = AlarmEntity(
            id = 3, label = "Work", hour = 6, minute = 30, repeatDays = "",
            enabled = true, skipOnDaysOff = true,
            locationPlaceId = null, fireWhenLocationUnknown = true,
        )
        assertNull(entity.toDomain(placesById).locationRule)
    }

    @Test
    fun `plain alarm without rules round-trips`() {
        val alarm = Alarm(id = 1, label = "", hour = 6, minute = 30)
        assertEquals(alarm, alarm.toEntity().toDomain(placesById))
    }

    @Test
    fun `empty repeat days map to empty string and back`() {
        val entity = Alarm(id = 1, label = "x", hour = 6, minute = 0).toEntity()
        assertEquals("", entity.repeatDays)
        assertEquals(emptySet<DayOfWeek>(), entity.toDomain(placesById).repeatDays)
    }

    @Test
    fun `work schedule round-trips`() {
        val days = setOf(DayOfWeek.MONDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
        val entity = WorkScheduleEntity(workingDays = days.toStorageString())
        assertEquals(days, entity.toDomain().workingDays)
    }

    @Test
    fun `pto period round-trips`() {
        val range = DateRange(LocalDate.of(2026, 7, 6), LocalDate.of(2026, 7, 10))
        assertEquals(range, range.toEntity().toDomain())
    }

    @Test
    fun `one-time holiday round-trips`() {
        val holiday = Holiday.OneTime("Good Friday", LocalDate.of(2026, 4, 3))
        assertEquals(holiday, holiday.toEntity().toDomain())
    }

    @Test
    fun `annual holiday round-trips`() {
        val holiday = Holiday.Annual("Christmas", Month.DECEMBER, 25)
        assertEquals(holiday, holiday.toEntity().toDomain())
    }

    @Test
    fun `pto entity exposes its row id for deletion`() {
        val entity = PtoPeriodEntity(id = 42, startEpochDay = 100, endEpochDay = 105)
        assertEquals(42, entity.toEntry().id)
        assertEquals(entity.toDomain(), entity.toEntry().range)
    }

    @Test
    fun `holiday entity exposes its row id for deletion`() {
        val entity = HolidayEntity(
            id = 9, name = "Christmas", annual = true,
            epochDay = null, month = 12, dayOfMonth = 25,
        )
        assertEquals(9, entity.toEntry().id)
        assertEquals(Holiday.Annual("Christmas", Month.DECEMBER, 25), entity.toEntry().holiday)
    }
}
