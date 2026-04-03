package com.smartalarm.app.scheduler

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnoozeSchedulerTest {

    @Test
    fun `snoozeTriggerTimeMillis returns now plus duration in millis`() {
        val now = 1_000_000L
        val durationMinutes = 9
        val trigger = AlarmScheduler.snoozeTriggerTimeMillis(durationMinutes, now)
        assertEquals(now + 9 * 60_000L, trigger)
    }

    @Test
    fun `snoozeTriggerTimeMillis with 5 minutes adds 5 minutes`() {
        val now = 0L
        val trigger = AlarmScheduler.snoozeTriggerTimeMillis(5, now)
        assertEquals(5 * 60_000L, trigger)
    }

    @Test
    fun `snoozeTriggerTimeMillis result is strictly in the future`() {
        val now = System.currentTimeMillis()
        val trigger = AlarmScheduler.snoozeTriggerTimeMillis(1, now)
        assertTrue("Snooze trigger must be in the future", trigger > now)
    }

    @Test
    fun `snoozeTriggerTimeMillis with zero duration returns now`() {
        val now = 5_000L
        val trigger = AlarmScheduler.snoozeTriggerTimeMillis(0, now)
        assertEquals(now, trigger)
    }

    @Test
    fun `snoozeTriggerTimeMillis with 1 minute is 60 seconds ahead`() {
        val now = 10_000L
        val trigger = AlarmScheduler.snoozeTriggerTimeMillis(1, now)
        assertEquals(now + 60_000L, trigger)
    }
}
