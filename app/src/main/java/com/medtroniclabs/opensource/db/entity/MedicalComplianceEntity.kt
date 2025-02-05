package com.medtroniclabs.opensource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MedicalComplianceEntity")
data class MedicalComplianceEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    @ColumnInfo(name = "display_order")
    val displayOrder: Int? = null,
    @ColumnInfo(name = "culture_value")
    val displayValue: String? = null,
    @ColumnInfo(name = "parent_compliance_id")
    val parentComplianceId: Long? = null,
    @ColumnInfo(name = "child_exists")
    var childExists: Boolean = false,
    val value: String ?= null
)
