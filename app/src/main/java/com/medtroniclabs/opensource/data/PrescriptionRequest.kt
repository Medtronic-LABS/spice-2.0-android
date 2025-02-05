package com.medtroniclabs.opensource.data


data class PrescriptionRequest(
    val encounter: EncounterDetails,
    val prescriptions: List<Prescription>
)

data class  Prescription(
    val prescribedDays: Long? = null,
    val medicationName: String,
    val medicationId: String,
    val frequency: Int,
    val prescribedSince: String,
    val prescriptionId: String? = null,
    val dosageDurationName: String? = null,
    val endDate: String? = null,
    val discontinuedDate: String? = null,
    var codeDetails: CodeDetailsObject? = null,
    var frequencyName: String,
    val discontinuedReason: String? = null,
    val encounterId: String? = null,
    val isActive: Boolean = true,
    val isDeleted: Boolean = false,
    val classificationName: String? = null,
    val brandName: String? = null,
    val dosageFrequencyName: String? = null,
    val dosageUnitName: String? = null,
    val dosageUnitValue: String? = null,
    val instructionNote: String? = null,
    val dosageFormName: String? = null,
    val prescriptionRemainingDays: Int? = null,
    val prescriptionFilledDays: Any? = null,
    val discontinuedOn : String? =null,
    val reason: String? = null,
    val instructionUpdated : String? = null
)



