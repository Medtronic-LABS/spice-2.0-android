package org.medtroniclabs.uhis.data.registration

import java.io.Serializable

data class BloodGlucoseListResponse(
    var skip: Int,
    var limit: Int,
    var total: Int,
    var glucoseLogList: ArrayList<BloodGlucose>?,
    var latestGlucoseLog: BloodGlucose?,
    var glucoseThreshold: List<GlucoseThreshold>? = null,
) : java.io.Serializable

data class GlucoseThreshold(
    var fbs: Int,
    var rbs: Int,
    var hba1c: Float,
    val unit: String,
) : Serializable
