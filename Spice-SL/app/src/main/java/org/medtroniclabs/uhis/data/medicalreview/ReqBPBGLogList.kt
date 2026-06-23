package org.medtroniclabs.uhis.data.medicalreview

data class ReqBPBGLogList(
    val memberId: String,
    val sortOrder: Int = 0,
    val skip: Int? = null,
    val limit: Int? = null,
)
