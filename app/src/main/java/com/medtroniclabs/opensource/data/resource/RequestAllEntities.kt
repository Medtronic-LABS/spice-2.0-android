package com.medtroniclabs.opensource.data.resource

import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SecuredPreference

data class RequestAllEntities(
    var villageIds: List<Long> = listOf(),
    val lastSyncTime: String? = null,
    val userId: Long = SecuredPreference.getUserId(),
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val deviceId: String? = SecuredPreference.getDeviceId(),
    val appType: String = CommonUtils.isCommunityOrNot()
)