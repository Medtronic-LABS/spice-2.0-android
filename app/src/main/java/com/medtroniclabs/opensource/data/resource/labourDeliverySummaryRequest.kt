package com.medtroniclabs.opensource.data.resource

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class LabourDeliverySummaryRequest ( var motherDTO: MotherDTO?,
                                          var neonateDTO: NeonateDTO?)



data class MotherDTO(
    val id: String?=null,
    val patientReference: String?=null,
    val patientId: String?=null,
    val memberId: String?=null,
    var nextVisitDate: String?=null,
    val householdId: String?=null,
    val provenance: ProvanceDto,
    val patientStatus: String?=null
)

data class NeonateDTO(
    val id: String?=null,
    val patientReference: String?=null,
    val patientId: String?=null,
    val memberId: String?=null,
    val householdId: String?=null,
    val provenance: ProvanceDto,
    val patientStatus: String?=null
)

