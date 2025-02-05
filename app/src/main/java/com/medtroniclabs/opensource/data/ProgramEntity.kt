package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "ProgramEntity")
data class ProgramEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val healthFacilityIds: ArrayList<Long>
) {
    @Ignore
    val healthFacilities: ArrayList<HealthFacility>? = null
}
