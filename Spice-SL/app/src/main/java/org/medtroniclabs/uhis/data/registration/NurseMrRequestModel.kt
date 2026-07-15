package org.medtroniclabs.uhis.data.registration

import java.io.Serializable

data class NurseMrRequestModel(
    val clinicalNote: String? = null,
    val complications: List<String>? = null,
    val isPregnant: Boolean? = null,
    var nextMedicalReviewDate: String? = null,
    var patientTrackId: Long? = null,
    var patientVisitId: Long? = null,
    var tenantId: Long? = null,
    var assessmentOrganizationId: String? = null,
    var encounterReference: String? = null,
    var memberReference: String? = null,
    var patientReference: String? = null,
    var bpLog: BpLog? = null,
    var glucoseLog: ArrayList<GlucoseLog>? = null,
    var prescription: Prescription? = null,
    var confirmDiagnosis: ArrayList<String>? = arrayListOf(),
    var hadCounselling: String? = null,
    var labTest: ArrayList<LabTest>? = null,
    var symptomsLog: SymptomsLog? = null,
) : java.io.Serializable

data class BpLog(
    var avgSystolic: Double? = null,
    var bpTakenOn: String? = null,
    var type: String? = null,
    var avgBloodPressure: String? = null,
    var unitMeasurement: String? = null,
    var avgDiastolic: Double? = null,
    var bpLogDetails: ArrayList<BpLogDetails>? = null,
    val height: Double? = null,
    val bmi: Double? = null,
    val weight: Double? = null,
) : java.io.Serializable

data class BpLogDetails(
    var diastolic: Double? = null,
    var systolic: Double? = null,
) : java.io.Serializable

data class GlucoseLog(
    var refId: String? = null,
    var glucoseType: String? = null,
    var hba1c: Double? = null,
    var ogtt: Double? = null,
    var glucoseValue: Double? = null,
    var glucoseDate: String? = null,
) : java.io.Serializable

// var glucoseType: String? = null,
// var hba1c: Double? = null,
// var ogtt:   Double? = null,
// var glucoseValue: Double? = null,
// var date: String? = null
data class Prescription(
    var prescriptionList: ArrayList<PrescriptionItem>? = ArrayList<PrescriptionItem>(),
) : java.io.Serializable

data class PrescriptionItem(
    var id: Long? = null,
    var medicationId: Long? = null,
    var dosageFrequencyId: Long? = null,
    var dosageUnitId: Long? = null,
    var endDate: String? = null,
    var prescribedDays: Int? = null,
    var classificationName: String? = null,
    var brandName: String? = null,
    var medicationName: String? = null,
    var dosageUnitValue: String? = null,
    var dosageUnitName: String? = null,
    var dosageFrequencyName: String? = null,
    var instructionNote: String? = null,
    var dosageFormName: String? = null,
    var prescribedSince: String? = null,
    var prescriptionRemainingDays: Int? = null,
    var discontinuedOn: String? = null,
) : java.io.Serializable

data class LabTest(
    var id: Long? = null,
    var labTestId: Long? = null,
    var labTestName: String? = null,
    var resultDate: String? = null,
    var referredBy: Long? = null,
    var isReviewed: Boolean? = null,
    var isAbnormal: Boolean? = null,
    var comment: String? = null,
    var patientLabTestResults: ArrayList<LabTestResults>? = arrayListOf(),
) : java.io.Serializable

data class LabTestResults(
    val id: Long? = null,
    val name: String? = null,
    val displayName: String? = null,
    val resultValue: Double? = null,
    val unit: String? = null,
    val resultStatus: String? = null,
    val isAbnormal: Boolean? = null,
    val displayOrder: Int? = null,
) : Serializable

data class Symptom(
    val name: String? = null,
    val id: Long? = null,
    val type: String? = null,
    var newWorseningSymptoms: String? = null,
)

data class SymptomsLog(
    var symptoms: ArrayList<Symptom>? = null,
    var compliance: String? = null,
    var newWorseningSymptoms: String? = null,
    var medicationTakenDays: Double? = null,
    var hasSymptoms: Boolean? = null,
)
