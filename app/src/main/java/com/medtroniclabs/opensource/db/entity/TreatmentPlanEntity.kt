package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "TreatmentPlanEntity")
data class TreatmentPlanEntity(
    @PrimaryKey
    var id: Long,
    val name: String,
    val displayValue: String? = null,
    val displayOrder: Int,
    val duration: String,
    val period: String,
    val riskLevel: String? = null,
    val type: String? = null
) {
    @Ignore
    val carePlanId: String? = null
}