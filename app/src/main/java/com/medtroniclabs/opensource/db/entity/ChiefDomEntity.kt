package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ChiefDomEntity")
data class ChiefDomEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val code: String? = null,
    val districtId: Long
)