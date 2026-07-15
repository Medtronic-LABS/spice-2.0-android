package org.medtroniclabs.uhis.data.registration

data class FillPrescriptionRequest(
    val patientTrackId: Long,
    val tenantId: Long,
)
