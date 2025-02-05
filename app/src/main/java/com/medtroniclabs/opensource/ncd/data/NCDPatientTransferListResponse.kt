package com.medtroniclabs.opensource.ncd.data

data class PatientTransferListResponse(
    val incomingPatientList: ArrayList<PatientTransfer>,
    val outgoingPatientList: ArrayList<PatientTransfer>
)

data class PatientTransfer(
    val id: Long,
    val transferReason: String,
    val tenantId: Long,
    val patient: PatientObject,
    val transferStatus: String,
    val transferSite: SiteObject?,
    val oldSite: SiteDetail? = null,
)

data class SiteObject(val id: Long, val name: String?)

data class PatientObject(
    val id: String? = null,
    val firstName: String,
    val lastName: String? = null,
    val age: String,
    val gender: String,
    val phoneNumber: String,
    val identityType: String,
    val identityValue: String,
    val programId: Long,
    val provisionalDiagnosis: ArrayList<String>?,
    val confirmDiagnosis: ArrayList<String>?,
    val cvdRiskLevel: String,
    val cvdRiskScore: String,
    val isRedRiskPatient: Boolean,
    val enrollmentAt: String,
    val bmi: Double,
    val pregnancyDetails: Preganancy? = null
)

data class Preganancy(
    val lastMenstrualPeriodDate: String? = null,
    val estimatedDeliveryDate: String? = null
)

data class SiteDetail(val name: String? = null)