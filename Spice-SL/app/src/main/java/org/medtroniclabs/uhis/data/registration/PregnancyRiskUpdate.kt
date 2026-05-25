package org.medtroniclabs.uhis.data.registration

data class PregnancyRiskUpdate(
    var isPregnancyRisk: Boolean? = null,
    var patientTrackId: Long? = null,
)
