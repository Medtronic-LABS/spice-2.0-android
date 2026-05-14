package org.medtroniclabs.uhis.data

data class FollowUpPatientModel(
    val id: Long,
    val localPatientId: Long,
    val name: String?,
    val patientId: String?,
    val phoneNumber: String?,
    val dateOfBirth: String?,
    val gender: String?,
    val reason: String?,
    val patientStatus: String?,
    val village: String?,
    val householdId: Long?,
    val householdName: String?,
    val landmark: String?,
    val type: String?,
    val encounterType: String?,
    val encounterName: String?,
    val calledAt: Long? = null,
    val successfulAttempts: Int = 0,
    val unsuccessfulAttempts: Int = 0,
    val nextVisitDate: String? = null,
    val encounterDate: String? = null,
    val isWrongNumber: Boolean,
    val updatedAt: Long,
) {
    fun getReason(default: String): String =
        if (!reason?.trim().isNullOrEmpty()) {
            reason!!.trim()
        } else {
            default
        }
}
