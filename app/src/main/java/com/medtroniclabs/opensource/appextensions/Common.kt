package com.medtroniclabs.opensource.appextensions

import com.medtroniclabs.opensource.BuildConfig

fun isDebug(callback: (yes: Boolean) -> Unit) {
    if (BuildConfig.DEBUG) {
        callback.invoke(true)
    } else {
        callback.invoke(false)
    }
}


