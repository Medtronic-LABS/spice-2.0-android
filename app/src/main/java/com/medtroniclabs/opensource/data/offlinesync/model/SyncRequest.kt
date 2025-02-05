package com.medtroniclabs.opensource.data.offlinesync.model

import com.google.gson.annotations.SerializedName

data class SyncRequest<T>(
    val requestId: String,
    @SerializedName("")
    val houseHolds: List<T>
)
