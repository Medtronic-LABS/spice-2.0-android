package org.medtroniclabs.uhis.data.offlinesync.model

data class FollowUpCriteria(
    val malaria: Int,
    val pneumonia: Int,
    val diarrhea: Int,
    val muac: Int,
    val referral: Int,
    val screeningRetryAttempts: Int,
)
