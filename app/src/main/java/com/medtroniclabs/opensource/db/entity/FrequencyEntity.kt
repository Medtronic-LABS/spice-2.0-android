package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = EntitiesName.FrequencyEntity)
data class FrequencyEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val displayValue: String? = null,
    val displayOrder: Long,
    @SerializedName("value")
    val frequency: Int = 1,
    val description: String? = null
)