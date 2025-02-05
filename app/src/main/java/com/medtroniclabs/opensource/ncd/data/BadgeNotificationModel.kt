package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class BadgeNotificationModel(
    val patientReference: String? = null,
    val menuName: String? = null,
    val nutritionLifestyleReviewedCount: Int = 0,
    val psychologicalCount: Int = 0,
    val nonReviewedTestCount: Int = 0,
    val prescriptionDaysCompletedCount: Int = 0,
    val provenance: ProvanceDto = ProvanceDto()
)
