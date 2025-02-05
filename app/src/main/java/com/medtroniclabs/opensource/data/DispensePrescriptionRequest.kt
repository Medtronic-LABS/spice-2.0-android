package com.medtroniclabs.opensource.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class DispensePrescriptionRequest(
    val encounter: EncounterDetails,
    val prescriptions: List<DispenseUpdatePrescriptionRequest>
)
@Parcelize
data class DispensePrescriptionResponse(
    val prescribedDays: Int? = null,
    val medicationName: String? = null,
    val medicationId: String? = null,
    val frequency: Int? = null,
    val frequencyName: String? = null,
    val prescribedSince: String? = null,
    val endDate: String? = null,
    val prescriptionId: String? = null,
    val discontinuedReason: String? = null,
    val discontinuedDate: String? = null,
    val encounterId: String? = null,
    val isActive: Boolean? = null,
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
    var prescriptionFilledDays: Int? = null,
    val dispenseRemainingDays: Int? = null,
    val lastReFillDate: String? = null,
    var reason: String? = null,
    var otherReasonDetail: String? = null,
) : Parcelable {
    var isSelected: Boolean = false
}

data class DispenseUpdatePrescriptionRequest(
    val medicationName: String? = null,
    val dosageFrequencyName: String? = null,
    val prescriptionId: String? =null,
    val instructionNote: String? = null,
    val prescriptionFilledDays: Int? = null,
    val reason: String? = null
)