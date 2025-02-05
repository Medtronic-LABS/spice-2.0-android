package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDMentalHealthStatusRequest(
    val id: String? = null,
    val mentalHealthStatus: MentalHealthStatus? = null,
    val substanceUseStatus: MentalHealthStatus? = null,
    val provenance: ProvanceDto? = null,
    val ncdPatientStatus: NcdPatientStatus? = null,
    val patientReference: String? = null,
    val memberReference: String? = null,
    val patientVisitId: String? = null
)

data class MentalHealthStatus(
    val id: String? = null,
    val status: String? = null,
    val comments: String? = null,
    val yearOfDiagnosis: Int? = null,
    val mentalHealthDisorder: ArrayList<String>? = null,
)
