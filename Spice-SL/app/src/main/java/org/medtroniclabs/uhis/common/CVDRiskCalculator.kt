package org.medtroniclabs.uhis.common

import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.db.entity.RiskFactorModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_SYSTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BMI
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IS_REGULAR_SMOKER

object CVDRiskCalculator {
    fun calculateCVDRiskFactor(
        map: HashMap<String, Any>,
        list: ArrayList<RiskClassificationModel>,
        dob: String,
        gender: String,
    ) {
        val bpLogs = map[BP_LOG] as HashMap<*, *>
        val bmiValue = bpLogs[BMI]?.let { it as Double } // Need to Check
        val systolicAverage = bpLogs[AVG_SYSTOLIC]?.let { it as Int }
        val isSmoker = bpLogs[IS_REGULAR_SMOKER]?.let { getSmokerType(it) }

        val age = DateUtils.getV2YearMonthAndWeek(dob).years.toDouble()

        if (bmiValue == null || list.isEmpty()) return

        calculateRiskFactor(
            list,
            age,
            gender,
            bmiValue,
            systolicAverage,
            isSmoker,
        )?.let { result ->
            map[DefinedParams.CVD_RISK_SCORE] = result[DefinedParams.CVD_RISK_SCORE] as Int
            map[DefinedParams.CVD_RISK_LEVEL] = result[DefinedParams.CVD_RISK_LEVEL] as String
            map[DefinedParams.CVD_RISK_SCORE_DISPLAY] =
                result[DefinedParams.CVD_RISK_SCORE_DISPLAY] as String
        }
    }

    private fun getSmokerType(value: Any): Boolean =
        when (value) {
            is String -> value == DefinedParams.YES
            is Boolean -> value
            else -> false
        }

    private fun calculateRiskFactor(
        list: ArrayList<RiskClassificationModel>,
        age: Double?,
        gender: String?,
        bmiValue: Double?,
        systolicAverage: Int?,
        isSmoker: Boolean?,
    ): Map<String, Any>? {
        val model = list.firstOrNull {
            it.isSmoker == isSmoker &&
                it.gender.equals(gender, true) &&
                isAgeInLimit(age, it.age)
        } ?: return null

        return getRiskBasedOnParams(model.riskFactors, bmiValue, systolicAverage)
    }

    private fun getRiskBasedOnParams(
        riskFactors: ArrayList<RiskFactorModel>,
        bmiValue: Double?,
        systolicAverage: Int?,
    ): Map<String, Any>? {
        if (bmiValue == null || systolicAverage == null) return null

        val factor = riskFactors.firstOrNull {
            checkBMIValue(it.bmi, bmiValue) &&
                checkSystolicBPValue(it.sbp, systolicAverage)
        } ?: return null

        return hashMapOf(
            DefinedParams.CVD_RISK_SCORE to factor.riskScore,
            DefinedParams.CVD_RISK_LEVEL to factor.riskLevel,
            DefinedParams.CVD_RISK_SCORE_DISPLAY to
                "${factor.riskScore}% - ${factor.riskLevel}",
        )
    }

    private fun checkBMIValue(
        bmi: String,
        value: Double,
    ): Boolean =
        when {
            bmi.startsWith(">=") -> value >= bmi.substringAfter(">=").trim().toDouble()
            bmi.startsWith("<=") -> value <= bmi.substringAfter("<=").trim().toDouble()
            bmi.startsWith(">") -> value > bmi.substringAfter(">").trim().toDouble()
            bmi.startsWith("<") -> value < bmi.substringAfter("<").trim().toDouble()
            bmi.contains("-") -> {
                val (min, max) = bmi.split("-").map { it.trim().toDouble() }
                value in min..max
            }
            else -> false
        }

    private fun checkSystolicBPValue(
        sbp: String,
        value: Int,
    ): Boolean =
        when {
            sbp.startsWith("<") -> value < sbp.substringAfter("<").trim().toInt()
            sbp.startsWith(">=") -> value >= sbp.substringAfter(">=").trim().toInt()
            sbp.contains("-") -> {
                val (min, max) = sbp.split("-").map { it.trim().toInt() }
                value in min..max
            }
            else -> false
        }

    private fun isAgeInLimit(
        age: Double?,
        limit: String,
    ): Boolean {
        var status = false
        val limitArray = limit.split("-")
        if (age != null && limitArray.size == 2) {
            val minValue = limitArray[0].toIntOrNull()
            val maxValue = limitArray[1].toIntOrNull()
            if (minValue != null && maxValue != null) {
                status = age >= minValue && age <= maxValue
            }
        }
        return status
    }
}
