package org.medtroniclabs.uhis.data.registration

data class PatientRemoveRequest(
    val patientTrackId: Long,
    val tenantId: Long,
    val id: Long? = null, // Patient Id
    var deleteReason: String? = null,
    val deleteOtherReason: String? = null,
)
