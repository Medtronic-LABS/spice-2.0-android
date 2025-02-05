package com.medtroniclabs.opensource.data

data class DiseaseConditionItems(
    val id: Long,
    val diseaseId: Long,
    val name: String,
    val displayOrder: Int,
    val value: String? = null
)
