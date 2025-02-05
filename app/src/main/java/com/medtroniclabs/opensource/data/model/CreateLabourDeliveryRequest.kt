package com.medtroniclabs.opensource.data.model

import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class CreateLabourDeliveryRequest(
    val motherDTO: MotherDTO?,
    val neonateDTO: NeonateDTO?,
    val child: Child?,
)

data class Child(
    val name: String?,
    val village: String?,
    val villageId: Int?,
    val motherPatientId: String?,
    val dateOfBirth: String?,
    val patientId: String?,
    val gender: String?=null,
    val provenance: ProvanceDto,
    val phoneNumber: String?,
    val phoneNumberCategory: String?,
    val householdId: String?,
    val isChild: Boolean,
    val householdHeadRelationship: String?
)

data class LabourDTO(
    val dateAndTimeOfDelivery: String?,
    val dateAndTimeOfLabourOnset: String?,
    val deliveryAt: String?,
    val deliveryBy: String?,
    val deliveryStatus: String?,
    val deliveryByOther:String?,
    val deliveryType: String?,
    val noOfNeoNates: Int?,
    val deliveryAtOther: String?
    )

data class MotherDTO(
    val id: String?,
    val encounter: MedicalReviewEncounter?,
    val generalConditions: String?,
    val riskFactors: List<String>?,
    val signs: List<String>?,
    val labourDTO: LabourDTO?,
    val stateOfPerineum: String?,
    val status: List<String>?,
    val tear: String?,
    val ttDoseTaken: Int?,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null,
    val neonateOutcome: String?=null,
)

data class NeonateDTO(
    val birthWeight: String?,
    val encounter: MedicalReviewEncounter?,
    val gender: String?=null,
    val neonateOutcome: String?,
    val signs: List<String>?,
    val stateOfBaby: String?,
    val gestationalAge: String? = null,
    val apgarScoreOneMinuteDTO: ApgarScoreOneMinuteDTO?,
    val apgarScoreFiveMinuteDTO: ApgarScoreFiveMinuteDTO?,
    val apgarScoreTenMinuteDTO: ApgarScoreTenMinuteDTO?,
    val total: String? = null
)

data class ApgarScoreOneMinuteDTO(
    val activity: Int?,
    val pulse: Int?,
    val grimace: Int?,
    val appearance: Int?,
    val respiration: Int?,
    val oneMinuteTotalScore: Int?
)

data class ApgarScoreFiveMinuteDTO(
    val activity: Int?,
    val pulse: Int?,
    val grimace: Int?,
    val appearance: Int?,
    val respiration: Int?,
    val fiveMinuteTotalScore: Int?
)

data class ApgarScoreTenMinuteDTO(
    val activity: Int?,
    val pulse: Int?,
    val grimace: Int?,
    val appearance: Int?,
    val respiration: Int?,
    val tenMinuteTotalScore: Int?
)

data class LabourDeliverySummaryDetails(
    val motherId:String? ,
    val patientReference: String? ,
    val neonateId: String? ,
    val childPatientReference: String?
)