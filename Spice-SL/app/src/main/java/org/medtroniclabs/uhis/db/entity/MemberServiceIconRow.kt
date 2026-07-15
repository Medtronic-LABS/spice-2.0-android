package org.medtroniclabs.uhis.db.entity

/**
 * Lightweight projection of assessment history rows for member list service icons.
 */
data class MemberServiceIconRow(
    val memberId: Long?,
    val serviceProvided: String?,
    val visitDate: String?,
    val customStatus: ArrayList<String>?,
)
