package com.medtroniclabs.opensource.ncd.data

data class LifeStyleRequest(
    var patientReference: String?,
)

data class LifeStyleResponse(
    var comments: String? = null,
    var lifestyleAnswer: String?= null,
    var lifestyle: String?= null,
    var lifestyleType: String? = null,
    var value:String? = null
)
