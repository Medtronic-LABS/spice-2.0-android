package org.medtroniclabs.uhis.data.model

data class RegisterCallResponse(
    val id: Long,
    val firstName: String,
    val lastName: String,
    val phoneNumber: String,
    val callType: String,
    val patientTrackId: Long,
    val gender: String,
    val age: Long,
    val unionId: Long,
)
