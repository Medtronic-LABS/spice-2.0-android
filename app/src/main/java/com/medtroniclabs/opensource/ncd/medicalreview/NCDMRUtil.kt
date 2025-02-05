package com.medtroniclabs.opensource.ncd.medicalreview

import android.content.Context
import android.widget.TextView
import androidx.appcompat.widget.AppCompatEditText
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.textOrHyphen
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Other
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.history.Prescription
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.mappingkey.MemberRegistration
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams

object NCDMRUtil {
    const val MENU_ID = "MENU_ID"
    const val Complaints = "Complaints"
    const val Comorbidity = "Comorbidity"
    const val Complications = "Complications"
    const val LIFE_STYLE_SMOKE = "SMOKE"
    const val LIFE_STYLE_ALCOHOL = "ALCOHOL"
    const val LIFE_STYLE_NUT = "NUT"
    const val Yes_Currently = "Yes, currently"
    const val Yes_Past = "Yes, In the past"
    const val EncounterReference = "EncounterReference"
    const val PhysicalExamination = "PhysicalExamination"
    const val NCD = "NCD"
    const val MATERNAL_HEALTH = "MATERNALHEALTH"
    const val MENTAL_HEALTH = "MENTALHEALTH"
    const val IS_INITIAL_MR = "ISINITIALMR"
    const val IS_FEMALE = "ISFEMALE"
    const val IS_DIAGNOSIS_MISMATCH = "IS_DIAGNOSIS_MISMATCH"
    const val CONFIRM_DIAGNOSIS_TYPE = "CONFIRM_DIAGNOSIS_TYPE"
    const val CONFIRM_DIAGNOSIS_TYPE_GET = "CONFIRM_DIAGNOSIS_TYPE_GET"
    const val SUBSTANCE_DISORDER = "Substance Disorder"
    const val MENTALHEALTH = "Mental Health"
    const val HYPERTENSION = "Hypertension"
    const val DIABETES = "Diabetes"
    const val HIV_DIAGNOSIS = "hiv"
    const val HIV = "HIV"
    const val PREGNANCY = "Pregnancy"
    const val PATIENT_REFERENCE = "PatientReference"
    const val MEMBER_REFERENCE = "MemberReference"
    const val VISIT_ID = "VisitID"
    const val CurrentMedication = "CurrentMedication"
    const val Lifestyle = "Lifestyle"
    const val PatientLifestyle = "PatientLifestyle"
    const val FrequencyTypes = "FrequencyTypes"
    const val DEFAULT = "default"
    const val mmHg = "mmHg"
    const val maternalHealth = "Maternal Health"
    const val Tablet = "Tablet"
    const val Liquid_Oral = "Liquid"
    const val Injection_Injectable_Solution = "Injection"
    const val Capsule = "Capsule"
    const val TAG = "TAG"
    const val BP_TAG = "BPSummaryFragment"
    const val BG_TAG = "BGSummaryFragment"
    const val bg = "BG"
    const val fbs_code = 1
    const val HbA1c = "HbA1c"
    const val LipidProfile = "Lipid Profile"
    const val RenalFunctionTest = "Renal Function Test"
    const val rbs_code = 2
    const val fbs_rbs_code = 3
    const val hba1c_code = 4
    const val graphDetails = "history_graph_details"
    const val bp = "BP"
    const val mmhg = "mmHg"
    const val mmoll = "mmol/L"
    const val mgdl = "mg/dL"
    const val Systolic = "systolic"
    const val Diastolic = "diastolic"
    const val Pulse = "pulse"
    const val FBS = "FBS"
    const val RBS = "RBS"
    const val RBS_FBS = "RBS & FBS"
    const val rbs = "rbs"
    const val fbs = "fbs"
    const val percentage = "%"
    const val PageLimit = 15
    const val GraphPageLimit = 20
    const val BPTakenOn = "bpTakenOn"
    const val BGTakenOn = "bgTakenOn"
    const val IsPregnant = "IsPregnant"
    const val ShowNCD = "showNCD"
    const val ShowCHO = "showCHO"
    const val LifestyleResults = "lifeStyleReviewStatus"
    const val PsychologicalResults = "psychologicalReviewStatus"
    const val SMOKING = "SMOKE"
    const val ALCOHOL = "ALCOHOL"
    const val DIET_NUTRITION = "NUT"
    const val PHYSICAL_ACTIVITY = "OTHER"
    const val TYPE_DELETE = "PATIENT_DELETE"
    const val message = "message"
    const val Tobbaco = "Tobbaco"
    const val Anxiety = "Anxiety"
    const val Depression = "Depression"
    const val isEditAssessment = "isEditAssessment"
    const val questionnaireId = "questionnaireId"
    const val mentalHealth = "mentalHealth"
    const val NCDPatientStatus = "ncdPatientStatus"
    const val MentalHealthStatus = "mentalHealthStatus"
    const val SubstanceUseStatus = "substanceUseStatus"
    const val DiabetesStatus = "diabetesStatus"
    const val DiabetesYearOfDiagnosis = "diabetesYearOfDiagnosis"
    const val DiabetesControlledType = "diabetesControlledType"
    const val HypertensionStatus = "hypertensionStatus"
    const val HypertensionYearOfDiagnosis = "hypertensionYearOfDiagnosis"
    const val KnownPatient = "Known Patient"
    const val MENU_Name = "MENU_Name"
    const val VillageID = "VillageID"

