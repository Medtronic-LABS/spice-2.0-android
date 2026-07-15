package org.medtroniclabs.uhis.data.registration

data class BadgeResponseModel(
    var prescriptionDaysCompletedCount: Int?,
    var nonReviewedTestCount: Int?,
    var nutritionLifestyleReviewedCount: Int?,
    var psychologicalCount: Int?,
)
