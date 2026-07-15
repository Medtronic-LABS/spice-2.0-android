package org.medtroniclabs.uhis.data.model

data class UpdatePatientCallRegister(
    val callRegisterId: Long,
    val status: String,
    val duration: Double? = null,
    val notes: String? = null,
    val callType: String? = null,
    var isWillingToVisitUHC: Boolean? = null,
    val visitRejectReason: String? = null,
    val otherVisitRejectReason: String? = null,
    val unSuccessfulCallReason: String? = null,
    val wrongNumber: Boolean = false,
)
