package com.medtroniclabs.opensource.data

data class PatientPrescriptionModel(
    var id: Long? = null,
    var tenantId: Long? = null,
    var patientVisitId: Long? = null,
    var patientTrackId: Long? = null,
    var isDeleted: Boolean? = null,
    var prescriptions: ArrayList<PrescriptionDetails>? = null,
    var items: ArrayList<MedicationResponse>? = null,
    var discontinuedReason: String? = null,
    var lastRefillVisitId: String? = null,
    val patientReference : String? = null,
    val enrollmentType: String? = null,
    val identityValue: String? = null,
    val encounterId : String? = null

)


data class UpdateMedicationModel(
    var id: Long? = null,
    var isDeleted: Boolean? = null,
    var medicationId: Long? = null,
    var dosageForm: String? = null,
    var dosageUnitId: Long? = null,
    var dosageFrequencyId: Long? = null,
    var prescribedDays: Int? = null,
    var prescriptionRemainingDays: Int? = null,
    var prescribedSince: String? = null,
    var endDate: String? = null,
    var medicationName: String? = null,
    var dosageName: String? = null,
    var classification: String? = null,
    var classificationName: String? = null,
    var brandName: String? = null,
    var dosageUnitValue: String? = null,
    var dosageUnitName: String? = null,
    var dosageFrequencyName: String? = null,
    var instructionNote: String? = null,
    var discontinuedOn: String? = null,
    var dosageFormName: String? = null
)
data class ResponseDataModel(var message: String?)