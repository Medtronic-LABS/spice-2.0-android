package org.medtroniclabs.uhis.ncd.data

import org.medtroniclabs.uhis.data.registration.PatientModel

data class ScreeningPatientResponse(var message: String, val entity: PatientModel)
