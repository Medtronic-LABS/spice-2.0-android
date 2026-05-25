package org.medtroniclabs.uhis.data.registration

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.util.Calendar

@Parcelize
data class PrescriptionModel(
    var id: Long? = null,
    var prescriptionId: Long? = null,
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
    var dosageFormName: String? = null,
    val displayOrder: Int? = null,
) : Parcelable {
    var datetime: Long? = null

    init {
        datetime = Calendar.getInstance().timeInMillis
    }

    var filledPrescriptionDays: Int? = null
    var isEdit: Boolean = false
    var isEdited: Boolean = false
    var isInstructionUpdated = false
    var enteredDosageUnitValue: String? = null
    var dosageFormNameEntered: String? = null
    var dosageUnitSelected: Long? = null
    var dosageUnitNameEntered: String? = null
    var dosageFrequencyNameEntered: String? = null
    var dosageFrequencyEntered: Long? = null
    var instructionEntered: String? = null
}
