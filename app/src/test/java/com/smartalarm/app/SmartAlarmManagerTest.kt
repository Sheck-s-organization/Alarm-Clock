package com.smartalarm.app

import com.smartalarm.app.data.entities.ScheduleType
import com.smartalarm.app.data.entities.WorkSchedule
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

/**
 * Unit tests for work day / holiday logic that can run without Android framework.
 */
class SmartAlarmManagerTest {

    // Helper: create a WorkSchedule with fixed days Mon-Fri
    private fun weekdaySchedule() = WorkSchedule(
        id = 1L,
        name = "Standard",
        scheduleType = ScheduleType.FIXED_DAYS,
        workDays = setOf(1, 2, 3, 4, 5),
        alarmHour = 7,
        alarmMinute = 0
    )

    @Test
    fun `monday is a work day for weekday schedule`() {
        val schedule = weekdaySchedule()
        val monday = calendarForIsoDay(Calendar.MONDAY)
        val isoDow = toIsoDayOfWeek(monday.get(Calendar.DAY_OF_WEEK))
        assertTrue(isoDow in schedule.workDays)
    }

    @Test
    fun `saturday is not a work day for weekday schedule`() {
        val schedule = weekdaySchedule()
        val saturday = calendarForIsoDay(Calendar.SATURDAY)
        val isoDow = toIsoDayOfWeek(saturday.get(Calendar.DAY_OF_WEEK))
        assertFalse(isoDow in schedule.workDays)
    }

    @Test
    fun `sunday is not a work day for weekday schedule`() {
        val schedule = weekdaySchedule()
        // ISO Sunday = 7
        assertFalse(7 in schedule.workDays)
    }

    @Test
    fun `rotating 4-on-4-off cycle work day calculation`() {
        // 4 work + 4 off. Start day is "day 0" = work day.
        val cycleDays = 8
        fun isWorkDay(diffDays: Int): Boolean {
            val pos = diffDays % cycleDays
            return pos < 4
        }

        // Days 0-3 should be work days
        for (i in 0..3) assertTrue("Day $i should be work day", isWorkDay(i))
        // Days 4-7 should be off
        for (i in 4..7) assertFalse("Day $i should be off", isWorkDay(i))
        // Day 8 wraps back to work
        assertTrue(isWorkDay(8))
    }

    @Test
    fun `day 15 falls in second half of month period`() {
        val startDay = 15
        val endDay = 31
        val dayOfMonth = 15
        assertTrue(dayOfMonth in startDay..endDay)
    }

    @Test
    fun `day 14 does not fall in second half period starting 15`() {
        val startDay = 15
        val endDay = 31
        val dayOfMonth = 14
        assertFalse(dayOfMonth in startDay..endDay)
    }

    // ---- Helpers ----

    private fun toIsoDayOfWeek(javaDow: Int): Int =
        if (javaDow == Calendar.SUNDAY) 7 else javaDow - 1

    private fun calendarForIsoDay(javaDay: Int): Calendar {
        val cal = Calendar.getInstance()
        while (cal.get(Calendar.DAY_OF_WEEK) != javaDay) cal.add(Calendar.DAY_OF_MONTH, 1)
        return cal
    }
}
