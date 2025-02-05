package com.medtroniclabs.opensource.data.offlinesync.model

import com.google.gson.JsonElement

data class SyncEntityList(
    val type: String?,
    val requestId: String?,
    val fhirId: String?,
    val status: String?,
    val errorMessage: String?,
    val referenceId: String?,
    val data: JsonElement?
)
