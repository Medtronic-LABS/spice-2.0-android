package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "DistrictEntity")
data class DistrictEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val code: String? = null,
    val countryId: Long,
)
