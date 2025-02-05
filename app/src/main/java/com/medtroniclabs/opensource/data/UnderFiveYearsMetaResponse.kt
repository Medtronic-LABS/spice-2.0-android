package com.medtroniclabs.opensource.data

data class UnderFiveYearsMetaResponse(
    val systemicExaminations: ArrayList<MedicalReviewMetaItems>,
    val examinations : ArrayList<ExaminationListItems>,
    val diseaseCategories : ArrayList<DiseaseCategoryItems>,
    val patientStatus : ArrayList<MedicalReviewMetaItems>,
    val immunisationStatus: ArrayList<MedicalReviewMetaItems>,
    val muac : ArrayList<MedicalReviewMetaItems>,
    val symptoms : ArrayList<MedicalReviewMetaItems>
)