package com.medtroniclabs.opensource.model.assessment

data class AssessmentMemberDetails(
    val name: String,
    val gender: String,
    val dateOfBirth: String,
    val patientId: String? = null,
    val villageId: String,
    val memberId: String? = null,
    val householdId: String? = null,
    val householdLocalId: Long,
    val id:Long,
)
