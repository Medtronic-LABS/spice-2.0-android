package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.CodeDetailsObject

data class SearchLabTestResponse(
    val id: Long,
    val uniqueName: String,
    val testName: String,
    val formInput: String? = null,
    val countryId: Long,
    val updatedAt: String,
    val codeDetails: CodeDetailsObject? = null,
)