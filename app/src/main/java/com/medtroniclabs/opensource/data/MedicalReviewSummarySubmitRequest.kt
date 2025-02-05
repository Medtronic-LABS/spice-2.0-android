package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class MedicalReviewSummarySubmitRequest(
    val patientId: String? = null,
    val patientReference: String? = null,
    val memberId: String? = null,
    val id: String? = null,
    val provenance: ProvanceDto,
    val cost: String? = null,
    val medicalSupplies: List<String>? = null,
    val patientStatus: String? = null,
    val nextVisitDate: String? = null,
    val category:String? = null,
    val encounterType: String? = null,
    val householdId: String? = null,
    val villageId: String? = null
)
