package com.smartalarm.app.receiver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AlarmReceiverTest {

    @Test
    fun `alarmIdFromIntent returns id for a valid alarm id`() {
        assertEquals(42L, AlarmReceiver.alarmIdFromIntent(42L))
    }

    @Test
    fun `alarmIdFromIntent returns null for invalid id minus one`() {
        assertNull(AlarmReceiver.alarmIdFromIntent(-1L))
    }

    @Test
    fun `alarmIdFromIntent returns id of one`() {
        assertEquals(1L, AlarmReceiver.alarmIdFromIntent(1L))
    }

    @Test
    fun `alarmIdFromIntent returns id of zero`() {
        assertEquals(0L, AlarmReceiver.alarmIdFromIntent(0L))
    }

    @Test
    fun `alarmIdFromIntent returns large id`() {
        assertEquals(Long.MAX_VALUE, AlarmReceiver.alarmIdFromIntent(Long.MAX_VALUE))
    }
}
