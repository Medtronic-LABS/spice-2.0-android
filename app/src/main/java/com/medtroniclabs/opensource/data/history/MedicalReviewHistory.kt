package com.medtroniclabs.opensource.data.history

import com.medtroniclabs.opensource.model.ReferredDate
import java.io.Serializable

data class MedicalReviewHistory(
    val id: String? = null,
    val patientReference: String? = null,
    val dateOfReview: String? = null,
    val reviewDetails: ReviewDetails? = null,
    val history: List<ReferredDate>? = null,
    val type: String? = null
)

data class NCDMedicalReviewHistory(
    val patientVisitId: String? = null,
    val history: List<ReferredDate>? = null,
    val medicalReview: MedicalReview? = null,
    val dateOfReview: String? = null
)

data class MedicalReview(
    val physicalExams: List<PhysicalExaminations>? = null,
    val complaints: List<String>? = null,
    val notes: List<String>? = null,
    val prescriptions: List<String>? = null,
    val investigations: List<String>? = null
)

data class PhysicalExaminations(
    val physicalExaminations: List<String>? = null,
    val physicalExaminationsNote: String? = null
)

data class ReviewDetails(
    val id: String? = null,
    val visitNumber: Int? = null,
    val patientReference: String? = null,
    val diagnosis: List<DiseaseInfo>? = null,
    val patientStatus: String? = null,
    val isMotherAlive: Boolean? = null,
    val breastCondition: String? = null,
    val breastConditionNotes: String? = null,
    val involutionsOfTheUterus: String? = null,
    val involutionsOfTheUterusNotes: String? = null,
    val presentingComplaints: Any? = null,
    val presentingComplaintsNotes: String? = null,
    val systemicExaminations: List<String?>? = null,
    val systemicExamination: List<String?>? = null,
    val obstetricExaminations: List<String?>? = null,
    val systemicExaminationsNotes: String? = null,
    val systemicExaminationNotes: String? = null,
    val obstetricExaminationNotes: String? = null,
    val clinicalNotes: String? = null,
    val labourDTO:LabourDTO? = null,
    val neonateOutcome:String?=null,
    val stateOfBaby:String? = null,
    val birthWeight:String? = null,
    val signs:List<String?>? = null,
)

data class DiseaseInfo(
    val diseaseCategoryId: Long? = null,
    val diseaseConditionId: Long? = null,
    val diseaseCategory: String? = null,
    val notes: String? = null,
    val diseaseCondition: String? = null,
    val type: String? = null
) : Serializable

data class LabourDTO(
    val dateAndTimeOfDelivery: String? = null,
    val dateAndTimeOfLabourOnset: String? = null,
    val deliveryType: String? = null,
    val deliveryBy: String? = null,
    val deliveryAt: String? = null,
    val deliveryStatus: String? = null,
)


