package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.data.history.PatientStatus

data class MotherNeonateAncSummaryModel(
    val id: String? = null,
    val presentingComplaints: List<String?>? = null,
    val presentingComplaintsNotes: String? = null,
    val obstetricExaminations: List<String?>? = null,
    val obstetricExaminationNotes: String? = null,
    val clinicalNotes: String? = null,
    val bmi: Double? = null,
    val fundalHeight: Double? = null,
    val fetalHeartRate: Double? = null,
    val systolic: Double? = null,
    val diastolic: Double? = null,
    val pulse: Double? = null,
    val weight: Double? = null,
    val height: Double? = null,
    val dateOfReview: String? = null,
    val visitNumber:String? = null,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null,
    val summaryStatus:List<PatientStatus>?=null

)