package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = EntitiesName.CONSENT_FORM)
data class ConsentForm(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val type: String,
    val content: String
)