package org.medtroniclabs.uhis.ui.assessment.referrallogic

import org.medtroniclabs.uhis.appextensions.getLongTime
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import kotlin.collections.get
import kotlin.math.abs

/**
 * Utility class for evaluating ANC assessment conditions
 * Provides reusable helper functions for both AssessmentRMNCHFragment and AssessmentRMNCHSummaryFragment
 */
object ANCAssessmentEvaluator {
    /**
     * Helper method to get value from nested structure
     * Checks all ANC form groups first, then top level
     */
    fun getValueFromNestedMap(
        resultMap: HashMap<String, Any>,
        key: String,
    ): Any? {
        // Check all ANC form groups for the field
        for (groupId in AssessmentDefinedParams.ANC_FORM_GROUPS) {
            val groupMap = resultMap[groupId] as? Map<*, *>
            groupMap?.get(key)?.let { return it }
        }

        // If not found in any group, check top level
        return resultMap[key]
    }

    /**
     * Filters out "none" option from illness list
     * Used to exclude "none" from conditional checks and treatment dialog
     * @param illnessList List of illness items (can be ArrayList<Map<*, *>> or ArrayList<HashMap<String, Any>>)
     * @return Filtered list excluding "none" entries
     */
    private fun filterOutNoneOption(illnessList: ArrayList<*>?): ArrayList<*> {
        if (illnessList.isNullOrEmpty()) {
            @Suppress("UNCHECKED_CAST")
            return arrayListOf<Any>() as ArrayList<*>
        }

        @Suppress("UNCHECKED_CAST")
        return illnessList.filter { illness ->
            val illnessMap = illness as? Map<*, *> ?: return@filter true
            val value = illnessMap[DefinedParams.Value]?.toString()?.lowercase() ?: ""

            // Filter out if id is "none"
            value != DefinedParams.None.lowercase()
        } as ArrayList<*>
    }

    /**
     * Generic helper method to check if a specific chronic illness exists
     */
    fun hasChronicIllness(
        resultMap: HashMap<String, Any>,
        illnessName: String,
    ): Boolean {
        val existingIllness = getValueFromNestedMap(resultMap, AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS) as? ArrayList<*> ?: return false
        val filteredIllness = filterOutNoneOption(existingIllness)
        return filteredIllness.any { illness ->
            val illnessMap = illness as? Map<*, *> ?: return@any false
            val value = illnessMap[DefinedParams.Value]?.toString() ?: ""
            value.contains(illnessName, ignoreCase = true)
        }
    }

    /**
     * Check if a specific illness is in the treatment list
     */
    fun isIllnessOnTreatment(
        resultMap: HashMap<String, Any>,
        illnessName: String,
    ): Boolean {
        val treatmentList = getValueFromNestedMap(resultMap, AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT) as? ArrayList<*> ?: return false
        return treatmentList.any { treatment ->
            val treatmentMap = treatment as? Map<*, *> ?: return@any false
            val value = treatmentMap[DefinedParams.Value]?.toString() ?: ""
            value.contains(illnessName, ignoreCase = true)
        }
    }

