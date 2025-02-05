package com.medtroniclabs.opensource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "SymptomEntity")
data class SignsAndSymptomsEntity(
    @PrimaryKey
    @SerializedName("id")
    val _id: Long,
    @SerializedName("name")
    val symptom: String,
    val type: String? = null,
    @ColumnInfo(name = "culture_value")
    val displayValue: String? = null,
    @ColumnInfo(name = "display_order")
    var displayOrder: Int? = null,
    var value: String? = null
) {
    @Ignore
    var isSelected = false
}
