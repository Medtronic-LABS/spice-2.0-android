package org.medtroniclabs.uhis.data.registration

data class MedicalReviewEditModel(
    var initialMedicalReview: InitialEncounterRequest? = null,
    var continuousMedicalReview: CreateContinuousMedicalRequest? = null,
    var patientTrackId: Long? = -1,
    var patientVisitId: Long? = null,
    var nextMedicalReviewDate: String? = null,
    var tenantId: Long? = -1,
)
