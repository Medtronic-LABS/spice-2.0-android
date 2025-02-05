package com.medtroniclabs.opensource.data.model


data class MotherNeonatePncRequest(
    var pncMother: PncMother? = null,
    var pncChild: PncChild? = null,
    var child: Child? = null
)


data class PncMother(
    var id:String?=null,
    var isMotherAlive: Boolean? = null,
    var breastCondition: String? = null,
    var breastConditionNotes: String? = null,
    var involutionsOfTheUterus: String? = null,
    var involutionsOfTheUterusNotes: String? = null,
    var presentingComplaints: List<String?>? = null,
    var presentingComplaintsNotes: String? = null,
    var systemicExaminations: List<String?>? = null,
    var systemicExaminationsNotes: String? = null,
    var clinicalNotes: String? = null,
    var encounter: MedicalReviewEncounter? = null,
    var labourDTO: LabourDTO?=null,
    var neonateOutcome: String?=null
)


data class PncChild(
    var id:String?=null,
    var isChildAlive: Boolean? = null,
    var breastFeeding: Boolean? = null,
    var exclusiveBreastFeeding: Boolean? = null,
    var cordExamination: String? = null,
    var congenitalDetect: Boolean? = null,
    var presentingComplaints: List<String?>? = null,
    var presentingComplaintsNotes: String? = null,
    var physicalExaminations: List<String?>? = null,
    var physicalExaminationsNotes: String? = null,
    var clinicalNotes: String? = null,
    var encounter: MedicalReviewEncounter? = null
)
