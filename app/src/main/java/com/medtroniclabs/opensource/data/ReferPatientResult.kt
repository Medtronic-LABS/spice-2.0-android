package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class ReferPatientResult(
    val encounterId: String?,
    val type: String?,
    val referredReason: String?,
    val referredSiteId: String?,
    val referredClinicianId: String?,
    val patientReference: String?,
    val referred: Boolean = true,
    val provenance: ProvanceDto?,
    val patientStatus: String?,
    val currentPatientStatus: String?,
    val assessmentName: String?,
    val patientId: String?,
    val householdId: String?,
    val villageId: String?,
    val memberId: String?,
    val category:String? = null
)
