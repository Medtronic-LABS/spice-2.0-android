package org.medtroniclabs.uhis.data.model

data class ResponseDivisionDistrictUpazilas(
    val id: Long,
    val name: String,
    val ouCount: Long? = null,
    val siteCount: Long? = null,
    val tenantId: Long? = null,
)
