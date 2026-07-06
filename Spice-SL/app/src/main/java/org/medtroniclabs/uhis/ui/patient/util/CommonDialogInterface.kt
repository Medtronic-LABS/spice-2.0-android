package org.medtroniclabs.uhis.ui.patient.util

interface CommonDialogInterface {
    fun onSuccess(
        confirmedDiagnoses: ArrayList<String>?,
        diagnosisNotes: String?,
    )
}
