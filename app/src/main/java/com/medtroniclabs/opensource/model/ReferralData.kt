package com.medtroniclabs.opensource.model

data class ReferralData(
    val id: String? = null,
    val referredBy: String? = null,
    val phoneNumber: String? = null,
    val referredTo: String? = null,
    val patientStatus: String? = null,
    val referredReason: String? = null,
    val dateOfOnset: String? = null,
    val referredDate: String? = null,
    val referredDates: List<ReferredDate>? = null
)

data class ReferredDate(
    val id: String? = null,
    val date: String? = null,
    val type: String? = null
)

