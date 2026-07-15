package org.medtroniclabs.uhis.data.registration

import java.io.Serializable

data class NurseCreateResponse(
    val prescriptions: ArrayList<PrescriptionModels>? = null,
    val investigations: ArrayList<InvestigationModels>? = null,
    val avgSystolic: Int? = null,
    val avgDiastolic: Int? = null,
    val glucoseLog: ArrayList<GlucoseLog>? = null,
    val symptoms: ArrayList<SymptomModels>? = null,
    val compliance: String? = null,
    val medicationTakenDays: Int? = null,
    val bgInvestigationDate: String? = null,
    val nextMedicalReviewDate: String? = null,
    val patientTrackId: Long? = null,
    val tenantId: Long? = null,
    val id: Long? = null,
) : java.io.Serializable

data class PrescriptionModels(
    val id: Int? = null,
    val medicationName: String? = null,
    val dosageUnitValue: String? = null,
    val dosageUnitName: String? = null,
    val dosageFrequencyName: String? = null,
    val prescribedDays: Int? = null,
    val instructionNote: String? = null,
    val dosageFormName: String? = null,
) : java.io.Serializable

data class InvestigationModels(
    val id: Int? = null,
    val createdBy: Int? = null,
    val updatedBy: Int? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val tenantId: Int? = null,
    val labTestId: Int? = null,
    val labTestName: String? = null,
    val resultDate: String? = null,
    val referredBy: Int? = null,
    val isReviewed: Boolean? = null,
    val isAbnormal: Boolean? = null,
    val patientTrackId: Int? = null,
    val patientVisitId: Int? = null,
    val resultUpdateBy: String? = null,
    val comment: String? = null,
    val patientLabTestResults: List<String>? = null,
    val active: Boolean? = null,
    val deleted: Boolean? = null,
) : java.io.Serializable

data class SymptomModels(
    val id: Int? = null,
    val name: String,
    val type: String? = null,
    val otherSymptom: String? = null,
    val newWorseningSymptoms: String? = null,
) : Serializable
