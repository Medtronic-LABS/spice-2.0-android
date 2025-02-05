package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.data.history.PatientStatus

data class AboveFiveYearsSummaryDetails(
    val id: String,
    val encounterId: String? = null,
    val diagnosis: ArrayList<DiagnosisDiseaseModel>? = null,
    val presentingComplaints: ArrayList<String>? = null,
    val presentingComplaintsNotes: String? = null,
    val systemicExaminations: ArrayList<String>? = null,
    val systemicExaminationsNotes: String? = null,
    val clinicalNotes: String? = null,
    val patientReference: String? = null,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null,
    val summaryStatus:List<PatientStatus>?=null
)
