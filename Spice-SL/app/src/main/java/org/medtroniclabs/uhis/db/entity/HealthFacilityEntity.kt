package org.medtroniclabs.uhis.db.entity

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
    val isUserSite: Boolean = false,
    val phoneNumber: String? = null,
    // Facility tier from the metadata API (HealthFacility.type). Used by the
    // MicroCoaching compliance gaps as actual.destinationTier. Nullable: null
    // until the next metadata sync repopulates it.
    val type: String? = null,
)
