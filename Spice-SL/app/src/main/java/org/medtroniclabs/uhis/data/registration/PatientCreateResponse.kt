package org.medtroniclabs.uhis.data.registration

data class PatientCreateResponse(
    val dateOfEnrollment: String?,
    val name: String?,
    val gender: String?,
    val age: Int?,
    val programId: String?,
    val nationalId: String?,
    val phoneNumber: String?,
    val facilityName: String?,
    val bmi: Double?,
    val dateOfBirth: String?,
    val id: String?,
    val isActive: Boolean?,
    val patientStatus: String?,
    val identityType: String?,
    val identityValue: String?,
    val village: String?,
    val villageId: String?,
    val memberReference: String?,
    val firstName: String?,
    val middleName: String?,
    val lastName: String?,
    val levelOfEducation: String?,
    val memberId: String?,
    val patientId: String?,
    val treatmentPlanResponse: Any?,
    val patientDiagnosisStatus: PatientDiagnosisStatus?,
    val patientHealthHistory: PatientHealthHistory?,
)

data class PatientDiagnosisStatus(
    val id: String?,
    val diabetesStatus: String?,
    val hypertensionStatus: String?,
    val diabetesYearOfDiagnosis: String?,
    val hypertensionYearOfDiagnosis: String?,
    val diabetesControlledType: String?,
    val diabetesDiagnosis: String?,
    val isHtnDiagnosis: Boolean?,
    val isDiabetesDiagnosis: Boolean?,
)

data class PatientHealthHistory(
    val heartAttack: String?,
    val stroke: String?,
    val kidneyDisease: String?,
    val copd: String?,
)
