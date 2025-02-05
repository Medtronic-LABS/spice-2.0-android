package com.medtroniclabs.opensource.data.model

data class CreateLabourDeliveryResponse(
    val childPatientReference: String,
    val patientReference: String,
    val neonateId: String,
    val motherId: String
)
