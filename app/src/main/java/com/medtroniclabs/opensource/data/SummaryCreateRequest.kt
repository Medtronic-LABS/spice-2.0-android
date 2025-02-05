package com.medtroniclabs.opensource.data

data class SummaryCreateRequest(
    var motherDTO: MedicalReviewSummarySubmitRequest?=null,
    var neonateDTO: MedicalReviewSummarySubmitRequest?=null)


