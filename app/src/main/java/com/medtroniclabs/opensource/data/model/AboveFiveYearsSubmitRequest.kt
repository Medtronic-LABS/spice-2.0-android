package com.medtroniclabs.opensource.data.model

data class AboveFiveYearsSubmitRequest(
    val id: String? = null,
    val assessmentType: String? = null,
    val presentingComplaints: List<String?>? = null,
    val presentingComplaintsNotes: String? = null,
    val systemicExaminations: List<String?>? = null,
    val systemicExaminationsNotes: String? = null,
    val clinicalNotes: String? = null,
    val encounter:MedicalReviewEncounter? = null,
)

