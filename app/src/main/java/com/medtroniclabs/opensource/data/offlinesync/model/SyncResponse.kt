package com.medtroniclabs.opensource.data.offlinesync.model

data class SyncResponse(
    val message: String,
    val status: Boolean,
    val entityList: List<SyncEntityList>?,
    val responseCode: Int,
    val totalCount: Int
)
