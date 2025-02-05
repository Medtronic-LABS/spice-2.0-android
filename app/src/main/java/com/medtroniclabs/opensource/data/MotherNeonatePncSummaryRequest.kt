package com.medtroniclabs.opensource.data

data class MotherNeonatePncSummaryRequest(
    var id: String?=null,
    var patientReference: String?=null,
    var childId: String?=null,
    var childPatientReference: String?=null
)
