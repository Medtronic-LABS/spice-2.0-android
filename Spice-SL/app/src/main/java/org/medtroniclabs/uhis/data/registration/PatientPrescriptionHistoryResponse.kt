package org.medtroniclabs.uhis.data.registration

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

data class PatientPrescriptionHistoryResponse(
    val patientPrescription: ArrayList<PatientPrescription>,
    val prescriptionHistoryDates: ArrayList<VisitDateModel>,
)

@Parcelize
data class PatientPrescription(
    var id: Long?,
    var medicationId: Long? = null,
    var prescriptionId: Long? = null,
    val medicationName: String?,
    val dosageUnitValue: String?,
    val dosageUnitName: String?,
    val dosageFrequencyName: String?,
    val patientVisitId: Long?,
    val createdAt: String,
    val prescribedSince: String?,
    val dosageFormName: String?,
    val prescribedDays: Int?,
    val instructionNote: String?,
    val classificationName: String? = null,
    val brandName: String? = null,
) : Parcelable
