package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.data.history.PatientStatus

data class MotherNeonatePncSummaryResponse(
    val pncChild: PncChild?,
    val pncMother: PncMother?
)

data class PncChild(
    val id: Any?,
    val visitNumber: Int,
    val patientReference: Any?,
    val isChildAlive: Boolean,
    val patientStatus: Any?,
    val presentingComplaints: List<String>,
    val presentingComplaintsNotes: String?,
    val physicalExaminations: List<String>,
    val physicalExaminationNotes: Any?,
    val congenitalDetect: String?,
    val cordExamination: String?,
    val breastFeeding: Boolean,
    val exclusiveBreastFeeding: Boolean,
    val clinicalNotes: String?,
    val encounter: Any?
)

data class PncMother(
    val id: Any?,
    val visitNumber: Int?,
    val patientReference: Any?,
    val patientStatus: Any?,
    val isMotherAlive: Boolean,
    val breastCondition: String?,
    val breastConditionNotes: Any?,
    val involutionsOfTheUterus: String?,
    val involutionsOfTheUterusNotes: Any?=null,
    val presentingComplaints: List<String>,
    val presentingComplaintsNotes: String?,
    val systemicExaminations: List<String>,
    val systemicExaminationsNotes: String?=null,
    val clinicalNotes: String?,
    val encounter: Any?,
    val diagnosis: ArrayList<DiagnosisDiseaseModel>? = null,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null,
    val summaryStatus:List<PatientStatus>?=null

)
