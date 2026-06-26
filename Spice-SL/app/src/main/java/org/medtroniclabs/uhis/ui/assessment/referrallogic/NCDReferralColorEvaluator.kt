package org.medtroniclabs.uhis.ui.assessment.referrallogic

import org.medtroniclabs.uhis.ncd.screening.utils.ReferredReason

/**
 * Red risk algorithm applied for NCD 2nd visit onwards.
 *
 * Derives NCD referral risk level and summary UI color from blood pressure,
 * blood glucose, and symptom data.
 *
 * Hypertension and diabetes are scored independently; the higher-severity result
 * is returned when both are present.
 *
 * Reference : [Doc](https://docs.google.com/document/d/1el3e-Bz6thuWk0cDvaqAb4ZuDFi0RbO8/edit)
 */
object NCDReferralColorEvaluator {
    /** Vital signs and symptom flags used for risk scoring. */
    data class Input(
        val systolic: Int? = null,
        val diastolic: Int? = null,
        val glucoseUnit: String? = null, // "mmol/L" or "mg/dL"
        val glucoseType: String? = null, // "fbs" or "rbs"
        val glucoseValue: Double? = null,
        val hasSymptoms: Boolean = false,
    )

    /** Risk outcome including display color for the assessment summary banner. */
    data class Result(
        val riskLevel: RiskLevel,
        val yellowPriority: YellowPriority? = null,
        val colorHex: String,
    )

    enum class RiskLevel { RED, ORANGE, YELLOW, GREEN }

    /** When both conditions are yellow, higher-priority band is referred first. */
    enum class YellowPriority { HIGHER, LOWER }

    private enum class Severity(val rank: Int, val riskLevel: RiskLevel) {
        GREEN(0, RiskLevel.GREEN),
        YELLOW_LOWER(1, RiskLevel.YELLOW),
        YELLOW_HIGHER(2, RiskLevel.YELLOW),
        ORANGE(3, RiskLevel.ORANGE),
        RED(4, RiskLevel.RED),
    }

    private data class ScoredRisk(
        val severity: Severity,
        val yellowPriority: YellowPriority? = null,
    )

    private object Thresholds {
        const val MGDL_TO_MMOL = 18.0182
        const val HYPOGLYCEMIA_MMOL = 3.9
        const val DIABETES_RED_MMOL = 27.8
        const val DIABETES_ORANGE_LOW_MMOL = 16.7
        const val DIABETES_YELLOW_HIGH_MMOL = 13.9
        const val FBS_YELLOW_LOW_MMOL = 7.0
        const val RBS_GREEN_HIGH_MMOL = 9.9
        const val LOW_GLUCOSE_BAND_HIGH_MMOL = 4.5

        const val BP_CRISIS_SYSTOLIC = 180
        const val BP_CRISIS_DIASTOLIC = 110
        const val BP_HYPOTENSION_SYSTOLIC = 90
        const val BP_HYPOTENSION_DIASTOLIC = 60
        const val BP_YELLOW_LOWER_SYSTOLIC_MAX = 159
        const val BP_YELLOW_LOWER_DIASTOLIC_MAX = 99
        const val BP_NORMAL_SYSTOLIC_MAX = 140
        const val BP_NORMAL_DIASTOLIC_MAX = 90
    }

    private object Colors {
        const val RED = "#8B0000"
        const val ORANGE = "#E4A476"
        const val YELLOW = "#E4CC76"
        const val GREEN = "#B1CD77"
    }

    /**
     * Scores hypertension and diabetes (when data is present) and returns the
     * highest-severity result, defaulting to [RiskLevel.GREEN].
     */
    fun evaluate(input: Input): Result =
        listOfNotNull(evaluateHypertension(input), evaluateDiabetes(input))
            .maxByOrNull { it.severity.rank }
            ?.toResult()
            ?: ScoredRisk(Severity.GREEN).toResult()

    fun isReferralRequired(input: Input): Boolean = evaluate(input).riskLevel != RiskLevel.GREEN

    fun referralReasons(input: Input): ArrayList<String> {
        val reasons = ArrayList<String>()
        if (input.hasSymptoms) {
            reasons.add(ReferredReason.SYMPTOMS)
        }
        evaluateHypertension(input)?.takeIf { it.severity != Severity.GREEN }?.let {
            reasons.add(ReferredReason.bloodPressure)
        }
        evaluateDiabetes(input)?.takeIf { it.severity != Severity.GREEN }?.let {
            reasons.add(ReferredReason.bloodGlucose)
        }
        return reasons
    }

