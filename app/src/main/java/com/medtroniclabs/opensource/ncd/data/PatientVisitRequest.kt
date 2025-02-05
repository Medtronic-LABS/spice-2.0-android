package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class PatientVisitRequest(
    val patientReference: String? = null,
    val provenance: ProvanceDto,
    val memberReference: String? = null
)

data class PatientVisitResponse(
    val encounterReference: String? = null,
    val initialReviewed:Boolean? = false
)
