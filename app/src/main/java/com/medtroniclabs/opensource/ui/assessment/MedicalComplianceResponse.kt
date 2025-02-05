package com.medtroniclabs.opensource.ui.assessment

data class MedicalComplianceResponse(
    val name: String,
    val otherCompliance: String? = null,
    val cultureValue: String? = null
)
