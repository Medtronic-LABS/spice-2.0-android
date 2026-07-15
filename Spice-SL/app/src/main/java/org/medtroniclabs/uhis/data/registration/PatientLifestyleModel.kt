package org.medtroniclabs.uhis.data.registration

data class PatientLifestyleModel(
    var id: Long? = null,
    var tenantId: Long? = null,
    var patientVisitId: Long? = null,
    var patientTrackId: Long? = null,
    var nutritionist: Boolean? = null,
    var lifestyles: ArrayList<Lifestyle>? = null,
    var nutritionHistoryRequired: Boolean? = null,
    var menuName: String? = null,
)

data class Lifestyle(
    var id: Long? = null,
    var patientTrackId: Long? = null,
    var lifestyleAssessment: String? = null,
    var otherNote: String? = null,
)
