package com.medtroniclabs.opensource.network

import android.content.Context
import android.os.Build
import android.provider.Settings
import com.medtroniclabs.opensource.common.AppConstants.ANDROID
import com.medtroniclabs.opensource.ncd.data.DeviceDetails

object DeviceInformation {
    fun getDeviceDetails(context: Context): DeviceDetails {
        return DeviceDetails(
            deviceId = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ANDROID_ID
            ),
            name = Build.MANUFACTURER,
            model = Build.MODEL,
            type = ANDROID,
            version = Build.VERSION.RELEASE
        )
    }
}