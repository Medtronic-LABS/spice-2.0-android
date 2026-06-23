package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class PatientDetailsModel(
    @SerializedName("id")
    val _id: Long,
    var tenantId: Long? = null,
    val firstName: String? = null,
    val middleName: String? = null,
    val name: String? = null,
    val memberId: String? = null,
    val lastName: String? = null,
    val age: Double? = null,
    val gender: String? = null,
    val enrollmentAt: String? = null,
    val programId: Long? = null,
    val nationalId: String? = null,
    val phoneNumber: String? = null,
    val lastAssessmentDate: String? = null,
    val cvdRiskLevel: String? = null,
    val cvdRiskScore: Double? = null,
    val avgSystolic: Double? = null,
    val avgDiastolic: Double? = null,
    val avgPulse: Double? = null,
    val bmi: Double? = null,
    val phq4Score: Int? = null,
    val glucoseValue: Double? = null,
    val glucoseType: String? = null,
    val glucoseUnit: String? = null,
    val provisionalDiagnosis: ArrayList<String>? = null,
    val isRegularSmoker: Boolean? = null,
    val confirmDiagnosis: ArrayList<String>? = null,
    val patientConfirmDiagnosis: ArrayList<String>? = null,
    val isConfirmDiagnosis: Boolean = false,
    val height: Double? = null,
    val weight: Double? = null,
    val phq9Score: Int? = null,
    val dateOfBirth: String? = null,
    val message: String? = null,
    val assessmentRequired: Boolean? = true,
    val patientTrackId: Long? = null,
    val landmark: String? = null,
    val phoneNumberCategory: String? = null,
    @SerializedName("assessmentDataRequired")
    var isAssessmentDataRequired: Boolean = false,
    @SerializedName("prescriberRequired")
    var isPrescriberRequired: Boolean = false,
    var prescriberDetails: PrescriberModel? = null,
    val gad7Score: Int? = null,
    val isPhq9: Boolean = false,
    val isGad7: Boolean = false,
    var riskPatient: Boolean = false,
    @SerializedName("redRiskPatient")
    var isRedRiskPatient: Boolean = false,
    var riskColor: String? = null,
    var phq4RiskLevel: String? = null,
    var phq9RiskLevel: String? = null,
    var gad7RiskLevel: String? = null,
    var isPregnant: Boolean = false,
    var isPregnancyRisk: Boolean? = null,
    var diagnosisComments: String? = null,
    @SerializedName("lifeStyleRequired")
    var isLifestyleRequired: Boolean = false,
    var patientLifestyles: ArrayList<LifestyleModel>? = null,
    var patientCounselorAssessment: ArrayList<LifestyleModel>? = null,
    var screeningLogId: Long? = null,
    var screeningId: Long? = null,
    var patientId: String? = null,
    val isDiabetesDiagnosis: Boolean = false,
    val isHtnDiagnosis: Boolean = false,
    var lastMenstrualPeriodDate: String? = null,
    var estimatedDeliveryDate: String? = null,
    var sdoh: SDOHModel? = null,
    var emrNumber: String? = null,
    var village: String? = null,
    var isGestationalDiabetes: Boolean? = null,
    var siteId: Long? = null,
    var comorbidities: ArrayList<InitialEncounterRequest>? = null,
    var suicidalIdeation: String? = null,
    var cageAid: Long? = null,
    var isDangerSymptom: Boolean = false,
    var countryName: String? = null,
    var countyName: String? = null,
    var subCountyName: String? = null,
    var patientStatus: String? = null,
    var sessionRequired: Boolean = false,
    var sessionCount: Int? = null,
    var hba1c: String? = null,
    var hba1cUnit: String? = null,
    var cycleCompleted: Boolean = false,
    var isSessionDropOut: Boolean = false,
    val languages: String? = null,
    val otherLanguages: String? = null,
    val piScore: String? = null,
    val ncdStatus: String? = null,
    val patientHealthHistory: PatientHistoryModel? = null,
    val familyHealthHistory: FamilyHealthModel? = null,
    val lastReviewDate: String? = null,
    val lastReviewPlace: String? = null,
    val nextMedicalReviewDate: String? = null,
    val initialReview: Boolean = false,
    val unselectedDiagnosis: ArrayList<UnselectedDiagnosis>? = null,
    var eyeCare: EyeCare? = null,
    val identityType: String? = null,
    val identityValue: String? = null,
) : java.io.Serializable

data class PregnancyDetails(
    var lastMenstrualPeriodDate: String? = null,
    var estimatedDeliveryDate: String? = null,
) : java.io.Serializable

data class UnselectedDiagnosis(
    var name: String? = null,
    var cultureValue: String? = null,
)

data class LifestyleModel(
    var comments: String? = null,
    var lifestyleAnswer: String? = null,
    var lifestyle: String? = null,
    var lifestyleType: String? = null,
) : java.io.Serializable

data class SDOHModel(
    val id: Long,
    var sdoh: HashMap<String, Any>? = null,
) : Serializable

data class PatientHistoryModel(
    val heartAttack: String? = null,
    val stroke: String? = null,
    val kidneyDisease: String? = null,
    val copd: String? = null,
)

data class FamilyHealthModel(
    val condition: String? = null,
)

data class EyeCare(
    val typeOfFrame: String? = null,
    val haveTheGlassesBeenSold: String? = null,
    val firstTimeUser: String? = null,
    val glassPower: String? = null,
    val eyeTestOutcome: String? = null,
    val typeOfGlass: String? = null,
    val referPlace: String? = null,
)
