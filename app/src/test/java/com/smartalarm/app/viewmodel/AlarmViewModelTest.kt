package com.smartalarm.app.viewmodel

import com.smartalarm.app.data.dao.AlarmDao
import com.smartalarm.app.data.entities.Alarm
import com.smartalarm.app.data.repository.AlarmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeAlarmDao
    private lateinit var repository: AlarmRepository
    private lateinit var viewModel: AlarmViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAlarmDao()
        repository = AlarmRepository(fakeDao)
        viewModel = AlarmViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `allAlarms is initially empty`() {
        assertEquals(0, viewModel.allAlarms.value.size)
    }

    @Test
    fun `addAlarm inserts alarm into list`() = runTest {
        viewModel.addAlarm(7, 30, "Morning")
        val alarms = viewModel.allAlarms.first { it.isNotEmpty() }
        assertEquals(1, alarms.size)
        assertEquals(7, alarms[0].hour)
        assertEquals(30, alarms[0].minute)
        assertEquals("Morning", alarms[0].label)
    }

    @Test
    fun `addAlarm uses default label when none provided`() = runTest {
        viewModel.addAlarm(6, 0)
        val alarms = viewModel.allAlarms.first { it.isNotEmpty() }
        assertEquals("Alarm", alarms[0].label)
    }

    @Test
    fun `deleteAlarm removes alarm from list`() = runTest {
        viewModel.addAlarm(8, 0, "To Delete")
        val alarm = viewModel.allAlarms.first { it.isNotEmpty() }[0]
        viewModel.deleteAlarm(alarm)
        val alarms = viewModel.allAlarms.first { it.isEmpty() }
        assertEquals(0, alarms.size)
    }

    @Test
    fun `allAlarms reflects multiple alarms`() = runTest {
        viewModel.addAlarm(6, 0, "First")
        viewModel.addAlarm(7, 0, "Second")
        val alarms = viewModel.allAlarms.first { it.size == 2 }
        assertEquals(2, alarms.size)
    }
}

/** In-memory fake DAO — mirrors the one in AlarmRepositoryTest, no Android/Room deps. */
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
}
