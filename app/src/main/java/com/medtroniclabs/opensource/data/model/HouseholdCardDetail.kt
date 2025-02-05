package com.medtroniclabs.opensource.data.model

data class HouseholdCardDetail(
    val id: Long,
    val name: String,
    val householdNo: Long? = null,
    val villageName: String,
    val landmark: String?,
    val householdHeadPhoneNumber: String?,
    val memberRegistered: Int,
    val memberAdded: Int
)
