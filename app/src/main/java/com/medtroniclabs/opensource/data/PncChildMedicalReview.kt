package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.history.Investigation

data class PncChildMedicalReview(
    val id: String? = null,
    val patientId: String? = null,
    val patientReference: String? = null,
    val patientStatus: String? = null,
    val dateOfReview: String? = null,
    val reviewDetails: ReviewDetails? = null,
    val history: List<HistoryItem>? = null,
    val nextVisitDate: String? = null,
    val type: String? = null,
    val visitNumber: Int? = null
)

data class ReviewDetails(
    val pncChild: PncChildDetails? = null,
    val pncMother: PncMotherDetails? = null
)

data class PncChildDetails(
    val id: String? = null,
    val visitNumber: Int? = null,
    val patientReference: String? = null,
    val isChildAlive: Boolean? = null,
    val patientStatus: String? = null,
    val presentingComplaints: List<String?>? = null,
    val presentingComplaintsNotes: String? = null,
    val physicalExaminations: List<String?>? = null,
    val physicalExaminationNotes: String? = null,
    val congenitalDetect: String? = null,
    val cordExamination: String? = null,
    val breastFeeding: Boolean? = null,
    val exclusiveBreastFeeding: Boolean? = null,
    val clinicalNotes: String? = null,
    val encounter: String? = null,
    val prescriptions: String? = null
)

data class PncMotherDetails(
    val id: String? = null,
    val visitNumber: Int? = null,
    val patientReference: String? = null,
    val patientStatus: String? = null,
    val isMotherAlive: Boolean? = null,
    val diagnosis: List<DiagnosisDiseaseModel>? = null,
    val breastCondition: String? = null,
    val breastConditionNotes: String? = null,
    val involutionsOfTheUterus: String? = null,
    val involutionsOfTheUterusNotes: String? = null,
    val presentingComplaints: List<String?>? = null,
    val presentingComplaintsNotes: String? = null,
    val systemicExaminations: List<String?>? = null,
    val systemicExaminationsNotes: String? = null,
    val clinicalNotes: String? = null,
    val encounter: String? = null,
    val prescriptions:  List<Prescription>? = null,
    val investigations: List<Investigation>? = null
)

data class HistoryItem(
    val date: String? = null,
    val id: String? = null,
    val type: String? = null
)



