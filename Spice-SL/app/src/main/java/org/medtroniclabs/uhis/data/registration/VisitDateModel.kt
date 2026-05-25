package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class VisitDateModel(
    val visitDate: String,
    @SerializedName("id")
    val _id: Long,
)
