package com.example.orcafacil.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import java.util.Date

@Entity(tableName = "Budget")
@TypeConverters(Converters::class)
data class Budget(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "phone") val phone: String,
    @ColumnInfo(name = "address") val address: String,
    @ColumnInfo(name = "description") val description: List<String>,
    @ColumnInfo(name = "unitPrice") val unitPrice: List<Double>,
    @ColumnInfo(name = "totalPrice") val totalPrice: Double,
    @ColumnInfo(name = "createdDate") val createdDate: Date = Date(),
)
