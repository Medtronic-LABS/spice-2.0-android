package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDPatientStatusRequest(
    val id: String? = null,
    val memberReference: String? = null,
    val provenance: ProvanceDto? = null,
    val ncdPatientStatus: NcdPatientStatus? = null,
    val patientReference: String? = null,
    val patientVisitId: String? = null
)

data class NcdPatientStatus(
    val id: String? = null,
    val diabetesStatus: String? = null,
    val hypertensionStatus: String? = null,
    val hypertensionYearOfDiagnosis: String? = null,
    val diabetesYearOfDiagnosis: String? = null,
    val diabetesControlledType: String? = null,
    val diabetesDiagnosis: String? = null
)