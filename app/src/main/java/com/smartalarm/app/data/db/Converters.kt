package com.smartalarm.app.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartalarm.app.data.entities.AlarmEvent
import com.smartalarm.app.data.entities.AlarmType
import com.smartalarm.app.data.entities.ChargingRequirement
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.data.entities.LocationTrigger
import com.smartalarm.app.data.entities.ScheduleType
import com.smartalarm.app.data.entities.SkipReason

class Converters {

    private val gson = Gson()

    // Set<Int> <-> String (comma-separated)
    @TypeConverter
    fun fromIntSet(value: Set<Int>?): String =
        value?.joinToString(",") ?: ""

    @TypeConverter
    fun toIntSet(value: String?): Set<Int> =
        value?.takeIf { it.isNotBlank() }
            ?.split(",")
            ?.mapNotNull { it.trim().toIntOrNull() }
            ?.toSet() ?: emptySet()

    // Enums
    @TypeConverter fun fromAlarmType(v: AlarmType): String = v.name
    @TypeConverter fun toAlarmType(v: String): AlarmType = AlarmType.valueOf(v)

    @TypeConverter fun fromChargingReq(v: ChargingRequirement): String = v.name
    @TypeConverter fun toChargingReq(v: String): ChargingRequirement = ChargingRequirement.valueOf(v)

    @TypeConverter fun fromScheduleType(v: ScheduleType): String = v.name
    @TypeConverter fun toScheduleType(v: String): ScheduleType = ScheduleType.valueOf(v)

    @TypeConverter fun fromHolidayType(v: HolidayType): String = v.name
    @TypeConverter fun toHolidayType(v: String): HolidayType = HolidayType.valueOf(v)

    @TypeConverter fun fromLocationTrigger(v: LocationTrigger): String = v.name
    @TypeConverter fun toLocationTrigger(v: String): LocationTrigger = LocationTrigger.valueOf(v)

    @TypeConverter fun fromAlarmEvent(v: AlarmEvent): String = v.name
    @TypeConverter fun toAlarmEvent(v: String): AlarmEvent = AlarmEvent.valueOf(v)

    @TypeConverter fun fromSkipReason(v: SkipReason?): String? = v?.name
    @TypeConverter fun toSkipReason(v: String?): SkipReason? = v?.let { SkipReason.valueOf(it) }
}
