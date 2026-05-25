package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class LabTestSearchResponse(
    @SerializedName("id")
    val _id: Long? = null,
    val name: String? = null,
    val country: String? = null,
)
