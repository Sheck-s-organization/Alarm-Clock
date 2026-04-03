package com.smartalarm.app.data.db

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.smartalarm.app.data.entities.HolidayType
import com.smartalarm.app.data.entities.LocationTimeOverride

class Converters {

    private val gson = Gson()

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

    @TypeConverter
    fun fromLocationOverrides(overrides: List<LocationTimeOverride>): String =
        gson.toJson(overrides)

    @TypeConverter
    fun toLocationOverrides(json: String): List<LocationTimeOverride> {
        if (json.isBlank() || json == "[]") return emptyList()
        val type = object : TypeToken<List<LocationTimeOverride>>() {}.type
        return gson.fromJson(json, type)
    }
}
