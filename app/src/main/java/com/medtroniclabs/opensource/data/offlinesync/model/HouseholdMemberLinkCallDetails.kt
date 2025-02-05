package com.medtroniclabs.opensource.data.offlinesync.model

data class HouseholdMemberLinkCallDetails(
    val memberId: String,
    val patientId: String?,
    val villageId: String,
    val callRegisterDetails: List<CallRegisterDetail>,
    val provenance: ProvanceDto = ProvanceDto()
)
