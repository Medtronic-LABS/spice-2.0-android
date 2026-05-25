package org.medtroniclabs.uhis.data.registration

data class InitialEncounterResponse(
    val message: String,
    val ncdStatus: String? = null,
    var patientTrackId: Long? = null,
    var tenantId: Long? = null,
    var id: Long? = null,
)
