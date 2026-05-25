package org.medtroniclabs.uhis.ui.patient

import org.medtroniclabs.uhis.data.model.PatientListResModel

interface PatientSelectionListener {
    fun onSelectedPatient(item: PatientListResModel)

    fun onRegisterPatientCall(item: PatientListResModel)
}
