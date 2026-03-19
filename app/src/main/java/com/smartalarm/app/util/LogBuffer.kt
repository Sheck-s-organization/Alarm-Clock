package com.smartalarm.app.util

import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * In-app log buffer. Stores the most recent [MAX_ENTRIES] log events so they can be
 * displayed on the Logs screen without needing ADB.
 */
object LogBuffer {

    const val MAX_ENTRIES = 200

    enum class Level { DEBUG, ERROR }

    data class Entry(
        val timestampMillis: Long = System.currentTimeMillis(),
        val tag: String,
        val level: Level,
        val message: String
    )

    private val _entries = MutableStateFlow<List<Entry>>(emptyList())
    val entries: StateFlow<List<Entry>> = _entries.asStateFlow()

    fun d(tag: String, message: String) {
        Log.d(tag, message)
        append(Entry(tag = tag, level = Level.DEBUG, message = message))
    }

    fun e(tag: String, message: String) {
        Log.e(tag, message)
        append(Entry(tag = tag, level = Level.ERROR, message = message))
    }

    fun clear() {
        _entries.value = emptyList()
    }

    private fun append(entry: Entry) {
        val updated = (_entries.value + entry).takeLast(MAX_ENTRIES)
        _entries.value = updated
    }
}
