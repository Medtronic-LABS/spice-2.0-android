package org.medtroniclabs.uhis.data.registration

data class PrescriptionPredictionResponse(
    val recentBGLogs: ArrayList<RecentBGLogs>,
    val prescriptionResults: ArrayList<PrescriptionResult>,
)

data class RecentBGLogs(
    val hba1c: String? = null,
    val glucoseType: String? = null,
    val glucoseValue: String? = null,
    val bgTakenOn: String? = null,
    val glucoseUnit: String? = null,
    val hba1cUnit: String? = null,
)

data class PrescriptionResult(
    val medicationName: String?,
    val dosageUnitValue: String?,
    val dosageUnitName: String?,
    val dosageFrequencyName: String?,
)
