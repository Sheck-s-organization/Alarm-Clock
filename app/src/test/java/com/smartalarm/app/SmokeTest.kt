package com.smartalarm.app

import org.junit.Assert.assertEquals
import org.junit.Test

class SmokeTest {
    @Test
    fun testRunnerWorks() {
        assertEquals(2, 1 + 1)
    }

    @Test
    fun intentionallyFailing() {
        assertEquals(3, 1 + 1)
    }
}
