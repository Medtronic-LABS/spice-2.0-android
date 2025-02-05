package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ScreeningEntity")
data class ScreeningEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var screeningDetails: String,
    var generalDetails: String,
    var uploadStatus: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: String? = null,
    var signature: ByteArray? = null,
    var isReferred: Boolean = false
)