package org.medtroniclabs.uhis.ui.patient.util

interface CommonDialogInterface {
    fun onSuccess(
        confirmedDiagnoses: ArrayList<String>? = null,
        diagnosisNotes: String? = null,
    )
}
