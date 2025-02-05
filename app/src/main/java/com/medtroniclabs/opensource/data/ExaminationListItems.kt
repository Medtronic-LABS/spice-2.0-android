package com.medtroniclabs.opensource.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ExaminationsEntity")
data class ExaminationListItems(
    @PrimaryKey
    val id: Long,
    val name: String,
    val type: String,
    val formInput: String? = null,
    val displayOrder: Int? = null
)