    fun validateInput(
        isMandatory: Boolean = false,
        chips: ArrayList<ChipViewItemModel>,
        etPhysicalExaminationComments: AppCompatEditText,
        tvErrorMessage: TextView
    ): Boolean {
        val hasChips = chips.isNotEmpty() // Check if there are any chips selected
        val hasOtherChip = chips.any {
            it.name.equals(
                DefinedParams.Other,
                ignoreCase = true
            )
        } // Check if 'Other' chip is present
        val commentsNotBlank =
            etPhysicalExaminationComments.text?.isNotBlank() == true // Check if the comments are not blank

        // If input is mandatory, additional validation is required
        if (isMandatory) {
            // If there are chips, we need to check further
            if (hasChips) {
                // If 'Other' chip is selected, check for non-blank comments
                if (hasOtherChip) {
                    return if (commentsNotBlank) {
                        tvErrorMessage.invisible() // Hide error if comments are valid
                        true
                    } else {
                        tvErrorMessage.visible() // Show error if comments are empty
                        false
                    }
                }
                // If no 'Other' chip is selected, input is valid
                tvErrorMessage.gone()
                return true
            } else {
                // If no chips are selected and mandatory, show error
                tvErrorMessage.visible()
                return false
            }
        }

        // If chips are empty and comments are blank, input is valid
        if (!hasChips && etPhysicalExaminationComments.text?.isBlank() == true) {
            tvErrorMessage.gone()
            return true
        }

        // If chips are not empty, check for 'Other' chip and non-blank comments
        if (hasChips) {
            if (hasOtherChip) {
                return if (commentsNotBlank) {
                    tvErrorMessage.invisible() // Hide error if comments are valid
                    true
                } else {
                    tvErrorMessage.visible() // Show error if comments are empty
                    false
                }
            }
            // If no 'Other' chip is selected, input is valid
            tvErrorMessage.gone()
            return true
        }

        return true // If no other conditions matched, input is considered valid
    }

    fun validateInputForCommentOption(
        isMandatory: Boolean = false,
        chips: ArrayList<ChipViewItemModel>,
        etPhysicalExaminationComments: AppCompatEditText,
        tvErrorMessage: TextView
    ): Boolean {
        val hasChips = chips.isNotEmpty() // Check if there are any chips selected
        val commentsNotBlank =
            etPhysicalExaminationComments.text?.isNotBlank() == true // Check if the comments are not blank

        // If input is mandatory, additional validation is required
        if (isMandatory) {
            // If there are chips, we need to check further
            if (hasChips || commentsNotBlank) {
                // If no 'Other' chip is selected, input is valid
                tvErrorMessage.gone()
                return true
            } else {
                // If no chips are selected and mandatory, show error
                tvErrorMessage.visible()
                return false
            }
        }

        // If chips are empty and comments are blank, input is valid
        if (!hasChips && etPhysicalExaminationComments.text?.isBlank() == true) {
            tvErrorMessage.gone()
            return true
        }

        // If chips are not empty, check for 'Other' chip and non-blank comments
        if (hasChips || commentsNotBlank) {
            tvErrorMessage.gone()
            return true
        }

        return true // If no other conditions matched, input is considered valid
    }

