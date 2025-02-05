package com.medtroniclabs.opensource.ncd.data

data class RegionSiteResponse(
    val id: Long,
    val name: String,
    val tenantId: Long,
    val operatingUnit: OperatingUnitModel,
)

data class OperatingUnitModel(
    val tenantId: Long
)