    /**
     * Check if any chronic illness (Diabetes, Heart Disease, TB, Asthma, Thyroid, Kidney Disease) exists
     */
    fun hasAnyChronicIllness(resultMap: HashMap<String, Any>): Boolean =
        hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_DM) ||
            hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_HEART_DISEASE) ||
            hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_TUBERCULOSIS) ||
            hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_ASTHMA) ||
            hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_THYROID) ||
            hasChronicIllness(resultMap, AssessmentDefinedParams.ILLNESS_KIDNEY_DISEASE)

    /**
     * Check if chronic illness exists but is NOT on treatment
     */
    fun hasChronicIllnessNotOnTreatment(resultMap: HashMap<String, Any>): Boolean {
        if (!hasAnyChronicIllness(resultMap)) return false

        // Check each chronic illness - if any exists but is not in treatment, return true
        val chronicIllnesses = listOf(
            AssessmentDefinedParams.ILLNESS_DM,
            AssessmentDefinedParams.ILLNESS_HEART_DISEASE,
            AssessmentDefinedParams.ILLNESS_TUBERCULOSIS,
            AssessmentDefinedParams.ILLNESS_ASTHMA,
            AssessmentDefinedParams.ILLNESS_THYROID,
            AssessmentDefinedParams.ILLNESS_KIDNEY_DISEASE,
        )

        return chronicIllnesses.any { illnessName ->
            hasChronicIllness(resultMap, illnessName) && !isIllnessOnTreatment(resultMap, illnessName)
        }
    }

    /**
     * Check if chronic illness exists AND is on treatment
     */
    fun hasChronicIllnessWithTreatment(resultMap: HashMap<String, Any>): Boolean {
        if (!hasAnyChronicIllness(resultMap)) return false

        // Check each chronic illness - if any exists and is in treatment, return true
        val chronicIllnesses = listOf(
            AssessmentDefinedParams.ILLNESS_DM,
            AssessmentDefinedParams.ILLNESS_HEART_DISEASE,
            AssessmentDefinedParams.ILLNESS_TUBERCULOSIS,
            AssessmentDefinedParams.ILLNESS_ASTHMA,
            AssessmentDefinedParams.ILLNESS_THYROID,
            AssessmentDefinedParams.ILLNESS_KIDNEY_DISEASE,
        )

        return chronicIllnesses.any { illnessName ->
            hasChronicIllness(resultMap, illnessName) && isIllnessOnTreatment(resultMap, illnessName)
        }
    }

    private fun isLowHeight(resultMap: HashMap<String, Any>): Boolean {
        val height = CommonUtils.getDoubleOrNull(
            getValueFromNestedMap(
                resultMap,
                AssessmentDefinedParams.HEIGHT,
            ),
        )

        return height != null && height > 0.0 && height < 145.0
    }

    private fun isLowWeight(resultMap: HashMap<String, Any>): Boolean {
        val weight = CommonUtils.getDoubleOrNull(
            getValueFromNestedMap(
                resultMap,
                AssessmentDefinedParams.WEIGHT,
            ),
        )

        return weight != null && weight > 0.0 && weight < 45.0
    }

    /**
     * Calculate member age from date of birth
     */
    fun getMemberAge(dateOfBirth: String?): Int? = dateOfBirth?.let { DateUtils.calculateAge(it) }

    /**
     * Calculate birth spacing in years from age of last child
     */
    fun calculateBirthSpacing(ageOfLastChild: String?): Double? =
        ageOfLastChild?.let {
            try {
                DateUtils.calculateAge(it).toDouble()
            } catch (_: Exception) {
                null
            }
        }

    /**
     * Check if high risk pregnancy condition is met
     * High risk if: short birth spacing <2 years OR Age <18 OR >35 OR Multipara>3
     */
    fun isHighRiskPregnancy(
        dateOfBirth: String?,
        pregnancyDetail: PregnancyDetail?,
    ): Boolean {
        // Check age
        val age = getMemberAge(dateOfBirth)
        val isAgeRisk = age != null && (age < AssessmentDefinedParams.AGE_MIN_THRESHOLD || age > AssessmentDefinedParams.AGE_MAX_THRESHOLD)

        // Check birth spacing
        val birthSpacing = calculateBirthSpacing(pregnancyDetail?.ageOfLastChild)
        val isBirthSpacingRisk = birthSpacing != null && birthSpacing < AssessmentDefinedParams.BIRTH_SPACING_THRESHOLD_YEARS

        // Check multipara - store in local variable to avoid smart cast issue
        val parity = pregnancyDetail?.parity
        val isMultiparaRisk = parity != null && parity > AssessmentDefinedParams.MULTIPARA_THRESHOLD

        return isAgeRisk || isBirthSpacingRisk || isMultiparaRisk
    }

    /**
     * Suspected/Existing Case of Diabetes (urine sugar/blood sugar/ known patient)
     */
    fun isSuspectedDiabetes(resultMap: HashMap<String, Any>): Boolean {
        // Check urine sugar
        val urinarySugar = getValueFromNestedMap(resultMap, AssessmentDefinedParams.URINARY_SUGAR) as? String
        if (urinarySugar == AssessmentDefinedParams.VALUE_PRESENT) return true

        // Check blood sugar fasting
        val bloodSugarFasting = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.BLOOD_SUGAR_FASTING) as? Number)?.toDouble()
        if (bloodSugarFasting != null && bloodSugarFasting >= AssessmentDefinedParams.BLOOD_SUGAR_FASTING_THRESHOLD) return true

        // Check blood sugar random
        val bloodSugarRandom = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.BLOOD_SUGAR_RANDOM) as? Number)?.toDouble()
        return bloodSugarRandom != null && bloodSugarRandom >= AssessmentDefinedParams.BLOOD_SUGAR_RANDOM_THRESHOLD
    }

    /**
     * Check if suspected pre-eclampsia condition is met
     * Suspected Pre-eclampsia (Urine Albumin or Edema WITH BP≥140/90)/High BP
     */
    fun isSuspectedPreEclampsia(resultMap: HashMap<String, Any>): Boolean {
        // Get BP values
        val systolic = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.SYSTOLIC) as? String)?.toDouble()
            ?: (getValueFromNestedMap(resultMap, AssessmentDefinedParams.SYSTOLIC) as? Number)?.toDouble() ?: 0.0
        val diastolic = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.DIASTOLIC) as? String)?.toDouble()
            ?: (getValueFromNestedMap(resultMap, AssessmentDefinedParams.DIASTOLIC) as? Number)?.toDouble() ?: 0.0
        val isHighBP = systolic >= AssessmentDefinedParams.HIGH_BP_SYSTOLIC_THRESHOLD || diastolic >= AssessmentDefinedParams.HIGH_BP_DIASTOLIC_THRESHOLD

        // Check urinary albumin
        val urinaryAlbumin = getValueFromNestedMap(resultMap, AssessmentDefinedParams.URINARY_ALBUMIN) as? String

        // Check edema
        val edema = getValueFromNestedMap(resultMap, AssessmentDefinedParams.EDEMA) as? String

        // Condition: (Urine Albumin OR Edema) WITH (BP≥140/90 OR existing HTN with normal BP)
        val hasUrineAlbuminOrEdema = urinaryAlbumin == AssessmentDefinedParams.VALUE_PRESENT || edema == AssessmentDefinedParams.VALUE_PRESENT

        return hasUrineAlbuminOrEdema && isHighBP
    }

    /**
     * Check if pregnancy-related medical complications exist
     * Checks for: H/O Convulsions, H/O Postpartum hemorrhage, H/O Severe Anemia, H/O GDM
     */
    fun hasPregnancyRelatedMedicalComplications(resultMap: HashMap<String, Any>): Boolean {
        val previousComplications = getValueFromNestedMap(resultMap, AssessmentDefinedParams.PREVIOUS_PREGNANCY_COMPLICATIONS) ?: return false

        // Handle list of maps (with id/name) or list of strings (IDs)
        val complicationsList = when (previousComplications) {
            is ArrayList<*> -> previousComplications
            is List<*> -> previousComplications
            else -> return false
        }

        // IDs to check for: convulsions, postpartum_hemorrhage, severe_anemia, gestational_diabetes
        val targetComplicationIds = listOf(
            AssessmentDefinedParams.COMPLICATION_CONVULSIONS,
            AssessmentDefinedParams.COMPLICATION_POSTPARTUM_HEMORRHAGE,
            AssessmentDefinedParams.COMPLICATION_SEVERE_ANEMIA,
            AssessmentDefinedParams.COMPLICATION_GESTATIONAL_DIABETES,
        )

        return complicationsList.any { complication ->
            when (complication) {
                is Map<*, *> -> {
                    // Handle map structure with id/name
                    val value = complication[DefinedParams.Value]?.toString() ?: ""
                    val name = complication[DefinedParams.NAME]?.toString() ?: ""
                    targetComplicationIds.any { targetId ->
                        value.equals(targetId, ignoreCase = true) || name.contains(targetId, ignoreCase = true)
                    }
                }

                is String -> {
                    // Handle string ID directly
                    targetComplicationIds.any { targetId ->
                        complication.equals(targetId, ignoreCase = true)
                    }
                }

                else -> false
            }
        }
    }

    /**
     * Evaluate Emergency Referral conditions
     * Returns list of condition texts that match
     */
    fun evaluateEmergencyReferralConditions(
        resultMap: HashMap<String, Any>,
        ancVisitNumber: Int,
        pregnancyDetail: PregnancyDetail?,
        gestationalAgeWeeks: Double?,
    ): List<String> {
        val conditions = mutableListOf<String>()

        // 1. Suspected Pre-eclampsia
        if (isSuspectedPreEclampsia(resultMap)) {
            conditions.add(ANCUrgentReferrals.SUSPECTED_PRE_ECLAMPSIA.value + "::" + ANCUrgentReferrals.SUSPECTED_PRE_ECLAMPSIA.cultureValue)
        }

        // 2. High Fever - >=102F
        val temperature =
            (getValueFromNestedMap(resultMap, AssessmentDefinedParams.TEMPERATURE) as? Number)?.toDouble()?.takeIf { it > 0.0 }
        if (temperature != null && temperature >= AssessmentDefinedParams.TEMP_HIGH_FEVER_THRESHOLD) {
            conditions.add(ANCUrgentReferrals.HIGH_FEVER.value + "::" + ANCUrgentReferrals.HIGH_FEVER.cultureValue)
        }

        // 3. Abnormal fundal height
        val fundalHeightValue = getValueFromNestedMap(resultMap, AssessmentDefinedParams.FUNDAL_HEIGHT)
        if (evaluateFundalHeightStatus(fundalHeightValue, gestationalAgeWeeks)?.first == AssessmentDefinedParams.STATUS_HIGH_RISK) {
            conditions.add(ANCUrgentReferrals.ABNORMAL_FUNDAL_HEIGHT.value + "::" + ANCUrgentReferrals.ABNORMAL_FUNDAL_HEIGHT.cultureValue)
        }

        // 4. Abnormal weight gain
        val weightValue = getValueFromNestedMap(resultMap, AssessmentDefinedParams.WEIGHT)
        if (evaluateAncWeightStatus(weightValue, ancVisitNumber, pregnancyDetail)?.first == AssessmentDefinedParams.STATUS_ABNORMAL) {
            conditions.add(ANCUrgentReferrals.ABNORMAL_WEIGHT_GAIN.value + "::" + ANCUrgentReferrals.ABNORMAL_WEIGHT_GAIN.cultureValue)
        }

        // 5. Pulse - >90 or <60
        val pulse = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.PULSE) as? Number)?.toDouble()?.takeIf { it > 0.0 }
        if (pulse != null && (pulse > AssessmentDefinedParams.PULSE_HIGH_THRESHOLD || pulse < AssessmentDefinedParams.PULSE_LOW_THRESHOLD)) {
            conditions.add(ANCUrgentReferrals.ABNORMAL_PULSE.value + "::" + ANCUrgentReferrals.ABNORMAL_PULSE.cultureValue)
        }

        // 6. Severe Anemia <8g/dl
        val hemoglobin =
            (getValueFromNestedMap(resultMap, AssessmentDefinedParams.HEMOGLOBIN) as? Number)?.toDouble()?.takeIf { it > 0.0 }
        if (hemoglobin != null && hemoglobin < AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD) {
            conditions.add(ANCUrgentReferrals.SEVERE_ANEMIA.value + "::" + ANCUrgentReferrals.SEVERE_ANEMIA.cultureValue)
        }

        // 7. Urinary Bilirubin present
        val urinaryBilirubin = getValueFromNestedMap(resultMap, AssessmentDefinedParams.URINARY_BILIRUBIN) as? String
        if (urinaryBilirubin == AssessmentDefinedParams.VALUE_PRESENT) {
            conditions.add(ANCUrgentReferrals.URINARY_BILIRUBIN.value + "::" + ANCUrgentReferrals.URINARY_BILIRUBIN.cultureValue)
        }

        // 8. PW with existing chronic illnesses and not on treatment
        if (hasChronicIllnessNotOnTreatment(resultMap)) {
            conditions.add(ANCUrgentReferrals.CHRONIC_ILLNESS_NOT_ON_TREATMENT.value + "::" + ANCUrgentReferrals.CHRONIC_ILLNESS_NOT_ON_TREATMENT.cultureValue)
        }

        return conditions
    }

    /**
     * Evaluate Non-Emergency Referral conditions
     * Returns list of condition texts that match
     * @param hasOtherSelected If true, adds "Any Other" to the conditions (when "other" option is selected in danger signs)
     */
    fun evaluateNonEmergencyReferralConditions(
        resultMap: HashMap<String, Any>,
        dateOfBirth: String?,
        hasOtherSelected: Boolean = false,
        pregnancyDetail: PregnancyDetail?,
    ): List<String> {
        val conditions = mutableListOf<String>()

        // 1. High risk pregnancy (short birth spacing <2 years/Age <18 years or >35 years/Multipara>3)
        if (isHighRiskPregnancy(dateOfBirth, pregnancyDetail)) {
            conditions.add(ANCNonUrgentReferrals.HIGH_RISK_PREGNANCY.value + "::" + ANCNonUrgentReferrals.HIGH_RISK_PREGNANCY.cultureValue)
        }

        // 2. Moderate Anemia (Hb-<10)
        val hemoglobin =
            (getValueFromNestedMap(resultMap, AssessmentDefinedParams.HEMOGLOBIN) as? Number)?.toDouble()?.takeIf { it > 0.0 }
        if (hemoglobin != null &&
            hemoglobin < AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD &&
            hemoglobin >= AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD
        ) {
            conditions.add(ANCNonUrgentReferrals.MODERATE_ANEMIA.value + "::" + ANCNonUrgentReferrals.MODERATE_ANEMIA.cultureValue)
        }

        // 3. Mild Anemia (Hb < 11)
        if (hemoglobin != null &&
            hemoglobin >= AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD &&
            hemoglobin < AssessmentDefinedParams.HEMOGLOBIN_MILD_ANEMIA_THRESHOLD
        ) {
            conditions.add(ANCNonUrgentReferrals.MILD_ANEMIA.value + "::" + ANCNonUrgentReferrals.MILD_ANEMIA.cultureValue)
        }

        // 4. Suspected/Existing Case of Diabetes
        if (isSuspectedDiabetes(resultMap)) {
            conditions.add(ANCNonUrgentReferrals.SUSPECTED_DIABETES.value + "::" + ANCNonUrgentReferrals.SUSPECTED_DIABETES.cultureValue)
        }

        val systolic = CommonUtils.getDoubleOrNull(getValueFromNestedMap(resultMap, AssessmentDefinedParams.SYSTOLIC))?.takeIf { it > 0.0 }
        val diastolic = CommonUtils.getDoubleOrNull((getValueFromNestedMap(resultMap, AssessmentDefinedParams.DIASTOLIC)))?.takeIf { it > 0.0 }
        // 5. Low BP
        if ((systolic != null && systolic <= AssessmentDefinedParams.LOW_BP_SYSTOLIC_THRESHOLD) ||
            (diastolic != null && diastolic <= AssessmentDefinedParams.LOW_BP_DIASTOLIC_THRESHOLD)
        ) {
            conditions.add(ANCNonUrgentReferrals.LOW_BP.value + "::" + ANCNonUrgentReferrals.LOW_BP.cultureValue)
        }

        // 6. PW with existing chronic illnesses with treatment
        if (hasChronicIllnessWithTreatment(resultMap)) {
            conditions.add(
                ANCNonUrgentReferrals.CHRONIC_ILLNESS_WITH_TREATMENT.value + "::" + ANCNonUrgentReferrals.CHRONIC_ILLNESS_WITH_TREATMENT.cultureValue,
            )
        }

        // 7. Mild Fever - 100-101.9
        val temperature =
            (getValueFromNestedMap(resultMap, AssessmentDefinedParams.TEMPERATURE) as? Number)?.toDouble()?.takeIf { it > 0.0 }
        if (temperature != null &&
            temperature >= AssessmentDefinedParams.TEMP_FEVER_MIN_THRESHOLD &&
            temperature <= AssessmentDefinedParams.TEMP_FEVER_MAX_THRESHOLD
        ) {
            conditions.add(ANCNonUrgentReferrals.MILD_FEVER.value + "::" + ANCNonUrgentReferrals.MILD_FEVER.cultureValue)
        }

        // 8. H/O Preg related medical complications (H/O Convulsions/ H/O Postpartum hemorrhage/H/O Severe Anemia /H/O GDM)
        if (hasPregnancyRelatedMedicalComplications(resultMap)) {
            conditions.add(
                ANCNonUrgentReferrals.PREGNANCY_RELATED_MEDICAL_COMPLICATIONS.value + "::" +
                    ANCNonUrgentReferrals.PREGNANCY_RELATED_MEDICAL_COMPLICATIONS.cultureValue,
            )
        }

        // Low Height (<145 cm)
        if (isLowHeight(resultMap)) {
            conditions.add(
                ANCNonUrgentReferrals.LOW_HEIGHT.value +
                    "::" +
                    ANCNonUrgentReferrals.LOW_HEIGHT.cultureValue,
            )
        }

        // Low Weight (<45 kg)
        if (isLowWeight(resultMap)) {
            conditions.add(
                ANCNonUrgentReferrals.LOW_WEIGHT.value +
                    "::" +
                    ANCNonUrgentReferrals.LOW_WEIGHT.cultureValue,
            )
        }
        // 9. Any Other - if "other" option is selected in danger signs
        if (hasOtherSelected) {
            conditions.add(ANCNonUrgentReferrals.OTHER.value + "::" + ANCNonUrgentReferrals.OTHER.cultureValue)
        }

        return conditions
    }

    /**
     * Evaluate all gap conditions in ANC
     * Returns list of gap condition texts that match
     */
    fun evaluateGapsInANC(
        resultMap: HashMap<String, Any>,
        gestationalAgeWeeks: Double?,
        pregnancyDetail: PregnancyDetail?,
        formGenerator: FormGenerator,
    ): List<String> {
        val gaps = mutableListOf<String>()

        // 1. TT vaccination incomplete >20 weeks
        val ttTdCompleted = getValueFromNestedMap(resultMap, AssessmentDefinedParams.TT_TD_COMPLETED) as? String
        if ((gestationalAgeWeeks ?: 0.0) > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_20 &&
            (
                ttTdCompleted.isNullOrBlank() ||
                    ttTdCompleted.equals(AssessmentDefinedParams.NO, ignoreCase = true)
            )
        ) {
            gaps.add(ANCGaps.TT_VACCINATION_INCOMPLETE.value + "::" + ANCGaps.TT_VACCINATION_INCOMPLETE.cultureValue)
        }

        // 2. USG not done >36 weeks
        val ultrasound = getValueFromNestedMap(resultMap, AssessmentDefinedParams.ULTRASOUND) as? String
        if (ultrasound != null &&
            ultrasound.equals(AssessmentDefinedParams.VALUE_NOT_DONE, ignoreCase = true) &&
            gestationalAgeWeeks != null &&
            gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_36
        ) {
            gaps.add(ANCGaps.USG_NOT_DONE.value + "::" + ANCGaps.USG_NOT_DONE.cultureValue)
        }

        // 3. ANC with Doctor not done >36 weeks
        val ancFromDoctor = getValueFromNestedMap(resultMap, AssessmentDefinedParams.ANC_FROM_MEDICAL_DOCTOR) as? String
        if (ancFromDoctor != null &&
            ancFromDoctor.equals(AssessmentDefinedParams.VALUE_NO, ignoreCase = true) &&
            gestationalAgeWeeks != null &&
            gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_36
        ) {
            gaps.add(ANCGaps.ANC_WITH_DOCTOR_NOT_DONE.value + "::" + ANCGaps.ANC_WITH_DOCTOR_NOT_DONE.cultureValue)
        }

        // 4. Less than 3 ANCs completed at end of 36 weeks
        val ancVisitNo = pregnancyDetail?.ancVisitNo ?: 0L
        if (ancVisitNo < AssessmentDefinedParams.MIN_ANC_VISITS_REQUIRED &&
            gestationalAgeWeeks != null &&
            gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_36
        ) {
            gaps.add(ANCGaps.LESS_THAN_3_ANCS.value + "::" + ANCGaps.LESS_THAN_3_ANCS.cultureValue)
        }

        // 5. Inadequate /Non consumption IFA
        if (formGenerator.isViewVisible(AssessmentDefinedParams.IFA_TOTAL_CONSUMED)) {
            val ifaConsumed = (getValueFromNestedMap(resultMap, AssessmentDefinedParams.IFA_TOTAL_CONSUMED) as? String)?.toIntOrNull()
            if (ifaConsumed == null || ifaConsumed < AssessmentDefinedParams.TABLET_CONSUMPTION_THRESHOLD) {
                gaps.add(ANCGaps.INADEQUATE_IFA.value + "::" + ANCGaps.INADEQUATE_IFA.cultureValue)
            }
        }

        // 6. Inadequate /Non consumption Calcium
        if (formGenerator.isViewVisible(AssessmentDefinedParams.CALCIUM_TOTAL_CONSUMED)) {
            val calciumConsumed =
                (getValueFromNestedMap(resultMap, AssessmentDefinedParams.CALCIUM_TOTAL_CONSUMED) as? String)?.toIntOrNull()
            if (calciumConsumed == null || calciumConsumed < AssessmentDefinedParams.TABLET_CONSUMPTION_THRESHOLD) {
                gaps.add(ANCGaps.INADEQUATE_CALCIUM.value + "::" + ANCGaps.INADEQUATE_CALCIUM.cultureValue)
            }
        }

        // 7. Facility not identified for institutional delivery
        val facilityIdentified = getValueFromNestedMap(resultMap, AssessmentDefinedParams.FACILITY_IDENTIFIED_FOR_DELIVERY) as? String
        if (facilityIdentified == AssessmentDefinedParams.FACILITY_NOT_IDENTIFIED) {
            gaps.add(ANCGaps.FACILITY_NOT_IDENTIFIED.value + "::" + ANCGaps.FACILITY_NOT_IDENTIFIED.cultureValue)
        }

        // 8. Planned for Home Delivery
        if (facilityIdentified == AssessmentDefinedParams.FACILITY_HOME_DELIVERY) {
            gaps.add(ANCGaps.PLANNED_HOME_DELIVERY.value + "::" + ANCGaps.PLANNED_HOME_DELIVERY.cultureValue)
        }

        return gaps
    }

    /**
     * 6. Fundal Height - High risk if fundal height is not within gestational age ± 2 cm
     */
    fun evaluateFundalHeightStatus(
        value: Any?,
        gestationalAgeWeeks: Double?,
    ): Pair<String?, String?>? {
        val fundalHeight = (value as? Number)?.toDouble() ?: return null
        if (fundalHeight == 0.0) return null
        gestationalAgeWeeks ?: return null

        // Expected fundal height = gestational age in weeks ± 2 cm
        val expectedMin = gestationalAgeWeeks - AssessmentDefinedParams.FUNDAL_HEIGHT_TOLERANCE_CM
        val expectedMax = gestationalAgeWeeks + AssessmentDefinedParams.FUNDAL_HEIGHT_TOLERANCE_CM

        return if (fundalHeight !in expectedMin..expectedMax) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * Evaluate : From 2nd ANC visit onwards – Auto-calculate difference in weight from previous ANC visit and highlight abnormal weight gain between ANC visits
     * i.e, less or more than expected weight gain of 1 kg per 15 days
     */
    fun evaluateAncWeightStatus(
        value: Any?,
        ancVisitNumber: Int,
        pregnancyDetail: PregnancyDetail?,
    ): Pair<String?, String?>? =
        if (ancVisitNumber < 2) {
            // If visit is less than 2 then no need to calculate risk
            null
        } else {
            val weight = CommonUtils.getDouble(value).takeIf { it > 0 }
            val daysSinceLastVisit = pregnancyDetail
                ?.ancVisitDate
                ?.let { DateUtils.parseDate(it) }
                ?.getLongTime()
                ?.let { DateUtils.getDaysDifference(it) }
            val previousWeight = pregnancyDetail
                ?.ancWeight
                ?.takeIf { it > 0 }
            if (weight == null || daysSinceLastVisit == null || previousWeight == null) {
                null
            } else {
                val weightDiff = abs(weight - previousWeight)
                val daysForCalculate = daysSinceLastVisit / 15
                if (weightDiff != daysForCalculate.toDouble()) {
                    AssessmentDefinedParams.STATUS_ABNORMAL to AssessmentDefinedParams.BN_STATUS_ABNORMAL
                } else {
                    null
                }
            }
        }
}
