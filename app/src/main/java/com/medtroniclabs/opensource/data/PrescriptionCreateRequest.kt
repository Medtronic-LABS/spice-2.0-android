package com.medtroniclabs.opensource.data

data class PrescriptionCreateRequest(
    val requestFrom: String,
    val encounter: EncounterDetails,
    val patientReference: String? = null,
    val enrollmentType: String? = null,
    val identityValue: String? = null,
    val prescriptions: ArrayList<PrescriptionDetails>
)

data class PrescriptionDetails(
    val prescriptionId: String? = null,
    val prescribedDays: Long? = null,
    val medicationName: String,
    val medicationId: String,
    val prescribedSince: String? = null,
    val dosageDurationName: String? = null,
    val dosageFormName: String,
    val dosageFrequencyName: String,
    val brandName: String,
    val classificationName: String,
    val dosageUnitName: String?,
    val dosageUnitValue: String?,
    val instructionNote: String?,
    val codeDetails: CodeDetailsObject? = null,
    val memberId : String? = null,
    val instructionUpdated : Boolean? = null,
    val prescriptionFilledDays : Long? = null,
    val reason : String? = null

)



