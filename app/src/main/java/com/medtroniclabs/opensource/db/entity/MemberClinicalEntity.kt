package com.medtroniclabs.opensource.db.entity

data class MemberClinicalEntity(
    val patientId: String?,
    val visitCount: Long,
    val clinicalDate: String?,
    val numberOfNeonate: Long? = null,
    val isDeliveryAtHome: Boolean? = null
)