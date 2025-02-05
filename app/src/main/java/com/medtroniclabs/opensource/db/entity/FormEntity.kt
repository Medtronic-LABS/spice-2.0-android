package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "FormEntity")
data class FormEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val formInput: String,
    val formType: String,
    val workflowName: String? = null,
    val clinicalWorkflowId: Long? = null
)