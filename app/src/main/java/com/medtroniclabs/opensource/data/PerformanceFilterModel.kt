package com.medtroniclabs.opensource.data

data class PerformanceFilterModel(
    var year: Int? = null,
    var month: Int? = null,
    var startDate: Long? = null,
    var endDate: Long? = null,
    var fromDate: String? = null,
    var toDate: String? = null,
)
