package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ClinicalWorkflowEntity")
data class ClinicalWorkflowEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val moduleType: String?=null,
    val workflowName: String?=null,
    val countryId: Long,
    val displayOrder:Int? = null,
)

data class NCDAssessmentClinicalWorkflow(
    val id: Long,
    val name: String,
    val workflowName: String,
    val category: String? = null,
    val groupName: String? = null,
    val cultureGroupName: String? = null,
    val subModule: String? = null,
    val displayOrder: Int? = null
)