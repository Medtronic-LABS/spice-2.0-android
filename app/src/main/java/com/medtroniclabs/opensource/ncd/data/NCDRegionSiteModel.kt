package com.medtroniclabs.opensource.ncd.data

data class NCDRegionSiteModel(
    val limit: Int? = null,
    val skip: Int? = null,
    val searchTerm: String? = null,
    val countryId: Long? = null,
    val tenantId: Long? = null
)
