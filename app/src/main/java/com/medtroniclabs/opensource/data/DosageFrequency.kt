package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "dosage_frequency_entity")
data class DosageFrequency(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val displayOrder: Long,
    @SerializedName("value")
    val frequency: Int = 1,
    val description: String? = null,
    val status: Boolean,
    val displayValue: String? = null,
    val isDefault: Boolean,
    val answerDependent: Boolean,
    val childExists: Boolean
)
