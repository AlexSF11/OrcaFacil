package com.example.orcafacil.model

import androidx.room.TypeConverter
import java.util.*

class Converters {
    @TypeConverter
    fun toDate(dateLong: Long?) : Date? {
        return if (dateLong != null) Date(dateLong) else null
    }

    @TypeConverter
    fun fromDate(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromListString(value: List<String>): String {
        return value.joinToString(";") // Usa ";" como separador
    }

    @TypeConverter
    fun toListString(value: String): List<String> {
        return value.split(";").map { it.trim() }
    }

    @TypeConverter
    fun fromListDouble(value: List<Double>): String {
        return value.joinToString(";") { it.toString() }
    }

    @TypeConverter
    fun toListDouble(value: String): List<Double> {
        return value.split(";").map { it.toDoubleOrNull() ?: 0.0 }
    }
}

