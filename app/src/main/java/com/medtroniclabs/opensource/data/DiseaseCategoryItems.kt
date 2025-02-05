package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DiagnosisEntity")
data class DiseaseCategoryItems(
    @PrimaryKey
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val value: String,
    var type: String? = null,
    val diseaseCondition: ArrayList<DiseaseConditionItems>,
    val cultureValue: String? = null
)
