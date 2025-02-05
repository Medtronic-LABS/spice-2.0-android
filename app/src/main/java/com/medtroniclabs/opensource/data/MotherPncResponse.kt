package com.medtroniclabs.opensource.data

data class MotherPncResponse(

    val patientStatus : List<MedicalReviewMetaItems>,
    val systemicExaminations: List<MedicalReviewMetaItems>,
    val presentingComplaints: List<MedicalReviewMetaItems>,
    val diseaseCategories: ArrayList<DiseaseCategoryItems>, // Assuming this can be any type or empty
    val dosageFrequencies: List<Any> // Assuming this can be any type or empty
)
data class NeonatePncResponse(
    val patientStatus : List<MedicalReviewMetaItems>,
    val presentingComplaints: List<MedicalReviewMetaItems>,
    val obstetricExaminations: List<MedicalReviewMetaItems>,
    val dosageFrequencies: List<Any> // Assuming this can be any type or empty
)