    fun getTypeForDiagnoses(menu: String?): ArrayList<String> {
        return when (menu?.lowercase()) {
            NCD.lowercase() -> arrayListOf(HYPERTENSION, DIABETES,Other, HIV_DIAGNOSIS)
            MENTAL_HEALTH.lowercase() -> arrayListOf(SUBSTANCE_DISORDER, MENTALHEALTH,Other, HIV_DIAGNOSIS)
            DefinedParams.PregnancyANC.lowercase() -> arrayListOf(PREGNANCY,Other, HIV_DIAGNOSIS)
            else -> arrayListOf()
        }
    }

    fun getConfirmDiagnoses(menu: String?): ArrayList<String> {
        return when (menu?.lowercase()) {
            NCD.lowercase() -> arrayListOf(NCD, HIV)
            MENTAL_HEALTH.lowercase() -> arrayListOf(MENTALHEALTH, HIV)
            DefinedParams.PregnancyANC.lowercase() -> arrayListOf(maternalHealth, HIV)
            else -> arrayListOf()
        }
    }
    fun requestTypeForConfirmDiagnoses(menu: String?): String? {
        return when (menu?.lowercase()) {
            NCD.lowercase() -> NCD
            MENTAL_HEALTH.lowercase() -> MENTALHEALTH
            DefinedParams.PregnancyANC.lowercase() -> maternalHealth
            else -> null
        }
    }

    fun isNCDMRMetaLoaded(): Boolean {
        return SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.IS_NCD_MEDICAL_REVIEW_LOADED.name)
    }

    fun getUserName(): String {
        val userDetails = SecuredPreference.getUserDetails()
        return "${userDetails?.firstName} ${userDetails?.lastName}"
    }
    fun createPrescription(prescriptions: List<Prescription>?, context: Context): List<String>? {
        return prescriptions?.map { prescription ->
            buildString {
                append(prescription.medicationName.textOrHyphen())
                append(" - ")
                append("${prescription.dosageFormName.textOrHyphen()} / ")
                val dosageValue = prescription.dosageUnitValue?.toDoubleOrNull()?.toInt() ?: "-"
                append("${dosageValue}${prescription.dosageUnitName.textOrHyphen()} / ")
                append("${prescription.dosageFrequencyName.textOrHyphen()} / ")
                append(
                    "${prescription.prescriptionRemainingDays ?: "-"} ${
                        dayPeriod(
                            prescription.prescriptionRemainingDays,
                            context
                        )
                    }"
                )
                if (!prescription.instructionNote.isNullOrBlank())
                    append(" / " + prescription.instructionNote)
            }
        }
    }

    private fun dayPeriod(prescribedDays: Int?, context: Context): String {
        return if (prescribedDays == 1) {
            context.getString(R.string.day)
        } else {
            context.getString(R.string.days)
        }
    }

    fun printNumberedListString(items: List<String>?, context: Context): String {
        if (items.isNullOrEmpty()) {
            return context.getString(R.string.hyphen_symbol)
        }

        if (items.all { it.isBlank() }) {
            return context.getString(R.string.hyphen_symbol)
        }

        return items.filter { it.isNotBlank() }
            .mapIndexed { index, item -> "${index + 1}. $item" }
            .joinToString("\n")

    }

    fun currentUserId() = SecuredPreference.getUserFhirId()

    fun getBioDataBioMetrics(
        result: HashMap<String, Any>,
        patientData: PatientListRespModel,
        height: Double? = null,
        weight: Double? = null,
        isGlucose: Boolean = false
    ) {
        result.apply {
            put(
                Screening.bioData, hashMapOf(
                    Screening.firstName to patientData.firstName,
                    Screening.lastName to patientData.lastName,
                    Screening.phoneNumber to patientData.phoneNumber,
                    AssessmentDefinedParams.phoneNumberCategory to patientData.phoneNumberCategory,
                    Screening.identityValue to patientData.identityValue,
                    Screening.identityType to patientData.identityType
                )
            )

            val bioMetricsData = hashMapOf(
                MemberRegistration.gender to patientData.gender,
                Screening.Age to patientData.age
            )
            if (!isGlucose) {
                bioMetricsData[Screening.Height] = height ?: patientData.height
                bioMetricsData[Screening.Weight] = weight ?: patientData.weight
                bioMetricsData[Screening.BMI] = get(Screening.BMI)?.toString()
                bioMetricsData[Screening.BMI_CATEGORY] = get(Screening.BMI_CATEGORY)?.toString()
            }
            put(Screening.BioMetrics, bioMetricsData)
        }
    }
}

