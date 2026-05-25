package org.medtroniclabs.uhis.data.offlinesync.utils

import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
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
        map[DefinedParams.APP_TYPE] = CommonUtils.isCommunityOrNot()
        return map
    }
}
