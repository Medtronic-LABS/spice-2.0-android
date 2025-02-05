package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "LabourDeliveryMetaEntity")
data class LabourDeliveryMetaEntity(
    @PrimaryKey(autoGenerate = true)
    val itemId: Long,
    val id: Long,
    val name: String,
    var category: String? = null,
    var type: String? = null,
    val displayOrder: Int,
    val value: String
)
