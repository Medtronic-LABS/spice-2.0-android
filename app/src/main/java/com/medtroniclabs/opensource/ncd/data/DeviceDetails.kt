package com.medtroniclabs.opensource.ncd.data

data class DeviceDetails(
    val id: Long? = null,
    val deviceId: String? = null,
    val name: String? = null,
    val model: String? = null,
    val type: String? = null,
    val version: String? = null,
    var tenantId: Long? = null,
)
