package com.tddalarm.app.data

import com.tddalarm.app.data.entity.AlarmEntity
import com.tddalarm.app.data.entity.HolidayEntity
import com.tddalarm.app.data.entity.PtoPeriodEntity
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

    @Test
    fun `alarm with all rules round-trips through its entity`() {
        val alarm = Alarm(
            id = 7,
            label = "Church",
            hour = 8,
            minute = 15,
            repeatDays = setOf(DayOfWeek.SUNDAY, DayOfWeek.WEDNESDAY),
            enabled = true,
            skipOnDaysOff = true,
            locationRule = LocationRule(
                fence = GeoFence(GeoPoint(39.768403, -86.158068), 25_000.0),
                fireWhenLocationUnknown = false,
            ),
        )
        assertEquals(alarm, alarm.toEntity().toDomain())
    }

    @Test
    fun `plain alarm without rules round-trips`() {
        val alarm = Alarm(id = 1, label = "", hour = 6, minute = 30)
        assertEquals(alarm, alarm.toEntity().toDomain())
    }

    @Test
    fun `empty repeat days map to empty string and back`() {
        val entity = Alarm(id = 1, label = "x", hour = 6, minute = 0).toEntity()
        assertEquals("", entity.repeatDays)
        assertEquals(emptySet<DayOfWeek>(), entity.toDomain().repeatDays)
    }

    @Test
    fun `entity without location columns maps to null location rule`() {
        val entity = AlarmEntity(
            id = 3, label = "Work", hour = 6, minute = 30, repeatDays = "",
            enabled = true, skipOnDaysOff = true,
            locationLatitude = null, locationLongitude = null, locationRadiusMeters = null,
            fireWhenLocationUnknown = true,
        )
        assertNull(entity.toDomain().locationRule)
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
