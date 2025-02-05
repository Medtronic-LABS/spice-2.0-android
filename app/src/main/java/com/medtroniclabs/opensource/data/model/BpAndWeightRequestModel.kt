package com.medtroniclabs.opensource.data.model

data class BpAndWeightRequestModel(
    val systolic: Double? = null,
    val diastolic: Double? = null,
    val pulse: Double? = null,
    val weight: Double? = null,
    val encounter: MedicalReviewEncounter? = null
)
