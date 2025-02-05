package com.medtroniclabs.opensource.data

data class BirthHistoryResponse(
    val birthWeight: String?=null,
    val gestationalAge: Int?=null,
    val haveBreathingProblem: Boolean?=null,
    val birthWeightCategory:String?=null   ,
    val gestationalAgeCategory:String?=null
)
