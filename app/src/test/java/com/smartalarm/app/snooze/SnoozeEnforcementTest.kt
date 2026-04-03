package com.smartalarm.app.snooze

import android.content.Context
import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.repository.AlarmRepository
import com.smartalarm.app.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class SnoozeEnforcementTest {

    private lateinit var fakeDao: FakeAlarmDao
    private lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        fakeDao = FakeAlarmDao()
        repository = AlarmRepository(fakeDao)
    }

    // ── canSnooze helper (mirrors AlarmFiringActivity logic) ──────────────────

    private fun canSnooze(alarm: Alarm): Boolean = alarm.snoozeCount < alarm.snoozeMaxCount

    // ── snooze eligibility ────────────────────────────────────────────────────

    @Test
    fun `canSnooze returns true when snoozeCount is zero`() {
        val alarm = Alarm(label = "Test", hour = 7, minute = 0, snoozeCount = 0, snoozeMaxCount = 3)
        assertTrue(canSnooze(alarm))
    }

    @Test
    fun `canSnooze returns true when snoozeCount is below max`() {
        val alarm = Alarm(label = "Test", hour = 7, minute = 0, snoozeCount = 2, snoozeMaxCount = 3)
        assertTrue(canSnooze(alarm))
    }

    @Test
    fun `canSnooze returns false when snoozeCount equals max`() {
        val alarm = Alarm(label = "Test", hour = 7, minute = 0, snoozeCount = 3, snoozeMaxCount = 3)
        assertFalse(canSnooze(alarm))
    }

    @Test
    fun `canSnooze returns false when snoozeCount exceeds max`() {
        val alarm = Alarm(label = "Test", hour = 7, minute = 0, snoozeCount = 5, snoozeMaxCount = 3)
        assertFalse(canSnooze(alarm))
    }

    @Test
    fun `canSnooze returns false when max is zero`() {
        val alarm = Alarm(label = "Test", hour = 7, minute = 0, snoozeCount = 0, snoozeMaxCount = 0)
        assertFalse(canSnooze(alarm))
    }

    // ── repository snooze count operations ───────────────────────────────────

    @Test
    fun `incrementSnoozeCount increases count by one`() = runTest {
        val id = repository.insert(Alarm(label = "A", hour = 7, minute = 0, snoozeCount = 0))
        repository.incrementSnoozeCount(id)
        assertEquals(1, repository.getById(id)?.snoozeCount)
    }

    @Test
    fun `incrementSnoozeCount accumulates across multiple calls`() = runTest {
        val id = repository.insert(Alarm(label = "A", hour = 7, minute = 0, snoozeCount = 0))
        repository.incrementSnoozeCount(id)
        repository.incrementSnoozeCount(id)
        repository.incrementSnoozeCount(id)
        assertEquals(3, repository.getById(id)?.snoozeCount)
    }

    @Test
    fun `resetSnoozeCount sets count to zero`() = runTest {
        val id = repository.insert(Alarm(label = "A", hour = 7, minute = 0, snoozeCount = 2))
        repository.resetSnoozeCount(id)
        assertEquals(0, repository.getById(id)?.snoozeCount)
    }

    @Test
    fun `resetSnoozeCount on already zero count stays zero`() = runTest {
        val id = repository.insert(Alarm(label = "A", hour = 7, minute = 0, snoozeCount = 0))
        repository.resetSnoozeCount(id)
        assertEquals(0, repository.getById(id)?.snoozeCount)
    }

    @Test
    fun `snooze disabled after max snoozes reached`() = runTest {
        val id = repository.insert(
            Alarm(label = "A", hour = 7, minute = 0, snoozeCount = 0, snoozeMaxCount = 2)
        )
        repository.incrementSnoozeCount(id)
        repository.incrementSnoozeCount(id)
        val alarm = repository.getById(id)!!
        assertFalse(canSnooze(alarm))
    }

    @Test
    fun `alarm defaults have expected snooze values`() {
        val alarm = Alarm(label = "Default", hour = 8, minute = 0)
        assertEquals(9, alarm.snoozeDurationMinutes)
        assertEquals(3, alarm.snoozeMaxCount)
        assertEquals(0, alarm.snoozeCount)
    }
}

// ── Fakes ─────────────────────────────────────────────────────────────────────

private class FakeAlarmDao : AlarmDao {

    private val alarms = mutableListOf<Alarm>()
    private val flow = MutableStateFlow<List<Alarm>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(alarm: Alarm): Long {
        val id = if (alarm.id == 0L) nextId++ else alarm.id
        alarms.removeAll { it.id == id }
        alarms.add(alarm.copy(id = id))
        flow.value = alarms.toList()
        return id
    }

    override suspend fun delete(alarm: Alarm) {
        alarms.removeAll { it.id == alarm.id }
        flow.value = alarms.toList()
    }

    override fun getAll(): Flow<List<Alarm>> = flow

    override suspend fun getById(id: Long): Alarm? = alarms.find { it.id == id }

    override suspend fun setEnabled(id: Long, enabled: Boolean) {
        val idx = alarms.indexOfFirst { it.id == id }
        if (idx != -1) {
            alarms[idx] = alarms[idx].copy(enabled = enabled)
            flow.value = alarms.toList()
        }
    }

    override suspend fun setSnoozeCount(id: Long, count: Int) {
        val idx = alarms.indexOfFirst { it.id == id }
        if (idx != -1) {
            alarms[idx] = alarms[idx].copy(snoozeCount = count)
            flow.value = alarms.toList()
        }
    }
}

private class FakeAlarmScheduler : AlarmScheduler(mock(Context::class.java)) {
    override fun schedule(alarm: Alarm) {}
    override fun cancel(alarm: Alarm) {}
}
