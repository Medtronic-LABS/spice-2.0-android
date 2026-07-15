package org.medtroniclabs.uhis.data.resource

import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference

data class RequestAllEntities(
    var villageIds: List<Long> = listOf(),
    val lastSyncTime: String? = null,
    var memberIds: List<Long> = listOf(),
    var practitionerId: String? = null,
    val userId: Long = SecuredPreference.getUserId(),
    val appVersionName: String = BuildConfig.VERSION_NAME,
    val appVersionCode: Int = BuildConfig.VERSION_CODE,
    val deviceId: String? = SecuredPreference.getDeviceId(),
    val appType: String = CommonUtils.isCommunityOrNot(),
)
