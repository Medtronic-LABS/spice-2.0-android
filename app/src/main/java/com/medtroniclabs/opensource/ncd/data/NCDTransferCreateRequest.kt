package com.medtroniclabs.opensource.ncd.data

data class NCDTransferCreateRequest(
    val tenantId: String,
    val transferTo: Long?,
    val transferSite: Long?,
    val oldSite: Long?,
    val transferReason: String? = null,
    val patientReference: String? = null,
    val memberReference: String? = null
)