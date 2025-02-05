package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class AddMemberRegRequest(
    var dateOfBirth: String?=null,
    var gender: String?=null,
    val householdId: Int?=null,
    var name: String?=null,
    val patientId: String?=null,
    val motherPatientId: Int?=null,
    val child: Boolean?=null,
    var isPregnant: Boolean?=null,
    var phoneNumber: String?=null,
    var phoneNumberCategory: String?=null,
    var provenance:  ProvanceDto? = null,
    var village: String?=null,
    var villageId: String?=null,
)


