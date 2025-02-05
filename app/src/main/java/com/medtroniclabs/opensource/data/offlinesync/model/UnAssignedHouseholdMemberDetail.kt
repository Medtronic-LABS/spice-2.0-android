package com.medtroniclabs.opensource.data.offlinesync.model

data class UnAssignedHouseholdMemberDetail(
    val memberId: String,
    val name: String,
    val phoneNumber: String,
    val dateOfBirth: String,
    val gender: String,
    val villageId: Long,
    val villageName: String,
    val lMemberId:String
)
