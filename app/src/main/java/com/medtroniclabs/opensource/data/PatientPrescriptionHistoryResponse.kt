package com.medtroniclabs.opensource.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class PatientPrescriptionHistoryResponse(
    var id: Long? = null,
    var medicationId: Long? = null,
    val medicationName: String? = null,
    val dosageUnitValue: String? = null,
    val dosageUnitName: String? = null,
    val dosageFrequencyName: String? = null,
    val patientVisitId: Long? = null,
    val createdAt: String? = null,
    val prescribedSince: String? = null,
    val dosageFormName: String? = null,
    val prescribedDays: Long? = null,
    val instructionNote: String? = null
) : Parcelable
