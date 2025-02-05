package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "MentalHealthEntity")
data class MentalHealthEntity(
    @PrimaryKey
    val formType: String,
    val formInput: String?
)