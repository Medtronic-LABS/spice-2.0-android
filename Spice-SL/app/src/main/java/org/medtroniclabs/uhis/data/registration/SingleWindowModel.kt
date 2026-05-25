package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class SingleWindowModel(
    var diagnosis: InitialDiagnosis? = null,
    var isPregnant: Boolean? = null,
    var lifestyle: ArrayList<InitialLifeStyle>? = null,
    @SerializedName("currentMedications")
    var currentMedicationDetails: InitialCurrentMedicationDetails? = null,
    var complaints: ArrayList<Long>? = null,
    var physicalExams: ArrayList<Long>? = null,
    var physicalExamComments: String? = null,
    var clinicalNote: String? = null,
    var complaintComments: String? = null,
    var comorbidities: ArrayList<InitialComorbidities>? = null,
    var complications: ArrayList<InitialComplications>? = null,
    var patientTrackId: Long? = -1,
    var patientVisitId: Long? = null,
    var tenantId: Long? = -1,
    var nextMedicalReviewDate: String? = null,
)
