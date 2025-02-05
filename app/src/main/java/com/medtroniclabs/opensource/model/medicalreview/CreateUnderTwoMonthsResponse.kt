package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.DiagnosisDiseaseModel
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.history.Investigation
import com.medtroniclabs.opensource.data.history.PatientStatus

data class CreateUnderTwoMonthsResponse(
    val encounterId: String? = null,
    val patientReference: String?,
    val type: String? = null
)


data class SummaryDetails(
    val id: String,
    val clinicalNotes: String?,
    val presentingComplaints: String?,
    val examination: Map<String, List<ExaminationDetail>>?,
    val patientStatus:String?,
    val diagnosis: ArrayList<DiagnosisDiseaseModel>? = null,
    val prescriptions: List<Prescription>? = null,
    val investigations: List<Investigation>? = null,
    var examinationDisplayNames:Map<String,String>?=null,
    val summaryStatus:List<PatientStatus>?=null

)

data class ExaminationDetail(
    val title: String?,
    val value: Any? = null
)


