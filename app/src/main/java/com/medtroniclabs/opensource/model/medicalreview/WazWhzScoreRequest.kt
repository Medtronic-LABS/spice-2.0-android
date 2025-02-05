package com.medtroniclabs.opensource.model.medicalreview

data class WazWhzScoreRequest (val gender: String?=null,
                               val height: String?=null,
                               val ageInMonths: String?=null,
                               val weight: String?=null)

data class WazWhzScoreResponse(
    val wfa: Double?,
    val wfh: Double?
)