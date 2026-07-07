package org.medtroniclabs.uhis.ui.assessment.utils

import org.medtroniclabs.uhis.data.registration.PatientHistoryModel
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentObservations
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams

/**
 * Formats enrollment observation fields (diagnosis, health history) for display in
 * member profile bio-data and service history. Maps backend codes (e.g. dmType2) to display names.
 */
object EnrollmentObservationFormatter {
    private val diagnosisCodeToDisplay = mapOf(
        "dmtype1" to DefinedParams.DMT_ONE,
        "dmtype2" to DefinedParams.DMT_TWO,
        "diabetesmellitustype1" to DefinedParams.DMT_ONE,
        "diabetesmellitustype2" to DefinedParams.DMT_TWO,
        "gestationaldiabetes" to DefinedParams.GESTATIONAL_DIABETES,
        "gestationaldiabetesgdm" to DefinedParams.GESTATIONAL_DIABETES,
        "prediabetic" to DefinedParams.PRE_DIABETIC,
        "prediabetes" to DefinedParams.PRE_DIABETIC,
        "hypertension" to DefinedParams.HYPERTENSION,
        "prehypertension" to DefinedParams.PRE_HYPERTENSION,
        "eclampsia" to "Eclampsia",
        "preeclampsia" to "Pre-Eclampsia",
        "other" to DefinedParams.OTHER,
        "diabetes" to DefinedParams.DIABETES,
    )

    /**
     * Comma-separated diagnosis string from enrollment history observations.
     */
    fun formatDiagnosisFromObservations(
        observations: MemberAssessmentObservations?,
        diagnosisLookup: List<DiagnosisEntity>? = null,
    ): String? = formatDiagnosisString(observations?.confirmDiagnosis, diagnosisLookup)

    /**
     * List of diagnosis strings from patient details (may contain codes or display names).
     */
    fun formatDiagnosisList(
        diagnoses: List<String>?,
        diagnosisLookup: List<DiagnosisEntity>? = null,
    ): String? {
        if (diagnoses.isNullOrEmpty()) return null
        val mapped = diagnoses
            .mapNotNull { it.trim().takeIf { token -> token.isNotEmpty() } }
            .map { mapDiagnosisToken(it, diagnosisLookup) }
            .distinct()
        return mapped.takeIf { it.isNotEmpty() }?.joinToString(", ")
    }

    fun formatDiagnosisString(
        confirmDiagnosis: String?,
        diagnosisLookup: List<DiagnosisEntity>? = null,
    ): String? {
        if (confirmDiagnosis.isNullOrBlank()) return null
        val tokens = confirmDiagnosis.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        return formatDiagnosisList(tokens, diagnosisLookup)
    }

    fun formatHealthHistoryFromObservations(observations: MemberAssessmentObservations?): String? =
        formatHealthHistory(
            heartAttack = observations?.heartAttack,
            stroke = observations?.stroke,
            kidneyDisease = observations?.kidneyDisease,
            copd = observations?.copd,
        )

    fun formatHealthHistoryFromPatientHistory(patientHistory: PatientHistoryModel?): String? =
        formatHealthHistory(
            heartAttack = patientHistory?.heartAttack,
            stroke = patientHistory?.stroke,
            kidneyDisease = patientHistory?.kidneyDisease,
            copd = patientHistory?.copd,
        )

    fun hasHealthHistoryData(
        heartAttack: String?,
        stroke: String?,
        kidneyDisease: String?,
        copd: String?,
    ): Boolean =
        listOf(heartAttack, stroke, kidneyDisease, copd)
            .any { it.equals(DefinedParams.YES, ignoreCase = true) }

    private fun formatHealthHistory(
        heartAttack: String?,
        stroke: String?,
        kidneyDisease: String?,
        copd: String?,
    ): String? {
        if (!hasHealthHistoryData(heartAttack, stroke, kidneyDisease, copd)) {
            return null
        }
        val conditions = mutableListOf<String>()
        if (heartAttack.equals(DefinedParams.YES, ignoreCase = true)) {
            conditions.add(DefinedParams.HEART_ATTACK)
        }
        if (stroke.equals(DefinedParams.YES, ignoreCase = true)) {
            conditions.add(DefinedParams.STROKE)
        }
        if (kidneyDisease.equals(DefinedParams.YES, ignoreCase = true)) {
            conditions.add(DefinedParams.KIDNEY_DISEASE)
        }
        if (copd.equals(DefinedParams.YES, ignoreCase = true)) {
            conditions.add(DefinedParams.COPD)
        }
        return conditions.joinToString(", ").takeIf { it.isNotEmpty() }
    }

    /**
     * Resolves a diagnosis code or label from patientDetails / observations to a display name.
     */
    fun resolveDisplayName(
        token: String,
        diagnosisLookup: List<DiagnosisEntity>? = null,
    ): String = mapDiagnosisToken(token.trim(), diagnosisLookup)

    /**
     * Matches API / observation diagnosis tokens to local diagnosis chip master data.
     */
    fun matchesDiagnosisChip(
        token: String,
        chipValue: String?,
        chipName: String,
        diagnosisLookup: List<DiagnosisEntity>? = null,
    ): Boolean {
        val selectedNorm = token.trim()
        if (selectedNorm.equals(chipName, ignoreCase = true)) return true
        if (!chipValue.isNullOrBlank() && selectedNorm.equals(chipValue, ignoreCase = true)) return true

        val resolvedSelected = mapDiagnosisToken(selectedNorm, diagnosisLookup)
        if (resolvedSelected.equals(chipName, ignoreCase = true)) return true
        if (!chipValue.isNullOrBlank()) {
            val resolvedChipValue = mapDiagnosisToken(chipValue, diagnosisLookup)
            if (resolvedSelected.equals(resolvedChipValue, ignoreCase = true)) return true
        }

        val selectedWithoutQualifier = selectedNorm
            .replace(Regex("\\s*\\([^)]*\\)\\s*$"), "")
            .trim()
        if (selectedWithoutQualifier != selectedNorm) {
            return matchesDiagnosisChip(selectedWithoutQualifier, chipValue, chipName, diagnosisLookup)
        }
        return false
    }

    private fun mapDiagnosisToken(
        token: String,
        diagnosisLookup: List<DiagnosisEntity>?,
    ): String {
        diagnosisLookup?.forEach { entity ->
            if (token.equals(entity.value, ignoreCase = true) ||
                token.equals(entity.diagnosis, ignoreCase = true)
            ) {
                return entity.diagnosis
            }
        }
        diagnosisCodeToDisplay[token.lowercase()]?.let { return it }
        return token
    }
}
