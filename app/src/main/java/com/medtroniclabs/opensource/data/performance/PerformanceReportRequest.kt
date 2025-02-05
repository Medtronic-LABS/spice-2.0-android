package com.medtroniclabs.opensource.data.performance

data class PerformanceReportRequest(
    val userIds: List<Long>? = null,
    val fhirIds: List<String>? = null,
    val villageIds: List<Long>? = null,
    val fromDate: String = "2024-08-01",
    val toDate: String = "2024-08-31",
    var skip : Int = 0,
    val limit: Int = 15
)
