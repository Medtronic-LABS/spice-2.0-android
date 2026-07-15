package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ShasthyaKormiEntity")
data class ShasthyaKormiEntity(
    @PrimaryKey
    val id: Long,
    val firstName: String,
    val lastName: String,
    val gender: String? = null,
    val phoneNumber: String? = null,
    val username: String? = null,
    val email: String? = null,
)
