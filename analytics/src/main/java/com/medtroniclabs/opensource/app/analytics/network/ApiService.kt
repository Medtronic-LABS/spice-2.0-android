package com.medtroniclabs.opensource.app.analytics.network

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("spice-service/in-app-analytics/upload-file")
    suspend fun uploadFile(
        @Part file: MultipartBody.Part
    ): retrofit2.Response<Unit>
}
