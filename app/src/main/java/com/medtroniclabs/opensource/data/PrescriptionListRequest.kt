package com.medtroniclabs.opensource.data

data class PrescriptionListRequest(
    val patientReference: String? = null,
    val memberReference: String? = null,
    val isActive: Boolean = false,
    val requestFrom: String? = null
)



