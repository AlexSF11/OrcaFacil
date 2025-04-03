package com.example.orcafacil.model

import android.os.Parcel
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "Budget")
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val phone: String,
    val address: String,
    val description: List<String>,
    val unitPrice: List<Double>,
    val totalPrice: Double,
    val createdDate: Date = Date(),
    val pdfPath: String? = null // Campo para o caminho do PDF
) : Parcelable {
    constructor(parcel: Parcel) : this(
        id = parcel.readInt(),
        name = parcel.readString() ?: "",
        phone = parcel.readString() ?: "",
        address = parcel.readString() ?: "",
        description = parcel.createStringArrayList() ?: emptyList(),
        unitPrice = parcel.createDoubleArray()?.toList() ?: emptyList(),
        totalPrice = parcel.readDouble(),
        createdDate = Date(parcel.readLong()),
        pdfPath = parcel.readString()
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
        parcel.writeString(phone)
        parcel.writeString(address)
        parcel.writeStringList(description)
        parcel.writeDoubleArray(unitPrice.toDoubleArray())
        parcel.writeDouble(totalPrice)
        parcel.writeLong(createdDate.time)
        parcel.writeString(pdfPath)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Budget> {
        override fun createFromParcel(parcel: Parcel): Budget {
            return Budget(parcel)
        }

        override fun newArray(size: Int): Array<Budget?> {
            return arrayOfNulls(size)
        }
    }
}