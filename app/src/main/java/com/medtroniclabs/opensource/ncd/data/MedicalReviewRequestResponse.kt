package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.history.Prescription
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class MedicalReviewRequestResponse(
    var initialMedicalReview: InitialMedicalReview? = null,
    var continuousMedicalReview: ContinuousMedicalReview? = null,
    val isPregnant: Boolean? = null,
    val patientReference: String? = null,
    val encounterReference: String? = null,
    val enrollmentType: String? = null,
    var identityValue: String? = null,
    var provenance: ProvanceDto? = null,
    val memberReference: String? = null
)

data class InitialMedicalReview(
    var currentMedications : CurrentMedications? = null,
    var comorbidities: List<Chip>? = null,
    var complications: List<Chip>? = null,
    var lifestyle: List<Chip>? = null
)

data class CurrentMedications(
    val medications: List<Chip>? = null,
    val drugAllergies: Boolean? = null,
    val adheringCurrentMed: Boolean? = null,
    val adheringMedComment: String? = null,
    val allergiesComment: String? = null
)


data class ContinuousMedicalReview(
    val physicalExams: List<Chip>? = null,
    val complaints: List<Chip>? = null,
    val complaintComments: String? = null,
    val physicalExamComments: String? = null,
    var clinicalNote: String? = null
)

data class Chip(
    val id: Long? = null,
    val name: String? = null,
    val value: String? = null,
    val other: Boolean? = null,
    val comments: String? = null,
    val answer: Answer? = null
)

data class Answer(
    var name: String? = null,
    val value: String? = null
)

data class MedicalReviewResponse(
    val memberReference: String? = null,
    val encounterReference: String? = null,
    val patientReference: String? = null,
    val patientVisitId: String? = null,
    val diagnosisType: List<String>? = null
)

data class MRSummaryResponse(
    val isSigned: Boolean? = null,
    val medicalReviewFrequency: String? = null,
    val isPregnant: Boolean? = null,
    val isInitialPregnancyReview: Boolean? = null,
    val lastMenstrualPeriodDate: String? = null,
    val estimatedDeliveryDate: String? = null,
    val physicalExams: List<String>? = emptyList(),
    val complaints: List<String>? = emptyList(),
    val physicalExamComments: String? = null,
    val compliantComments: String? = null,
    val reviewedAt: String? = null,
    val clinicalNote: String? = null,
    val comorbidities: List<String?>? = emptyList(),
    val complications: List<String?>? = emptyList(),
    val prescriptions: List<Prescription>? = emptyList(),
    val investigations: List<String>? = emptyList(),
    val confirmDiagnosis: Diagnosis? = null
)
data class Diagnosis(
    val provenanceDTO: ProvanceDto? = null,
    val diagnosis: List<NCDDiagnosisItem>? = null,
    val diagnosisNotes: String? = null,
    val patientReference: String? = null
)

