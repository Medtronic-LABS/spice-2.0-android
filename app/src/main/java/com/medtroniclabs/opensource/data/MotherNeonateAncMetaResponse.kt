package com.medtroniclabs.opensource.data

data class MotherNeonateAncMetaResponse(
    val presentingComplaints: List<MedicalReviewMetaItems>,
    val obstetricExaminations: List<MedicalReviewMetaItems>,
    val pregnancyHistories: List<MedicalReviewMetaItems>,
    val bloodGroup: List<MedicalReviewMetaItems>,
    val patientStatus : List<MedicalReviewMetaItems>,
    val diseaseCategories : ArrayList<DiseaseCategoryItems>,
)
