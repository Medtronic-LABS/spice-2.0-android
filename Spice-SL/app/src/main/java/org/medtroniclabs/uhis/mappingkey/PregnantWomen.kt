package org.medtroniclabs.uhis.mappingkey

import android.content.Context
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams

/**
 * Contains constants and some helper methods for pregnant women registration
 */
object PregnantWomen {
    /**
     * Card : Pregnancy Details & History
     */
    const val ID_PREGNANCY_DETAILS_AND_HISTORY = "pregnancyDetailsAndHistory"

    /**
     * Date : LMP (Last Menstrual Period)
     */
    const val ID_LMP = "lmp"

    const val ID_TOO_EARLY_TITLE = "TooEarlyTitle"
    const val ID_TOO_EARLY_DESC1 = "TooEarlyDesc1"
    const val ID_TOO_EARLY_DESC2 = "TooEarlyDesc2"

    /**
     * Edit : 👶 EDD (Expected Date of Delivery)
     */
    const val ID_EDD_TITLE = "EDDTitle"
    const val ID_EDD = "EDD"

    /**
     * Edit : 📅 Gestational Week
     */
    const val ID_GESTATIONAL_WEEK_TITLE = "gestationalWeekTitle"
    const val ID_GESTATIONAL_WEEK = "gestationalWeek"

    /**
     * Spinner : Pregnancy Test
     */
    const val ID_PREGNANCY_TEST = "pregnancyTest"

    /** Pregnancy Test is shown only up to exactly this many gestational weeks (inclusive). */
    const val PREGNANCY_TEST_MAX_GESTATIONAL_WEEK = 16

    /** Maximum total gestational days for Pregnancy Test visibility (16 weeks × 7 days). */
    const val PREGNANCY_TEST_MAX_GESTATIONAL_DAYS = PREGNANCY_TEST_MAX_GESTATIONAL_WEEK * 7

    /**
     * Edit : Gravida
     */
    const val ID_GRAVIDA = "gravida"

    /**
     * Edit : Parity (Number of Total Births)
     */
    const val ID_PARITY = "parity"

    /**
     * Edit : Number of Living Children
     */
    const val ID_LIVING_CHILDREN = "livingChildren"

    /**
     * Age : Age of last child
     */
    const val ID_AGE_OF_LAST_CHILD = "ageOfLastChild"

    /**
     * Card : Health & Risk Screening
     */
    const val ID_HEALTH_RISK_SCREENING = "healthRiskScreening"

    /**
     * DialogCheckbox : Obstetric complications
     */
    const val ID_OBSTETRIC_COMPLICATIONS = "obstetricComplications"

    /**
     * DialogCheckbox : Medical complications
     */
    const val ID_MEDICAL_COMPLICATIONS = "medicalComplications"

    /**
     * DialogCheckbox : Current medical conditions
     */
    const val ID_CURRENT_MEDICAL_CONDITIONS = "currentMedicalConditions"

    const val SSP_18 = 18

    /**
     * Number days to consider whether to continue pregnancy registration or not
     */
    const val LMP_THRESHOLD_DAYS = 42

    /**
     * Options of H/O miscarriage and H/O Induced abortion to be disabled if the gap between parity and gravida is 1.
     * These  options should appear only if the gap is 2 or more indicating chances of abortion
     */
    val OBSTETRIC_COMPLICATIONS_TO_IGNORE = listOf("hoMiscarriage", "hoInducedAbortion")

    const val GRAVIDA_PARITY_IGNORE_DIFF = 2

    const val PREGNANCY_MIN_AGE_THRESHOLD = 18
    const val PREGNANCY_MAX_AGE_THRESHOLD = 35

