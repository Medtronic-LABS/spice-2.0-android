package com.medtroniclabs.opensource.data.model

import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class MotherNeonateAncRequest(
    var id: String? = null,
    var memberId: String? = null,
    var previousHistory:Boolean? = null,
    var assessmentType: String? = null,
    var patientId: String? = null,
    var presentingComplaints: List<String?>? = null,
    var presentingComplaintsNotes: String? = null,
    var patientReference: String? = null,
    var obstetricExaminations: List<String?>? = null,
    var obstetricExaminationNotes: String? = null,
    var pregnancyHistory: List<String?>? = null,
    var pregnancyHistoryNotes: String? = null,
    var fundalHeight: Double? = null,
    var visitNumber: Long? = null,
    var fetalHeartRate: Double? = null,
    var clinicalNotes: String? = null,
    var pregnancyDetails: PregnancyDetailsModel? = null,
    var encounter: MedicalReviewEncounter? = null,
    var deliveryKit: Boolean? = null
)
data class MedicalReviewEncounter(
    var id: String? = null,
    var patientId: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val referred: Boolean? = null,
    val patientReference:String?=null,
    val provenance: ProvanceDto? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val householdId: String? =null,
    val memberId: String? = null,
    var visitNumber: Int? = null
)

data class PatientEncounterResponse(
    val patientReference: String? = null,
    val encounterId: String? = null
)

data class PncSubmitResponse(
    val encounterId: String?=null,
    val patientReference: String?=null,
    val childEncounterId: String?=null,
    val childPatientReference: String?=null
)
