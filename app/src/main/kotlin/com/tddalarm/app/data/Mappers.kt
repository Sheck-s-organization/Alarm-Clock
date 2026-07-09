package com.tddalarm.app.data

import com.tddalarm.app.data.entity.AlarmEntity
import com.tddalarm.app.data.entity.HolidayEntity
import com.tddalarm.app.data.entity.PtoPeriodEntity
import com.tddalarm.app.data.entity.SavedLocationEntity
import com.tddalarm.app.data.entity.WorkScheduleEntity
import com.tddalarm.core.alarm.Alarm
import com.tddalarm.core.alarm.LocationRule
import com.tddalarm.core.geo.GeoFence
import com.tddalarm.core.geo.GeoPoint
import com.tddalarm.core.schedule.DateRange
import com.tddalarm.core.schedule.Holiday
import com.tddalarm.core.schedule.WorkSchedule
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month

/** A named, saved place an alarm can be restricted to. */
data class Place(val id: Long, val name: String, val fence: GeoFence)

/** A stored PTO period together with its row id, so the UI can delete it. */
data class PtoEntry(val id: Long, val range: DateRange)

/** A stored holiday together with its row id, so the UI can delete it. */
data class HolidayEntry(val id: Long, val holiday: Holiday)

fun Set<DayOfWeek>.toStorageString(): String = sortedBy { it.value }.joinToString(",") { it.name }

fun daysFromStorageString(value: String): Set<DayOfWeek> =
    if (value.isEmpty()) emptySet()
    else value.split(",").map { DayOfWeek.valueOf(it) }.toSet()

fun SavedLocationEntity.toDomain(): Place = Place(
    id = id,
    name = name,
    fence = GeoFence(GeoPoint(latitude, longitude), radiusMeters),
)

fun Place.toEntity(): SavedLocationEntity = SavedLocationEntity(
    id = id,
    name = name,
    latitude = fence.center.latitude,
    longitude = fence.center.longitude,
    radiusMeters = fence.radiusMeters,
)

fun Alarm.toEntity(): AlarmEntity = AlarmEntity(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = repeatDays.toStorageString(),
    enabled = enabled,
    skipOnDaysOff = skipOnDaysOff,
    locationPlaceId = locationRule?.placeId,
    fireWhenLocationUnknown = locationRule?.fireWhenLocationUnknown ?: true,
)

/**
 * Rebuilds the domain alarm, resolving its saved-place reference to a concrete
 * fence. A dangling reference (place deleted) drops the rule: the alarm then
 * rings everywhere rather than never.
 */
fun AlarmEntity.toDomain(placesById: Map<Long, SavedLocationEntity>): Alarm = Alarm(
    id = id,
    label = label,
    hour = hour,
    minute = minute,
    repeatDays = daysFromStorageString(repeatDays),
    enabled = enabled,
    skipOnDaysOff = skipOnDaysOff,
    locationRule = locationPlaceId?.let { placeId ->
        placesById[placeId]?.let { place ->
            LocationRule(
                fence = place.toDomain().fence,
                fireWhenLocationUnknown = fireWhenLocationUnknown,
                placeId = placeId,
            )
        }
    },
)

fun WorkScheduleEntity.toDomain(): WorkSchedule = WorkSchedule(daysFromStorageString(workingDays))

fun DateRange.toEntity(id: Long = 0): PtoPeriodEntity = PtoPeriodEntity(
    id = id,
    startEpochDay = start.toEpochDay(),
    endEpochDay = endInclusive.toEpochDay(),
)

fun PtoPeriodEntity.toDomain(): DateRange = DateRange(
    start = LocalDate.ofEpochDay(startEpochDay),
    endInclusive = LocalDate.ofEpochDay(endEpochDay),
)

fun PtoPeriodEntity.toEntry(): PtoEntry = PtoEntry(id, toDomain())

fun Holiday.toEntity(id: Long = 0): HolidayEntity = when (this) {
    is Holiday.OneTime -> HolidayEntity(
        id = id, name = name, annual = false,
        epochDay = date.toEpochDay(), month = null, dayOfMonth = null,
    )
    is Holiday.Annual -> HolidayEntity(
        id = id, name = name, annual = true,
        epochDay = null, month = month.value, dayOfMonth = dayOfMonth,
    )
}

fun HolidayEntity.toDomain(): Holiday =
    if (annual) {
        Holiday.Annual(name, Month.of(requireNotNull(month)), requireNotNull(dayOfMonth))
    } else {
        Holiday.OneTime(name, LocalDate.ofEpochDay(requireNotNull(epochDay)))
    }

fun HolidayEntity.toEntry(): HolidayEntry = HolidayEntry(id, toDomain())