    /**
     * Calculates risk factors based on given hashmap input
     *
     * like age related risks, short term births, multi para
     * and medical conditions and complications
     */
    fun computeRiskFactors(
        input: Map<String, Any?>,
        dateOfBirth: String,
    ): List<String> {
        val pregnancyHistory = input[ID_PREGNANCY_DETAILS_AND_HISTORY] as Map<String, Any>
        val riskScreening = input[ID_HEALTH_RISK_SCREENING] as? Map<String, Any>
        val riskFactors = arrayListOf<String>()
        val lastMenstrualDateString = pregnancyHistory[ID_LMP] as String
        val calculatedAge = DateUtils.calculateAgeToDate(dateOfBirth, lastMenstrualDateString)
        // Age related risk factors
        if (calculatedAge < PREGNANCY_MIN_AGE_THRESHOLD) {
            riskFactors.add("Age <18 years")
        } else if (calculatedAge > PREGNANCY_MAX_AGE_THRESHOLD) {
            riskFactors.add("Age >35 years")
        }
        val livingChildren = CommonUtils.getDouble(pregnancyHistory[ID_LIVING_CHILDREN])
        if (livingChildren >= 1) {
            val ageOfLastChild = pregnancyHistory[ID_AGE_OF_LAST_CHILD] as String
            val calculatedAgeOfLastChild = DateUtils.calculateAge(ageOfLastChild)
            // Short term birth
            if (calculatedAgeOfLastChild < 2) {
                riskFactors.add("Short birth spacing <2 year")
            }
        }
        val parity = CommonUtils.getDouble(pregnancyHistory[ID_PARITY])
        // Multi para
        if (parity > 3) {
            riskFactors.add("Multipara >3")
        }
        val obstetricComplications = mutableListOf<String>()
        (riskScreening?.get(ID_OBSTETRIC_COMPLICATIONS) as? List<Map<String, Any>>)?.let {
            it.forEach { complicationMap ->
                obstetricComplications.add(complicationMap[DefinedParams.Value] as String)
            }
        } ?: (riskScreening?.get(ID_OBSTETRIC_COMPLICATIONS) as? List<String>)?.let {
            obstetricComplications.addAll(it)
        }
        val medicalComplications = mutableListOf<String>()
        (riskScreening?.get(ID_MEDICAL_COMPLICATIONS) as? List<Map<String, Any>>)?.let {
            it.forEach { complicationMap ->
                medicalComplications.add(complicationMap[DefinedParams.Value] as String)
            }
        } ?: (riskScreening?.get(ID_MEDICAL_COMPLICATIONS) as? List<String>)?.let {
            medicalComplications.addAll(it)
        }
        val medicalConditions = mutableListOf<String>()
        (riskScreening?.get(ID_CURRENT_MEDICAL_CONDITIONS) as? List<Map<String, Any>>)?.let {
            it.forEach { conditionsMap ->
                medicalConditions.add(conditionsMap[DefinedParams.Value] as String)
            }
        } ?: (riskScreening?.get(ID_CURRENT_MEDICAL_CONDITIONS) as? List<String>)?.let {
            medicalConditions.addAll(it)
        }
        if (obstetricComplications.contains("previousCSection")) {
            riskFactors.add("Previous C-section")
        }
        if (obstetricComplications.contains("previousAssistedDelivery")) {
            riskFactors.add("Previous Assisted delivery")
        }
        if (obstetricComplications.contains("hoStillBirth")) {
            riskFactors.add("H/O still birth")
        }
        if (obstetricComplications.contains("hoMiscarriage") ||
            obstetricComplications.contains("hoInducedAbortion")
        ) {
            riskFactors.add("H/O miscarriage & abortion")
        }
        if (obstetricComplications.contains("previousPretermLabour")) {
            riskFactors.add("Previous Preterm Labour")
        }
        if (medicalComplications.contains("excessiveBleedingDuringAfterDelivery")) {
            riskFactors.add("H/O excessive bleeding during/after delivery")
        }
        if (medicalComplications.contains("bleedingAfter24Weeks2ndTrimester")) {
            riskFactors.add("H/O Bleeding after 2nd trimester")
        }
        if (medicalComplications.contains("preEclampsia") ||
            medicalComplications.contains("eclampsia")
        ) {
            riskFactors.add("H/O Pre-eclampsia/Eclampsia")
        }
        if (medicalComplications.contains("gestationalDiabetes")) {
            riskFactors.add("H/O GDM")
        }
        if (medicalComplications.contains("infectionSepsis")) {
            riskFactors.add("H/O Infection/Sepsis during pregnancy")
        }
        if (medicalComplications.contains("severeAnemia")) {
            riskFactors.add("H/O Severe Anemia")
        }
        if (medicalConditions.contains("highBloodPressure")) {
            riskFactors.add("Known patient of HTN")
        }
        if (medicalConditions.contains("diabetes")) {
            riskFactors.add("Known patient of DM")
        }
        if (medicalConditions.contains("heartDisease")) {
            riskFactors.add("Known patient of Heart Disease")
        }
        if (medicalConditions.contains("tuberculosis")) {
            riskFactors.add("Existing patient of Tuberculosis")
        }
        if (medicalConditions.contains("asthma")) {
            riskFactors.add("Known patient of Asthma")
        }
        if (medicalConditions.contains("thyroidDisease")) {
            riskFactors.add("Known patient of Thyroid disease")
        }
        if (medicalConditions.contains("epilepsy")) {
            riskFactors.add("Known patient of Epilepsy")
        }
        if (medicalConditions.contains("kidneyDisease")) {
            riskFactors.add("Known patient of Kidney Disease")
        }
        if (medicalConditions.contains("liverDisease")) {
            riskFactors.add("Known patient of Liver Disease")
        }
        if (medicalConditions.contains("autoimmuneDisease")) {
            riskFactors.add("Known patient of Autoimmune Disease")
        }
        return riskFactors
    }

    /**
     * Localized display for a risk string produced by [computeRiskFactors] (internal keys are English).
     */
    fun riskFactorDisplayText(
        riskFactor: String,
        context: Context,
    ): String {
        val resId =
            when (riskFactor) {
                "Age <18 years" -> R.string.risk_factor_age_under_18
                "Age >35 years" -> R.string.risk_factor_age_over_35
                "Short birth spacing <2 year" -> R.string.risk_factor_short_birth_spacing
                "Multipara >3" -> R.string.risk_factor_multipara
                else -> return riskFactor
            }
        return context.getString(resId)
    }
}
