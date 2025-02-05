package com.medtroniclabs.opensource.data.offlinesync.model

import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime

data class AssessmentEncounter(
    val householdId: String?,
    val memberId: String?,
    val referred: Boolean,
    val patientId: String?,
    val provenance: ProvanceDto,
    val latitude: Double,
    val longitude: Double,
    val visitNumber: Long? = null,
    val neonatePatientId: String? = null,
    val startTime: String = System.currentTimeMillis().convertToUtcDateTime(),
    val endTime: String = System.currentTimeMillis().convertToUtcDateTime()
)
