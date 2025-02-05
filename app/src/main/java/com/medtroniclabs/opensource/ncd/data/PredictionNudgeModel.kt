package com.medtroniclabs.opensource.ncd.data

data class PrescriptionNudgeResponse(
    var recentBGLogs: ArrayList<RecentBGLogs>,
    var prescriptionResults: ArrayList<PrescriptionResults>
)

data class RecentBGLogs(
    val glucoseUnit: String? = "mmol/L",
    val glucoseDateTime: String? = null,
    val glucoseType: String? = null,
    val glucoseValue: Double? = null,
    val lastMealTime: String? = null,
    val hba1c: Double? = null,
    val hba1cUnit: String? = "%",
    val hba1cDateTime: String? = null,
    val type: String? = null,
    val encounterId: String? = null,
    val createdAt: String? = null,
)

data class PrescriptionResults(
    val medicationName: String? = null,
    val dosageFrequencyName: String? = null,
    val dosageFormName: String? = null,
    val dosageUnitName: String? = null,
    val dosageUnitValue: String? = null,
)


data class PredictionRequest(val memberId : String? = null, val patientReference : String? = null)


data class HBA1CModel(
    val id: String? = null,
    val testName: String? = null,
    val testedOn: String? = null,
    val recommendedBy: String? = null,
    val recommendedName: String? = null,
    val recommendedOn: String? = null,
    val patientId: String? = null,
    val labTestResults: List<LabTestResult> = emptyList(),
    val codeDetails: Any? = null,
    val labTestCustomization: Any? = null,
    val comments: Any? = null,
    val roleName: Any? = null,
    val isReview: Any? = null,
    val resultUpdatedBy: Any? = null
)

data class LabTestResult(
    val id: String? = null,
    val name: String? = null,
    val value: String? = null,
    val unit: String? = null,
    val resource: Any? = null,
    val codeDetails: Any? = null,
    val testedOn: String? = null,
    val patientId: String? = null,
    val performedBy: Int? = null,
    val performedName: String? = null
)




