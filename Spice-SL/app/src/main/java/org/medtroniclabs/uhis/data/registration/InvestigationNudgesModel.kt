package org.medtroniclabs.uhis.data.registration

data class InvestigationNudgesModel(
    var HbA1c: ArrayList<HBA1CModel>? = null,
    var LipidProfile: ArrayList<HBA1CModel>? = null,
    var RenalFunctionTest: ArrayList<HBA1CModel>? = null,
)

data class HBA1CModel(
    val resultDate: String? = null,
    val createdAt: String,
    val labTestName: String,
    val labTestResult: ArrayList<HBA1CResultModel>? = null,
)

data class HBA1CResultModel(
    val resultValue: String? = null,
    val unit: String? = null,
    val name: String? = null,
    val resultName: String? = null,
)
