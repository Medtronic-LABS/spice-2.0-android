package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "unit_metric_entity")
data class UnitMetricEntity(
    @PrimaryKey
    val id: Long,
    @SerializedName("name")
    val unit: String,
    val type: String? = null,
    val displayOrder: Int,
    val description: String? = null,
    val status: Boolean,
    val displayValue: String,
    val isDefault: Boolean,
    val answerDependent: Boolean,
    val childExists: Boolean
)
