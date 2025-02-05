package com.medtroniclabs.opensource.data.resource

data class ExaminationResult(
    val index: Int,
    val symptomsTitle: String?,
    val description: List<String>?
)
