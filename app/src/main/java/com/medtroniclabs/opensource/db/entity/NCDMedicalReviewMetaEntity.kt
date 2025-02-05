package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NCDMedicalReviewMetaEntity")
data class NCDMedicalReviewMetaEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long,
    val name: String,
    val displayValue: String,
    val displayOrder: Int,
    val type: String? = null,
    var category: String? = null,
    val value: String? = null
)