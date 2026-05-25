package org.medtroniclabs.uhis.data.registration

data class PatientPregnancyModel(
    var patientTrackId: Long?,
    var tenantId: Long? = null,
    var patientPregnancyId: Long? = null,
)
