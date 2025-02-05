package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "CulturesEntity")
data class CulturesEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val code: String
)
