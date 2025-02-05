package com.medtroniclabs.opensource.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import kotlinx.android.parcel.RawValue
import java.util.Calendar


@Parcelize
data class MedicationResponse(
    val id: Long? = null,     // prescription id
    val name: String? = null,
    val classificationName: String? = null,
    val dosageDurationName: String? = null,
    val brandName: String? = null,
    val dosageFormName: String? = null,
    var dosageUnitId: Long? = null,
    var selectedMap: @RawValue HashMap<String, Any>?,
    var quantity: Long? = null,
    var prescribedDays: Long? = null,
    var prescriptionId: String? = null,
    var isEditable: Boolean = false,
    var prescribedSince: String? = null,
    var showErrorMessage: Boolean = false,
    var codeDetails: @RawValue CodeDetailsObject? = null,
    var instructionNote: String? = null,
    var dosageFrequencyName: String? = null,
    var dosageUnitName: String? = null,
    var dosageUnitValue: String? = null,
    var discontinuedOn: String? = null,
    var prescriptionRemainingDays: Int? = null,
) : Parcelable {
    var datetime :Long ?= null
    init {
        datetime = Calendar.getInstance().timeInMillis
    }
    var filledPrescriptionDays: Long? = null
    var isEdit: Boolean = false
    var isEdited: Boolean = false
    var isInstructionUpdated = false
    var enteredDosageUnitValue: String? = null
    var dosage_form_name_entered: String? = null
    var dosage_duration_name: String? = null
    var dosage_unit_selected: Long? = null
    var dosage_unit_name_entered: String? = null
    var dosage_frequency_name_entered: String? = null
    var dosage_frequency_entered: Long? = null
    var instruction_entered: String? = null
}

data class MedicationRequestObject(var medicationResponse: MedicationResponse)