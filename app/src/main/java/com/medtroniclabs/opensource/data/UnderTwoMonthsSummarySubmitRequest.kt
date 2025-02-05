package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class UnderTwoMonthsSummarySubmitRequest(
    val id: String,
    val memberId: String,
    val patientReference: String?=null,
    val provenance: ProvanceDto,
    val nextVisitDate:String? = null,
    val patientStatus:String? = null
)
