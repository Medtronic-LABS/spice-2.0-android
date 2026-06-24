package org.medtroniclabs.uhis.common

/**
 * Derives the Controlled / Uncontrolled patient status from the clinical signals collected during
 * a nurse medical review.
 *
 * Business rules:
 *  - Uncontrolled: meeting ANY one of the listed conditions qualifies a patient as Uncontrolled.
 *      BP:  Sys >= 141 OR Dia >= 91 (latest reading)
 *      BG:  RBS >= 11.1 OR FBS >= 7.1 (either of the last two readings)
 *      Any comorbidities or complications present
 *      "Are you taking medication" = No
 *  - Controlled: ALL listed conditions must be met to classify as Controlled.
 *      BP:  Sys <= 140 AND Dia <= 90 (latest reading)
 *      BG:  RBS <= 11 AND FBS <= 7.0 (last two readings)
 *      No comorbidities and no complications
 *      "Are you taking medication" = Yes
 *
 * The medication-change rule is intentionally skipped. BP / BG triggers are evaluated directly from
 * the readings (the diagnosis flags on patient/details are frequently null, so they aren't used as a
 * gate). When the available data isn't enough to positively assert either status (e.g. no readings
 * or an unknown medication answer) the result is null so the caller can fall back to a hyphen.
 */
object PatientStatusEvaluator {
    enum class ControlStatus { CONTROLLED, UNCONTROLLED }

    data class GlucoseReading(val type: String?, val value: Double?)

    private const val RBS = "RBS"
    private const val FBS = "FBS"

    fun evaluate(
        latestSystolic: Int?,
        latestDiastolic: Int?,
        lastTwoGlucose: List<GlucoseReading>,
        hasComorbidities: Boolean,
        hasComplications: Boolean,
        takingMedication: Boolean?,
    ): ControlStatus? {
        val bpUncontrolled = (latestSystolic != null && latestSystolic >= 141) ||
            (latestDiastolic != null && latestDiastolic >= 91)
        val bgUncontrolled = lastTwoGlucose.any { it.isUncontrolled() }
        val conditionsUncontrolled = hasComorbidities || hasComplications
        val medicationUncontrolled = takingMedication == false
        if (bpUncontrolled || bgUncontrolled || conditionsUncontrolled || medicationUncontrolled) {
            return ControlStatus.UNCONTROLLED
        }

        // Controlled requires positive confirmation: at least one in-band clinical reading, every
        // present reading in band, no comorbidities/complications and a "taking medication = Yes"
        // answer. A signal that can't be evaluated prevents asserting Controlled.
        val bpPresent = latestSystolic != null && latestDiastolic != null
        val bpControlled = !bpPresent || (latestSystolic!! <= 140 && latestDiastolic!! <= 90)
        val bgControlled = lastTwoGlucose.isEmpty() || lastTwoGlucose.all { it.isControlled() }
        val hasAnyReading = bpPresent || lastTwoGlucose.isNotEmpty()
        val conditionsControlled = !hasComorbidities && !hasComplications
        val medicationControlled = takingMedication == true
        if (hasAnyReading && bpControlled && bgControlled && conditionsControlled && medicationControlled) {
            return ControlStatus.CONTROLLED
        }
        return null
    }

    private fun GlucoseReading.isUncontrolled(): Boolean {
        val v = value ?: return false
        return when {
            RBS.equals(type, ignoreCase = true) -> v >= 11.1
            FBS.equals(type, ignoreCase = true) -> v >= 7.1
            else -> false
        }
    }

    private fun GlucoseReading.isControlled(): Boolean {
        val v = value ?: return false
        return when {
            RBS.equals(type, ignoreCase = true) -> v <= 11.0
            FBS.equals(type, ignoreCase = true) -> v <= 7.0
            else -> false
        }
    }
}
