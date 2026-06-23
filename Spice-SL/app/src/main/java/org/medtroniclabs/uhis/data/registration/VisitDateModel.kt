package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class VisitDateModel(
    @SerializedName("date")
    val visitDate: String,
    @SerializedName("id")
    val _id: Long,
)
