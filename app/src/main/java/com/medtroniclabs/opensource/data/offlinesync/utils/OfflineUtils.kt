package com.medtroniclabs.opensource.data.offlinesync.utils

import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import java.util.UUID

object OfflineUtils {

    fun getRequestObject(): MutableMap<String, Any> {
        val map = mutableMapOf<String, Any>()
        map[OfflineConstant.REQUEST_ID] = UUID.randomUUID().toString()
        map[OfflineConstant.APP_VERSION_NAME] = BuildConfig.VERSION_NAME
        map[OfflineConstant.APP_VERSION_CODE] = BuildConfig.VERSION_CODE
        SecuredPreference.getDeviceId()?.let {
            map[OfflineConstant.DEVICE_ID] = it
        }
        map[DefinedParams.appType] = CommonUtils.isCommunityOrNot()
        return map
    }
}