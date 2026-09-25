package com.studypartner.planner.data.local

import androidx.room.TypeConverter

class RoomTypeConverters {
    @TypeConverter
    fun fromStringList(value: List<String>?): String {
        return value?.joinToString(separator = ",") ?: ""
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        return value.split(",")
    }
}
