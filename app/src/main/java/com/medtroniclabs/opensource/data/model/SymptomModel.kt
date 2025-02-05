package com.medtroniclabs.opensource.data.model

import com.google.gson.annotations.SerializedName

class SymptomModel(
    val id: Long,
    val symptom: String,
    var isSelected: Boolean = false,
    val type: String? = null,
    val viewType: Int = 0,
    var otherSymptom: String? = null,
    var value: String? = null,
    @SerializedName("culture_value")
    val cultureValue:String? = null
)
