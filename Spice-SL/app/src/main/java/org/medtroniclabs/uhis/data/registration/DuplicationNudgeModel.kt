package org.medtroniclabs.uhis.data.registration

data class DuplicationNudgeModel(val message: String?, val entity: PatientModel?)

data class PatientModel(
    val id: Long? = null,
    val enrollment: PatientModel?,
    val patientTrackId: Long?,
    val programId: Long? = null,
    val accountId: Long? = null,
    val operatingUnitId: Long? = null,
    val canDoAssessment: Boolean? = null,
    val canDoEnrollment: Boolean? = null,
    val firstName: String? = null,
    val lastName: String? = null,
    val phoneNumber: String? = null,
    val nationalId: String? = null,
)