    private fun evaluateHypertension(input: Input): ScoredRisk? {
        val sys = input.systolic ?: return null
        val dia = input.diastolic ?: return null
        if (sys <= 0 || dia <= 0) return null

        val symptoms = input.hasSymptoms
        val crisis = isBpCrisis(sys, dia)

        return when {
            crisis && symptoms -> ScoredRisk(Severity.RED)
            crisis -> ScoredRisk(Severity.ORANGE)
            (
                (sys > Thresholds.BP_YELLOW_LOWER_SYSTOLIC_MAX && sys < Thresholds.BP_CRISIS_SYSTOLIC) ||
                    (dia > Thresholds.BP_YELLOW_LOWER_DIASTOLIC_MAX && dia < Thresholds.BP_CRISIS_DIASTOLIC)
            ) ||
                symptoms -> ScoredRisk(
                Severity.YELLOW_HIGHER,
                YellowPriority.HIGHER,
            )

            (
                sys in Thresholds.BP_NORMAL_SYSTOLIC_MAX..Thresholds.BP_YELLOW_LOWER_SYSTOLIC_MAX ||
                    dia in Thresholds.BP_NORMAL_DIASTOLIC_MAX..Thresholds.BP_YELLOW_LOWER_DIASTOLIC_MAX
            ) -> ScoredRisk(Severity.YELLOW_LOWER, YellowPriority.LOWER)

            sys < Thresholds.BP_NORMAL_SYSTOLIC_MAX &&
                dia < Thresholds.BP_NORMAL_DIASTOLIC_MAX -> ScoredRisk(Severity.GREEN)

            else -> ScoredRisk(Severity.GREEN)
        }
    }

    private fun evaluateDiabetes(input: Input): ScoredRisk? {
        val bg = input.glucoseValue?.toMmol(input.glucoseUnit) ?: return null
        if (bg <= 0) return null

        if (input.hasSymptoms) return ScoredRisk(Severity.RED)

        val type = input.glucoseType?.lowercase()
        return when {
            bg > Thresholds.DIABETES_RED_MMOL -> ScoredRisk(Severity.RED)
            bg < Thresholds.HYPOGLYCEMIA_MMOL -> ScoredRisk(Severity.ORANGE)
            bg in Thresholds.DIABETES_ORANGE_LOW_MMOL..Thresholds.DIABETES_RED_MMOL ->
                ScoredRisk(Severity.ORANGE)

            bg in Thresholds.DIABETES_YELLOW_HIGH_MMOL..Thresholds.DIABETES_ORANGE_LOW_MMOL ->
                ScoredRisk(Severity.YELLOW_HIGHER, YellowPriority.HIGHER)

            isYellowLowerDiabetes(bg, type) ->
                ScoredRisk(Severity.YELLOW_LOWER, YellowPriority.LOWER)

            isGreenDiabetes(bg, type) -> ScoredRisk(Severity.GREEN)
            else -> ScoredRisk(Severity.GREEN)
        }
    }

    private fun isBpCrisis(
        sys: Int,
        dia: Int,
    ) = sys >= Thresholds.BP_CRISIS_SYSTOLIC ||
        dia >= Thresholds.BP_CRISIS_DIASTOLIC ||
        sys < Thresholds.BP_HYPOTENSION_SYSTOLIC ||
        dia < Thresholds.BP_HYPOTENSION_DIASTOLIC

    private fun isYellowLowerDiabetes(
        bg: Double,
        type: String?,
    ): Boolean {
        val lowBand = bg >= Thresholds.HYPOGLYCEMIA_MMOL && bg < Thresholds.LOW_GLUCOSE_BAND_HIGH_MMOL
        return when (type) {
            "fbs" -> (bg >= Thresholds.FBS_YELLOW_LOW_MMOL && bg < Thresholds.DIABETES_YELLOW_HIGH_MMOL) || lowBand
            "rbs" -> (bg > Thresholds.RBS_GREEN_HIGH_MMOL && bg < Thresholds.DIABETES_YELLOW_HIGH_MMOL) || lowBand
            else ->
                (bg >= Thresholds.FBS_YELLOW_LOW_MMOL && bg < Thresholds.DIABETES_YELLOW_HIGH_MMOL) ||
                    (bg > Thresholds.RBS_GREEN_HIGH_MMOL && bg < Thresholds.DIABETES_YELLOW_HIGH_MMOL) ||
                    lowBand
        }
    }

    private fun isGreenDiabetes(
        bg: Double,
        type: String?,
    ): Boolean =
        when (type) {
            "fbs" -> bg >= Thresholds.LOW_GLUCOSE_BAND_HIGH_MMOL && bg < Thresholds.FBS_YELLOW_LOW_MMOL
            "rbs" -> bg in Thresholds.LOW_GLUCOSE_BAND_HIGH_MMOL..Thresholds.RBS_GREEN_HIGH_MMOL
            else ->
                bg >= Thresholds.LOW_GLUCOSE_BAND_HIGH_MMOL &&
                    bg < Thresholds.FBS_YELLOW_LOW_MMOL ||
                    bg in Thresholds.LOW_GLUCOSE_BAND_HIGH_MMOL..Thresholds.RBS_GREEN_HIGH_MMOL
        }

    private fun ScoredRisk.toResult(): Result {
        val level = severity.riskLevel
        return Result(
            riskLevel = level,
            yellowPriority = yellowPriority,
            colorHex = when (level) {
                RiskLevel.RED -> Colors.RED
                RiskLevel.ORANGE -> Colors.ORANGE
                RiskLevel.YELLOW -> Colors.YELLOW
                RiskLevel.GREEN -> Colors.GREEN
            },
        )
    }

    private fun Double.toMmol(unit: String?): Double = if (unit.equals("mg/dL", ignoreCase = true)) this / Thresholds.MGDL_TO_MMOL else this
}
