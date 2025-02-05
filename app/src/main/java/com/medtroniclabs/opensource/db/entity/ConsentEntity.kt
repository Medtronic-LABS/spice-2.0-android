package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ConsentEntity")
data class ConsentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val formType: String,
    val formInput: String
)