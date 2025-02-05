package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDMRSummaryRequestResponse(
    val memberReference: String? = null,
    val patientReference: String? = null,
    val encounterReference: String? = null,
    val nextMedicalReviewDate: String? = null,
    val villageId: String? = null,
    val provenance: ProvanceDto? = null,
    val patientVisitId: String? = null,
    val enrollmentType: String? = null,
    val identityValue: String? = null,
    val diagnosisType: List<String>? = null
)