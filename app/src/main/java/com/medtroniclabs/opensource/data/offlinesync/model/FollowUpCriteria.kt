package com.medtroniclabs.opensource.data.offlinesync.model

data class FollowUpCriteria(
    val malaria: Int,
    val pneumonia: Int,
    val diarrhea: Int,
    val muac: Int,
    val escalation: Int,
    val referral: Int,
    val ancVisit: Int,
    val pncVisit: Int,
    val childVisit: Int,
    val successfulAttempts: Int,
    val unsuccessfulAttempts: Int
)
