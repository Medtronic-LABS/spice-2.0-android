package com.medtroniclabs.opensource.ncd.data

import java.io.Serializable

data class BPBGListModel(
    var total: Int = 0,
    var limit: Int? = null,
    var skip: Int? = null,
    var sortOrder: Int? = null,
    var memberId: String? = null,
    var latestRequired: Boolean? = null,
    val latestBpLog: BPLogList?= null,
    val latestGlucoseLog: GlucoseLogList?= null,
    var bpLogList: ArrayList<BPLogList>? = null,
    var glucoseLogList: ArrayList<GlucoseLogList>? = null,
    var bpThreshold: BPThreshold? = null,
    var glucoseThreshold: List<GlucoseThreshold>? = null
)

data class GlucoseThreshold(
    var fbs: Int,
    var rbs: Int,
    var hba1c: Float,
    val unit: String
) : Serializable
data class BPThreshold(
    var systolic: Int,
    var diastolic: Int
) : Serializable

data class BPLogList(
    val encounterId: Long? = null,
    val bpTakenOn: String? = null,
    val avgSystolic: Long? = null,
    val avgDiastolic: Long? = null,
    val avgPulse: Long? = null,
    val createdAt: String,
    val symptoms: ArrayList<String>? = null
)

data class GraphModel(
    var bpResponse: BPLogList? = null,
    var bgResponse: GlucoseLogList? = null,
    var index: Int,
    var size: Int,
    var isForward: Boolean? = null
)

data class GlucoseLogList(
    val encounterId: Long? = null,
    val glucoseType: String? = null,
    val glucoseValue: Float? = null,
    val glucoseUnit: String? = null,
    val glucoseDateTime: String? = null,
    val createdAt: String? = null,
    val hba1c: Float? = null,
    val hba1cUnit: String? = null,
    val hba1cDateTime:String? = null,
    val symptoms: ArrayList<String>? = null,
)