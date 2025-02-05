package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserProfileEntity")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val profileData: String
)
