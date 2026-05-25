package org.medtroniclabs.uhis.data.servicerecipient

data class ServiceRecipientPatientHistory(
    val ncdScreening: List<ServiceRecipientHistory>,
    val eyeCareScreening: List<ServiceRecipientHistory>,
    val cataractScreening: List<ServiceRecipientHistory>,
)

data class ServiceRecipientHistory(
    val id: Long? = null,
    val type: String? = null,
    val createdAt: String? = null,
    val avgSystolic: Int? = null,
    val avgDiastolic: Int? = null,
    val glucoseType: String? = null,
    val glucoseValue: Double? = null,
    val glucoseUnit: String? = null,
    val eyeCare: EyeCare? = null,
    val cataract: Cataract? = null,
    val performedBy: String? = null,
    val performedByRole: String? = null,
)

data class EyeCare(
    val glassPower: String? = null,
    val hasGlassSold: String? = null,
    val eyeTestOutcome: String? = null,
)

data class Cataract(
    val eyeDisease: List<String>? = null,
    val glassPower: String? = null,
    val hasGlassSold: String? = null,
    val isPresbyopia: Boolean? = null,
    val operationName: List<String>? = null,
    val ncdServiceProvided: String? = null,
    val referredForOperation: String? = null,
)
