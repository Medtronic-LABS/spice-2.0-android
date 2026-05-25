package org.medtroniclabs.uhis.data.registration

data class PatientMedicalReviewHistoryResponse(
    val patientMedicalReview: ArrayList<MedicalReview>,
    var patientReviewDates: ArrayList<VisitDateModel>,
    var canUpdateDate: Boolean = false,
)
