package org.medtroniclabs.uhis.ui.patient

interface DuplicationNudgeInterface {
    fun proceedEnrollment(patientTrackerId: Long?)

    fun proceedAssessment(patientTrackerId: Long?)

    fun processDuplicateScreening()
}
