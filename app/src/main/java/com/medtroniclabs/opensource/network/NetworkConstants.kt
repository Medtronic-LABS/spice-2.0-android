package com.medtroniclabs.opensource.network

import com.medtroniclabs.opensource.BuildConfig

object NetworkConstants {
    const val BASE_URL = BuildConfig.API_BASE_URL
    //const val BASE_URL = "http://192.168.23.226/"
    const val AUTH_SESSION = "/auth-service"
    const val OFFLINE_SYNC_CREATE = "/offline-service/offline-sync/create"
    const val PRIVACY_POLICY = "privacy-policy"
}