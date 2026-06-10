package org.medtroniclabs.uhis.common

import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.db.entity.RiskFactorModel
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_SYSTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIO_METRICS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIOMETRIC_FAMILY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BMI
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IS_REGULAR_SMOKER
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.WEIGHT

object CVDRiskCalculator {
    fun calculateCVDRiskFactor(
        map: HashMap<String, Any>,
        list: ArrayList<RiskClassificationModel>,
        dob: String,
        gender: String,
    ) {
        if (list.isEmpty()) return

        val bpLogs = map[BP_LOG] as? HashMap<*, *>
        val bmiValue = resolveBmi(map, bpLogs) ?: return
        val systolicAverage = bpLogs?.get(AVG_SYSTOLIC)?.let { (it as Number).toInt() }
        val isSmoker = bpLogs?.get(IS_REGULAR_SMOKER)?.let { getSmokerType(it) } ?: false

        val age = DateUtils.getV2YearMonthAndWeek(dob).years.toDouble()

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

    private fun resolveBmi(
        map: HashMap<String, Any>,
        bpLogs: HashMap<*, *>?,
    ): Double? {
        toBmiDouble(bpLogs?.get(BMI))?.let { return it }
        for (familyKey in listOf(BIOMETRIC_FAMILY, BIO_METRICS)) {
            val family = map[familyKey] as? HashMap<*, *> ?: continue
            toBmiDouble(family[BMI])?.let { return it }
            val height = (family[HEIGHT] as? Number)?.toDouble()
            val weight = (family[WEIGHT] as? Number)?.toDouble()
            if (height != null && weight != null && height > 0) {
                CommonUtils.getBMIForNcd(height, weight)?.toDoubleOrNull()?.let { return it }
            }
        }
        return toBmiDouble(map[BMI])
    }

    private fun toBmiDouble(value: Any?): Double? =
        when (value) {
            is Double -> value
            is Number -> value.toDouble()
            else -> null
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
        isSmoker: Boolean,
    ): Map<String, Any>? {
        val model = findMatchingModel(list, age, gender, isSmoker) ?: return null
        return getRiskBasedOnParams(model.riskFactors, bmiValue, systolicAverage)
    }

    private fun findMatchingModel(
        list: ArrayList<RiskClassificationModel>,
        age: Double?,
        gender: String?,
        isSmoker: Boolean,
    ): RiskClassificationModel? {
        val normalizedGender = normalizeGender(gender)
        val candidates =
            list.filter {
                it.isSmoker == isSmoker &&
                    it.gender.equals(normalizedGender, ignoreCase = true)
            }
        if (candidates.isEmpty() || age == null) return null

        candidates.firstOrNull { isAgeInLimit(age, it.age) }?.let { return it }

        return candidates.minByOrNull { ageBandDistance(age, it.age) }
    }

    private fun normalizeGender(gender: String?): String =
        if (gender.equals(DefinedParams.FEMALE, ignoreCase = true)) {
            DefinedParams.FEMALE
        } else {
            DefinedParams.MALE
        }

    private fun ageBandDistance(
        age: Double,
        band: String,
    ): Double {
        val parts = band.split("-")
        if (parts.size != 2) return Double.MAX_VALUE
        val min = parts[0].toDoubleOrNull() ?: return Double.MAX_VALUE
        val max = parts[1].toDoubleOrNull() ?: return Double.MAX_VALUE
        return when {
            age < min -> min - age
            age > max -> age - max
            else -> 0.0
        }
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
            sbp.startsWith(">=") -> value >= sbp.substringAfter(">=").trim().toInt()
            sbp.startsWith("<=") -> value <= sbp.substringAfter("<=").trim().toInt()
            sbp.startsWith("<") -> value < sbp.substringAfter("<").trim().toInt()
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
