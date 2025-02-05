package com.medtroniclabs.opensource.data

data class AboveFiveYearsMetaResponse(
    val systemicExaminations : List<MedicalReviewMetaItems>,
    val presentingComplaints : List<MedicalReviewMetaItems >,
    val diseaseCategories : ArrayList<DiseaseCategoryItems>,
    val medicalSupplies: List<MedicalReviewMetaItems>,
    val cost: List<MedicalReviewMetaItems>,
    val patientStatus: List<MedicalReviewMetaItems>
)
