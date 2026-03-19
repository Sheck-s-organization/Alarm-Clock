package com.smartalarm.app.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class AlarmSchedulerTest {

    @Test
    fun `nextTriggerTimeMillis returns same day when alarm is in the future`() {
        val now = fixedTime(hour = 6, minute = 0)
        val trigger = AlarmScheduler.nextTriggerTimeMillis(7, 30, now)
        val triggerCal = calendarOf(trigger)
        val nowCal = calendarOf(now)
        assertEquals(7, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
        assertEquals(nowCal.get(Calendar.DAY_OF_MONTH), triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(nowCal.get(Calendar.MONTH), triggerCal.get(Calendar.MONTH))
    }

    @Test
    fun `nextTriggerTimeMillis returns next day when alarm time has already passed`() {
        val now = fixedTime(hour = 8, minute = 0)
        val trigger = AlarmScheduler.nextTriggerTimeMillis(7, 0, now)
        val triggerCal = calendarOf(trigger)
        val expectedDate = calendarOf(now).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(7, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(expectedDate.get(Calendar.MONTH), triggerCal.get(Calendar.MONTH))
    }

    @Test
    fun `nextTriggerTimeMillis returns next day when alarm is at exact current time`() {
        val now = fixedTime(hour = 7, minute = 30)
        val trigger = AlarmScheduler.nextTriggerTimeMillis(7, 30, now)
        val triggerCal = calendarOf(trigger)
        val expectedDate = calendarOf(now).apply { add(Calendar.DAY_OF_YEAR, 1) }
        assertEquals(expectedDate.get(Calendar.DAY_OF_MONTH), triggerCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(expectedDate.get(Calendar.MONTH), triggerCal.get(Calendar.MONTH))
    }

    @Test
    fun `nextTriggerTimeMillis trigger is always strictly in the future`() {
        val now = System.currentTimeMillis()
        val trigger = AlarmScheduler.nextTriggerTimeMillis(7, 0, now)
        assertTrue("Trigger must be in the future", trigger > now)
    }

    @Test
    fun `nextTriggerTimeMillis sets seconds and milliseconds to zero`() {
        val now = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 45)
            set(Calendar.MILLISECOND, 500)
        }.timeInMillis
        val trigger = AlarmScheduler.nextTriggerTimeMillis(7, 0, now)
        val triggerCal = calendarOf(trigger)
        assertEquals(0, triggerCal.get(Calendar.SECOND))
        assertEquals(0, triggerCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun `nextTriggerTimeMillis midnight alarm is scheduled for today when now is before midnight`() {
        val now = fixedTime(hour = 23, minute = 0)
        val trigger = AlarmScheduler.nextTriggerTimeMillis(23, 59, now)
        val triggerCal = calendarOf(trigger)
        val nowCal = calendarOf(now)
        assertEquals(23, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, triggerCal.get(Calendar.MINUTE))
        assertEquals(nowCal.get(Calendar.DAY_OF_MONTH), triggerCal.get(Calendar.DAY_OF_MONTH))
    }

    // ---- helpers ----

    private fun fixedTime(hour: Int, minute: Int): Long =
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun calendarOf(millis: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = millis }
}
