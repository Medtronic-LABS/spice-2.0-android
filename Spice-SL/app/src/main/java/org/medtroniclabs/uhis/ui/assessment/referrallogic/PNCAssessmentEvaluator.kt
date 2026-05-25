package org.medtroniclabs.uhis.ui.assessment.referrallogic

import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import kotlin.collections.get

/**
 * Evaluator class for Postnatal Care (PNC) assessments.
 * This class provides methods to identify urgent referrals, non-urgent referrals,
 * and care gaps based on assessment results.
 */
object PNCAssessmentEvaluator {
    /**
     * Identifies urgent referral conditions from the maternal health assessment data.
     * The logic follows a strict sequence of checks including danger signs, BP, fever,
     * pulse, and hemoglobin levels.
     *
     * @param resultMap The assessment result map containing maternal health data.
     * @return A list of strings representing the urgent referral reasons found.
     */
    fun getUrgentReferral(resultMap: Map<String, Any>): List<String> {
        val urgentReferral = arrayListOf<String>()

        (resultMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            (maternalAssessment[RMNCH.ID_POSTPARTUM_DANGER_SIGNS] as? List<*>)?.let { dangerSigns ->
                val selectedSigns = dangerSigns
                    .filterIsInstance<Map<String, Any>>()
                    .mapNotNull {
                        it[DefinedParams.Value] as? String
                    }.toSet()

                // Danger Signs (1-5)
                // 1. Heavy bleeding (>5 pads per day)
                if (selectedSigns.contains(PostpartumDangerSigns.HEAVY_BLEEDING.value)) {
                    urgentReferral.add(PNCUrgentReferrals.HEAVY_BLEEDING.value + "::" + PNCUrgentReferrals.HEAVY_BLEEDING.cultureValue)
                }

                // 2. Severe abdominal pain
                if (selectedSigns.contains(PostpartumDangerSigns.SEVERE_ABDOMINAL_PAIN.value)) {
                    urgentReferral.add(PNCUrgentReferrals.SEVERE_ABDOMINAL_PAIN.value + "::" + PNCUrgentReferrals.SEVERE_ABDOMINAL_PAIN.cultureValue)
                }

                // 3. Severe headache/visual issues/convulsions
                if (selectedSigns.contains(PostpartumDangerSigns.SEVERE_HEADACHE_VISION_CONVULSIONS.value)) {
                    urgentReferral.add(
                        PNCUrgentReferrals.SEVERE_HEADACHE_VISUAL_ISSUES_CONVULSIONS.value + "::" +
                            PNCUrgentReferrals.SEVERE_HEADACHE_VISUAL_ISSUES_CONVULSIONS.cultureValue,
                    )
                }

                // 4. Foul-smelling discharge
                if (selectedSigns.contains(PostpartumDangerSigns.FOUL_SMELLING_DISCHARGE.value)) {
                    urgentReferral.add(PNCUrgentReferrals.FOUL_SMELLING_DISCHARGE.value + "::" + PNCUrgentReferrals.FOUL_SMELLING_DISCHARGE.cultureValue)
                }

                // 5. Perineum tear / Discharge from wound area
                if (selectedSigns.contains(PostpartumDangerSigns.PERINEAL_WOUND_DISCHARGE.value)) {
                    urgentReferral.add(PNCUrgentReferrals.PERINEUM_TEAR_DISCHARGE.value + "::" + PNCUrgentReferrals.PERINEUM_TEAR_DISCHARGE.cultureValue)
                }
            }

            val systolic = CommonUtils.getInteger(maternalAssessment[AssessmentDefinedParams.SYSTOLIC])
            val diastolic = CommonUtils.getInteger(maternalAssessment[AssessmentDefinedParams.DIASTOLIC])
            val isBpHigh = isHighBp(systolic, diastolic)
            val isKnownHtn = isValueEquals(maternalAssessment[RMNCH.ID_KNOWN_HTN], DefinedParams.YES)
            val isEclampsia = isValueEquals(maternalAssessment[RMNCH.ID_ECLAMPSIA], DefinedParams.YES)
            val isOnTreatmentHtn = isValueEquals(maternalAssessment[RMNCH.ID_ON_TREATMENT_HTN_ECLAMPSIA], DefinedParams.YES)
            val isEdemaPresent = isValueEquals(maternalAssessment[RMNCH.ID_EDEMA])
            val isUrineAlbuminPresent = isValueEquals(maternalAssessment[RMNCH.ID_URINARY_ALBUMIN])

            // 6. High BP ≥140/90
            if (isBpHigh) {
                urgentReferral.add(PNCUrgentReferrals.HIGH_BP.value + "::" + PNCUrgentReferrals.HIGH_BP.cultureValue)
            }

            // 7. Not on treatment for HTN or Pre-eclampsia /Eclampsia
            if ((isKnownHtn || isEclampsia) && !isOnTreatmentHtn) {
                urgentReferral.add(
                    PNCUrgentReferrals.HTN_ECLAMPSIA_NOT_ON_TREATMENT.value + "::" + PNCUrgentReferrals.HTN_ECLAMPSIA_NOT_ON_TREATMENT.cultureValue,
                )
            }

            // 8. Edema + (BP≥140/90 or Urine Albumin present)
            if (isEdemaPresent && (isBpHigh || isUrineAlbuminPresent)) {
                urgentReferral.add(PNCUrgentReferrals.EDEMA_PLUS_RISK.value + "::" + PNCUrgentReferrals.EDEMA_PLUS_RISK.cultureValue)
            }

            // 9. Urine Albumin Positive + (BP≥140/90 or Edema present)
            if (isUrineAlbuminPresent && (isBpHigh || isEdemaPresent)) {
                urgentReferral.add(PNCUrgentReferrals.URINE_ALBUMIN_PLUS_RISK.value + "::" + PNCUrgentReferrals.URINE_ALBUMIN_PLUS_RISK.cultureValue)
            }

            // 10. High Fever - >=102°F
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_TEMPERATURE])?.takeIf { it > 0 }?.let { temp ->
                if (temp >= AssessmentDefinedParams.TEMP_HIGH_FEVER_THRESHOLD) {
                    urgentReferral.add(PNCUrgentReferrals.HIGH_FEVER.value + "::" + PNCUrgentReferrals.HIGH_FEVER.cultureValue)
                }
            }

