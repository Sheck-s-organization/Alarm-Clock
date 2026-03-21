package com.smartalarm.app.data.db

import androidx.room.TypeConverter
import com.smartalarm.app.data.entities.HolidayType

class Converters {

    @TypeConverter
    fun fromIntSet(set: Set<Int>): String = set.sorted().joinToString(",")

    @TypeConverter
    fun toIntSet(value: String): Set<Int> {
        if (value.isBlank()) return emptySet()
        return value.split(",").mapNotNull { it.trim().toIntOrNull() }.toSet()
    }

    @TypeConverter
    fun fromHolidayType(type: HolidayType): String = type.name

    @TypeConverter
    fun toHolidayType(value: String): HolidayType = HolidayType.valueOf(value)
}
