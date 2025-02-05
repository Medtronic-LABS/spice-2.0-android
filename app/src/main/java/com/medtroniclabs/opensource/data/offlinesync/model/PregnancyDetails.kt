package com.medtroniclabs.opensource.data.offlinesync.model

data class PregnancyDetails(
    val householdMemberId: String,
    val ancVisitNo: Long?,
    val lastMenstrualPeriod: String?,
    val pncVisitNo: Int?,
    val dateOfDelivery: String?,
    val noOfNeonates: Int?,
    val childVisitNo: Int?
)
