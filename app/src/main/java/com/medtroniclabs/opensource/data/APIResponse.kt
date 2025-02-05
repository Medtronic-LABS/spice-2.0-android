package com.medtroniclabs.opensource.data

data class APIResponse<out T> constructor(
    val status: Boolean,
    val entity: T? = null,
    val entityList: T? = null,
    val message: String? = null,
    val responseCode: Int,
    val totalCount: Int? = null
)
