package com.medtroniclabs.opensource.data.history

import com.medtroniclabs.opensource.model.ReferredDate

data class HistoryEntity(
    val encounterId: String? = null,
    val dateOfReview: String? = null,
    val patientReference: String? = null,
    val history: List<ReferredDate>? = null,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null
)

data class Prescription(
    val prescribedDays: Int? = null,
    val medicationName: String? = null,
    val medicationId: String? = null,
    val frequency: Int? = null,
    var frequencyName: String? = null,
    val prescribedSince: String? = null,
    val endDate: String? = null,
    val prescriptionId: String? = null,
    val isActive: Boolean? = null,
    val discontinuedReason: String? = null,
    val discontinuedDate: String? = null,
    val encounterId: String? = null,
    val isDeleted: Boolean? = null,
    val codeDetails: String? = null,
    val classificationName: String? = null,
    val brandName: String? = null,
    val dosageFrequencyName: String? = null,
    val dosageUnitName: String? = null,
    val dosageUnitValue: String? = null,
    val instructionNote: String? = null,
    val dosageFormName: String? = null,
    val prescriptionRemainingDays: Int? = null,
    val prescriptionFilledDays: Int? = null,
    val dispenseRemainingDays: Int? = null,
    val lastReFillDate: String? = null,
    val reason: String? = null
)

data class Investigation(
    val id: String,
    val testName: String,
    val patientId: String
)
data class PatientStatus(
    val name:String,
    val value:String
)
