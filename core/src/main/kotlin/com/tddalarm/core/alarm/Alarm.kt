package com.tddalarm.core.alarm

import com.tddalarm.core.geo.GeoFence
import java.time.DayOfWeek

/**
 * Restricts an alarm to a place: it only fires when the device is inside [fence].
 * [fireWhenLocationUnknown] decides the fail-safe when no location is available —
 * the default is to fire rather than silently stay quiet.
 * [placeId] identifies the saved place the fence was resolved from, when there is one,
 * so persistence can store a reference instead of raw coordinates.
 */
data class LocationRule(
    val fence: GeoFence,
    val fireWhenLocationUnknown: Boolean = true,
    val placeId: Long? = null,
)

/**
 * @param repeatDays weekdays the alarm repeats on; empty means every day.
 * @param skipOnDaysOff when true, the alarm stays silent on non-working days,
 *   PTO days and holidays according to the user's work calendar.
 * @param locationRule when set, the alarm only fires inside the given fence.
 */
data class Alarm(
    val id: Long = 0,
    val label: String = "",
    val hour: Int,
    val minute: Int,
    val repeatDays: Set<DayOfWeek> = emptySet(),
    val enabled: Boolean = true,
    val skipOnDaysOff: Boolean = false,
    val locationRule: LocationRule? = null,
) {
    init {
        require(hour in 0..23) { "hour must be 0..23, was $hour" }
        require(minute in 0..59) { "minute must be 0..59, was $minute" }
    }
}
