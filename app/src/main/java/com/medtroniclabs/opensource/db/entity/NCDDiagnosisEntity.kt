package com.medtroniclabs.opensource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "NCDDiagnosisEntity")
data class NCDDiagnosisEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val displayOrder: Int,
    val value: String? = null,
    @ColumnInfo(name = "culture_value")
    val displayValue: String? = null,
    var type: String? = null,
    var gender: String? = null
)
