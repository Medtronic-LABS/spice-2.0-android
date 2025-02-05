package com.medtroniclabs.opensource.app.analytics.model

import com.google.gson.JsonElement

data class AnalyticsData(
    val deviceDetails: DeviceDetail = DeviceDetail(),
    val userAnalytics: List<AnalyticsDetail>

)

data class AnalyticsDetail(
    val id: String,
    val lastSyncDate: String,
    val role:String,
    val analytics: MutableMap<String, MutableList<JsonElement>>,
    val sessionTracking:MutableList<JsonElement>
)

data class DeviceDetail(
    val appVersion: String = UserDetail.appVersion,
    val deviceName: String = android.os.Build.MODEL,
    val apiVersion: Int = android.os.Build.VERSION.SDK_INT
)