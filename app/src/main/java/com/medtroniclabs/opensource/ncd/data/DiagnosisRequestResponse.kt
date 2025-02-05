package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import java.io.Serializable

data class NCDDiagnosisRequestResponse(
    val provenanceDTO: ProvanceDto? = null,
    val confirmDiagnosis: List<NCDDiagnosisItem>? = null,
    val diagnosis: List<NCDDiagnosisItem>? = null,
    val diagnosisNotes: String? = null,
    val patientReference: String? = null,
    val memberReference: String? = null,
    val type: String? = null
)

data class NCDDiagnosisItem(
    val type: String? = null,
    val value: String? = null,
    val name: String? = null
)  : Serializable

data class NCDDiagnosisGetRequest(
    val patientReference: String? = null,
    val memberReference: String? = null,
    val diagnosisType: List<String>? = null
)

data class NCDDiagnosisGetResponse(
    val diagnosis: List<NCDDiagnosisItem>? = null,
    val diagnosisNotes: String? = null,
) : Serializable
