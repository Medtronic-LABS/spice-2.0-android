package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDCounselingModel(
    val id: String? = null,
    val patientReference: String? = null,
    val memberReference: String? = null,
    val visitId: String? = null,
    val patientVisitId: String? = null,
    val provenance: ProvanceDto = ProvanceDto(),
    val lifestyles: List<String>? = null,
    val cultureLifestyles: List<String>? = null,
    val clinicianNote: String? = null,
    var lifestyleAssessment: String? = null,
    var otherNote: String? = null,
    val isNutritionist: Boolean? = null,
    val clinicianNotes: List<String>? = null,
    var counselorAssessment: String? = null,
    val isCounselor: Boolean? = null,
    val referredDate: String? = null,
    val referredBy: String? = null,
    val referredByDisplay: String? = null,
    val assessedDate: String? = null,
    val assessedBy: String? = null,
    val assessedByDisplay: String? = null,
    var isExpanded: Boolean = false
)

data class AssessmentResultModel(
    val lifestyles: List<ResultModel>? = null,
    val counselorAssessments: List<ResultModel>? = null,
    val patientReference: String? = null,
    val memberReference: String? = null,
    val visitId: String? = null,
    val patientVisitId: String? = null,
    val referredDate: String? = null,
    val referredBy: String? = null,
    val referredByDisplay: String? = null,
    val assessedDate: String? = null,
    val assessedBy: String? = null,
    val assessedByDisplay: String? = null,
    val provenance: ProvanceDto = ProvanceDto(),
    val isNutritionist: Boolean = CommonUtils.isNonCommunity() && CommonUtils.isNutritionist()
)

data class ResultModel(
    val id: String? = null,
    val referredBy: String? = null,
    val lifestyleAssessment: String? = null,
    val counselorAssessment: String? = null,
    val otherNote: String? = null
)
