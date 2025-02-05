package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "HealthFacilityEntity")
data class HealthFacilityEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val tenantId: Long,
    val districtId: Long,
    val chiefdomId: Long,
    val fhirId: String? = null,
    val isDefault: Boolean = false,
    val isUserSite: Boolean = false
)