            // 11. Abnormal Pulse - If >90 or <60
            val pulse = CommonUtils.getInteger(maternalAssessment[RMNCH.ID_PULSE])
            if (pulse > 0 && pulse !in AssessmentDefinedParams.PULSE_LOW_THRESHOLD.toInt()..AssessmentDefinedParams.PULSE_HIGH_THRESHOLD.toInt()) {
                urgentReferral.add(PNCUrgentReferrals.ABNORMAL_PULSE.value + "::" + PNCUrgentReferrals.ABNORMAL_PULSE.cultureValue)
            }

            // 12. Severe Anemia - Hb <8 g/dl
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_HEMOGLOBIN])?.takeIf { it > 0 }?.let { hb ->
                if (hb < AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD) {
                    urgentReferral.add(PNCUrgentReferrals.SEVERE_ANEMIA.value + "::" + PNCUrgentReferrals.SEVERE_ANEMIA.cultureValue)
                }
            }

            // 13. High Blood Sugar - Fasting sugar ≥7mmol/L or Random sugar ≥11.1 mmol/L
            if (isHighBloodSugar(resultMap)) {
                urgentReferral.add(PNCUrgentReferrals.HIGH_BLOOD_SUGAR.value + "::" + PNCUrgentReferrals.HIGH_BLOOD_SUGAR.cultureValue)
            }

            // 14. Known DM/GDM patient not on treatment
            val dmPatient = isValueEquals(maternalAssessment[RMNCH.ID_DM_PATIENT], DefinedParams.YES)
            val gdmPatient = isValueEquals(maternalAssessment[RMNCH.ID_GDM_PATIENT], DefinedParams.YES)
            val isOnTreatmentDm = isValueEquals(maternalAssessment[RMNCH.ID_ON_TREATMENT_DM_GDM], DefinedParams.YES)
            if ((dmPatient || gdmPatient) && !isOnTreatmentDm) {
                urgentReferral.add(PNCUrgentReferrals.DM_GDM_NOT_ON_TREATMENT.value + "::" + PNCUrgentReferrals.DM_GDM_NOT_ON_TREATMENT.cultureValue)
            }

            // 15. Suspected Jaundice - Urinary Bilirubin-Positive
            if (isValueEquals(maternalAssessment[RMNCH.ID_URINARY_BILIRUBIN])) {
                urgentReferral.add(PNCUrgentReferrals.SUSPECTED_JAUNDICE.value + "::" + PNCUrgentReferrals.SUSPECTED_JAUNDICE.cultureValue)
            }
        }

        return urgentReferral
    }

    /**
     * Identifies non-urgent referral conditions from the maternal health assessment data.
     * Includes checks for moderate anemia, breast issues, mild fever, and monitoring
     * of patients already on treatment for chronic conditions.
     *
     * @param resultMap The assessment result map containing maternal health data.
     * @return A list of strings representing the non-urgent referral reasons found.
     */
    fun getNonUrgentReferral(resultMap: Map<String, Any>): List<String> {
        val nonUrgentReferral = arrayListOf<String>()

        (resultMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            // 1. Moderate Anemia - Hb 8–10
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_HEMOGLOBIN])?.takeIf { it > 0 }?.let { hb ->
                if (hb in AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD..AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD) {
                    nonUrgentReferral.add(PNCNonUrgentReferrals.MODERATE_ANEMIA.value + "::" + PNCNonUrgentReferrals.MODERATE_ANEMIA.cultureValue)
                }
            }

            // 2. Mild Anemia - Hb < 11
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_HEMOGLOBIN])?.takeIf { it > 0 }?.let { hb ->
                if (hb > AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD && hb < AssessmentDefinedParams.HEMOGLOBIN_MILD_ANEMIA_THRESHOLD) {
                    nonUrgentReferral.add(PNCNonUrgentReferrals.MILD_ANEMIA.value + "::" + PNCNonUrgentReferrals.MILD_ANEMIA.cultureValue)
                }
            }

            // 3. Cracked nipples / painful / swollen breasts with or without fever
            (maternalAssessment[RMNCH.ID_POSTPARTUM_DANGER_SIGNS] as? List<*>)?.let { dangerSigns ->
                val selectedSigns = dangerSigns
                    .filterIsInstance<Map<String, Any>>()
                    .mapNotNull {
                        it[DefinedParams.Value] as? String
                    }.toSet()

                if (selectedSigns.contains(PostpartumDangerSigns.BREAST_PAIN_SWELLING_FEVER.value)) {
                    nonUrgentReferral.add(PNCNonUrgentReferrals.BREAST_ISSUES.value + "::" + PNCNonUrgentReferrals.BREAST_ISSUES.cultureValue)
                }
            }

            // 4. Fever - >=100°F
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_TEMPERATURE])?.takeIf { it > 0 }?.let { temp ->
                if (temp in AssessmentDefinedParams.TEMP_FEVER_MIN_THRESHOLD..<AssessmentDefinedParams.TEMP_FEVER_MAX_THRESHOLD) {
                    nonUrgentReferral.add(PNCNonUrgentReferrals.FEVER.value + "::" + PNCNonUrgentReferrals.FEVER.cultureValue)
                }
            }

            val isKnownHtn = isValueEquals(maternalAssessment[RMNCH.ID_KNOWN_HTN], DefinedParams.YES)
            val isEclampsia = isValueEquals(maternalAssessment[RMNCH.ID_ECLAMPSIA], DefinedParams.YES)
            val isOnTreatmentHtn = isValueEquals(maternalAssessment[RMNCH.ID_ON_TREATMENT_HTN_ECLAMPSIA], DefinedParams.YES)

            // 5. On treatment for HTN or Pre-eclampsia / Eclampsia
            if ((isKnownHtn || isEclampsia) && isOnTreatmentHtn) {
                nonUrgentReferral.add(
                    PNCNonUrgentReferrals.HTN_ECLAMPSIA_ON_TREATMENT.value + "::" + PNCNonUrgentReferrals.HTN_ECLAMPSIA_ON_TREATMENT.cultureValue,
                )
            }

            val dmPatient = isValueEquals(maternalAssessment[RMNCH.ID_DM_PATIENT], DefinedParams.yes)
            val gdmPatient = isValueEquals(maternalAssessment[RMNCH.ID_GDM_PATIENT], DefinedParams.yes)
            val onTreatmentDmGdm = isValueEquals(maternalAssessment[RMNCH.ID_ON_TREATMENT_DM_GDM], DefinedParams.YES)

            // 6. On treatment for DM/GDM
            if ((dmPatient || gdmPatient) && onTreatmentDmGdm) {
                nonUrgentReferral.add(PNCNonUrgentReferrals.DM_GDM_ON_TREATMENT.value + "::" + PNCNonUrgentReferrals.DM_GDM_ON_TREATMENT.cultureValue)
            }

            // 7. Other
            (maternalAssessment[RMNCH.ID_POSTPARTUM_DANGER_SIGNS] as? List<*>)?.let { dangerSigns ->
                val selectedSigns = dangerSigns
                    .filterIsInstance<Map<String, Any>>()
                    .mapNotNull {
                        it[DefinedParams.Value] as? String
                    }.toSet()

                if (selectedSigns.contains(PostpartumDangerSigns.OTHER.value)) {
                    nonUrgentReferral.add(PNCNonUrgentReferrals.OTHER.value + "::" + PNCNonUrgentReferrals.OTHER.cultureValue)
                }
            }
        }

        return nonUrgentReferral
    }

    /**
     * Identifies care gaps in supplementation and contraception for the PNC period.
     * Supplementation gaps (Vitamin A, IFA, Calcium) are grouped into a single reason.
     *
     * @param resultMap The assessment result map containing maternal health and contraception data.
     * @return A list of strings representing the care gaps found.
     */
    fun getPncGaps(resultMap: Map<String, Any>): List<String> {
        val pncGaps = arrayListOf<String>()

        (resultMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            val supplementationGaps = arrayListOf<String>()
            val cultureSupplementationGaps = arrayListOf<String>()
            var hasSupplementationGaps = false

            // 1. Supplementation (Vitamin A, IFA, Calcium)
            // Vitamin A
            if (!isValueEquals(maternalAssessment[RMNCH.ID_VITAMIN_A_CONSUMED], DefinedParams.YES)) {
                hasSupplementationGaps = true
                supplementationGaps.add(PNCSupplementation.VITAMIN_A.value)
                cultureSupplementationGaps.add(PNCSupplementation.VITAMIN_A.cultureValue)
            }

            // IFA and Calcium
            val daysSinceDelivery = CommonUtils.getInteger(resultMap[RMNCH.ID_DAYS_SINCE_DELIVERY])
            val expectedTablets = daysSinceDelivery + 1

            // IFA (Non-optional integer)
            val ifaConsumed = CommonUtils.getInteger(maternalAssessment[RMNCH.ID_IFA_TABLETS_CONSUMED])
            if (ifaConsumed < expectedTablets) {
                hasSupplementationGaps = true
                supplementationGaps.add(PNCSupplementation.IFA.value)
                cultureSupplementationGaps.add(PNCSupplementation.IFA.cultureValue)
            }

            // Calcium (Non-optional integer)
            val calciumConsumed = CommonUtils.getInteger(maternalAssessment[RMNCH.ID_CALCIUM_TABLETS_CONSUMED])
            if (calciumConsumed < expectedTablets) {
                hasSupplementationGaps = true
                supplementationGaps.add(PNCSupplementation.CALCIUM.value)
                cultureSupplementationGaps.add(PNCSupplementation.CALCIUM.cultureValue)
            }

            if (hasSupplementationGaps) {
                pncGaps.add(
                    "${PNCGaps.SUPPLEMENTATION.value} (${supplementationGaps.joinToString()})" +
                        "::" +
                        "${PNCGaps.SUPPLEMENTATION.cultureValue} (${cultureSupplementationGaps.joinToString()})",
                )
            }
        }

        // 2. Not using postpartum contraception
        (resultMap[RMNCH.ID_POSTPARTUM_CONTRACEPTION] as? Map<*, *>)?.let { contraception ->
            if (isValueEquals(contraception[RMNCH.ID_FAMILY_PLANNING_METHODS], DefinedParams.None)) {
                pncGaps.add(PNCGaps.CONTRACEPTION_GAP.value + "::" + PNCGaps.CONTRACEPTION_GAP.cultureValue)
            }
        }

        return pncGaps
    }

    /**
     * Returns anemia level based on HB
     */
    fun getAnemiaLevel(resultMap: Map<String, Any>): AnemiaLevel {
        var level = AnemiaLevel.None
        (resultMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_HEMOGLOBIN])?.takeIf { it > 0 }?.let { hb ->
                when {
                    hb < AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD -> {
                        level = AnemiaLevel.Severe
                    }
                    hb < AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD -> {
                        level = AnemiaLevel.Moderate
                    }
                    hb < AssessmentDefinedParams.HEMOGLOBIN_MILD_ANEMIA_THRESHOLD -> {
                        level = AnemiaLevel.Mild
                    }
                }
            }
        }
        return level
    }

    /**
     * Returns true if the user is having high blood sugar
     */
    fun isHighBloodSugar(resultMap: Map<String, Any>): Boolean {
        (resultMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            val fastingSugar = CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_FASTING_BLOOD_SUGAR])?.takeIf { it > 0 }
            val randomSugar = CommonUtils.getDoubleOrNull(maternalAssessment[RMNCH.ID_RANDOM_BLOOD_SUGAR])?.takeIf { it > 0 }
            return (fastingSugar != null && fastingSugar >= 7.0) || (randomSugar != null && randomSugar >= 11.1)
        }
        return false
    }

    /**
     * Checks if blood pressure exceeds defined limits for given systolic and diastolic values.
     *
     * @param systolic The systolic pressure value.
     * @param diastolic The diastolic pressure value.
     * @return True if either value exceeds its respective limit, false otherwise.
     */
    fun isHighBp(
        systolic: Int,
        diastolic: Int,
    ): Boolean {
        if (systolic <= 0 && diastolic <= 0) return false
        return systolic >= AssessmentDefinedParams.BP_SYSTOLIC_THRESHOLD.toInt() ||
            diastolic >= AssessmentDefinedParams.BP_DIASTOLIC_THRESHOLD.toInt()
    }

    /**
     * Checks if a specific selection or value is present for a key in the assessment data.
     *
     * @param value The value to check.
     * @param target The expected target value (defaults to "present").
     * @return True if the value matches the target (case-insensitive), false otherwise.
     */
    fun isValueEquals(
        value: Any?,
        target: String = "present",
    ): Boolean = (value as? String)?.equals(target, ignoreCase = true) == true
}
