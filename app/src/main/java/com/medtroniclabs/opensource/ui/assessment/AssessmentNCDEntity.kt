package com.medtroniclabs.opensource.ui.assessment

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "AssessmentNCDEntity")
data class AssessmentNCDEntity(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var assessmentDetails: String,
    var uploadStatus: Boolean = false,
    var createdAt: Long = System.currentTimeMillis(),
    var userId: Long? = null
)
