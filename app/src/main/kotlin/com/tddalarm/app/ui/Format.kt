package com.tddalarm.app.ui

import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

fun formatClockTime(hour: Int, minute: Int): String =
    String.format(Locale.getDefault(), "%02d:%02d", hour, minute)

private val NEXT_FIRING_FORMAT = DateTimeFormatter.ofPattern("EEE, MMM d HH:mm")

fun LocalDateTime.toNextFiringText(): String = format(NEXT_FIRING_FORMAT)

fun Set<DayOfWeek>.toShortNames(locale: Locale = Locale.getDefault()): String =
    sortedBy { it.value }.joinToString(" ") { it.getDisplayName(TextStyle.SHORT, locale) }
