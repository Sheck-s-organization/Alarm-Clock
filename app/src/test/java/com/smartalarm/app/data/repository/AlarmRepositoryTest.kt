package com.smartalarm.app.data.repository

import android.content.Context
import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.scheduler.AlarmScheduler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class AlarmRepositoryTest {

    private lateinit var fakeDao: FakeAlarmDao
    private lateinit var repository: AlarmRepository

    @Before
    fun setUp() {
        fakeDao = FakeAlarmDao()
        repository = AlarmRepository(fakeDao)
    }

    @Test
    fun `insert returns assigned id`() = runTest {
        val alarm = Alarm(label = "Wake up", hour = 7, minute = 0)
        val id = repository.insert(alarm)
        assertEquals(1L, id)
    }

    @Test
    fun `allAlarms emits inserted alarm`() = runTest {
        val alarm = Alarm(label = "Morning", hour = 6, minute = 30)
        repository.insert(alarm)
        val list = repository.allAlarms.first()
        assertEquals(1, list.size)
        assertEquals("Morning", list[0].label)
    }

    @Test
    fun `delete removes alarm from list`() = runTest {
        val id = repository.insert(Alarm(label = "To delete", hour = 8, minute = 0))
        val inserted = repository.getById(id)!!
        repository.delete(inserted)
        val list = repository.allAlarms.first()
        assertEquals(0, list.size)
    }

    @Test
    fun `getById returns correct alarm`() = runTest {
        val id = repository.insert(Alarm(label = "Exact", hour = 9, minute = 15))
        val found = repository.getById(id)
        assertEquals("Exact", found?.label)
        assertEquals(9, found?.hour)
        assertEquals(15, found?.minute)
    }

    @Test
    fun `getById returns null when id not found`() = runTest {
        val result = repository.getById(999L)
        assertNull(result)
    }

    @Test
    fun `insert multiple alarms tracked separately`() = runTest {
        repository.insert(Alarm(label = "First", hour = 6, minute = 0))
        repository.insert(Alarm(label = "Second", hour = 7, minute = 0))
        val list = repository.allAlarms.first()
        assertEquals(2, list.size)
    }

    @Test
    fun `setEnabled enables alarm and calls scheduler schedule`() = runTest {
        val id = repository.insert(Alarm(label = "Test", hour = 7, minute = 0, enabled = false))
        val fakeScheduler = FakeAlarmScheduler()
        repository.setEnabled(id, true, fakeScheduler)
        assertEquals(true, repository.getById(id)?.enabled)
        assertTrue(fakeScheduler.scheduledIds.contains(id))
    }

    @Test
    fun `setEnabled disables alarm and calls scheduler cancel`() = runTest {
        val id = repository.insert(Alarm(label = "Test", hour = 7, minute = 0, enabled = true))
        val fakeScheduler = FakeAlarmScheduler()
        repository.setEnabled(id, false, fakeScheduler)
        assertEquals(false, repository.getById(id)?.enabled)
        assertTrue(fakeScheduler.cancelledIds.contains(id))
    }
}

/** In-memory fake DAO — no Android/Room dependencies. */
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
        val index = alarms.indexOfFirst { it.id == id }
        if (index != -1) {
            alarms[index] = alarms[index].copy(enabled = enabled)
            flow.value = alarms.toList()
        }
    }
}

/** Fake AlarmScheduler for testing — records which alarm IDs were scheduled or cancelled. */
private class FakeAlarmScheduler : AlarmScheduler(mock(Context::class.java)) {
    val scheduledIds = mutableListOf<Long>()
    val cancelledIds = mutableListOf<Long>()
    override fun schedule(alarm: Alarm) { scheduledIds.add(alarm.id) }
    override fun cancel(alarm: Alarm) { cancelledIds.add(alarm.id) }
}
