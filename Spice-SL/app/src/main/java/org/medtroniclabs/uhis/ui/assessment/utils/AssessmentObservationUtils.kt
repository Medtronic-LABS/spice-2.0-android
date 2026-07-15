package org.medtroniclabs.uhis.ui.assessment.utils

import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.db.entity.MemberAssessmentObservations
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_BLOOD_PRESSURE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_DIASTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_SYSTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIOMETRIC_FAMILY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIO_METRICS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CATARACT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DESIRE_FOR_CHILDREN_IN_FUTURE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FUNDAL_HEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanning
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanningDetails
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FamilyPlanningMethods
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GESTATION_MONTH_AT_ABORTION
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GROUP_ANC_SERVICES_BIRTH_PREPAREDNESS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GROUP_POINT_OF_CARE_INVESTIGATIONS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HEMOGLOBIN
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_ABORTION
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_DELIVERY_OUTCOMES
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_LIVE_BIRTH_NUMBERS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_MODE_OF_DELIVERY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_PREGNANCY_OUTCOME_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_STILL_BIRTH_NUMBERS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.IS_BABY_ALIVE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MATERNAL_DEATH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NUMBER_OF_LIVING_CHILDREN
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TIME_OF_DEATH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.TYPE_OF_ABORTION
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.WEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.YES
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ncd
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil.calculateAverageBloodPressure
import kotlin.math.roundToInt

/**
 * Extracts structured [MemberAssessmentObservations] from a completed assessment form map.
 *
 * Used when persisting assessment history ([org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel])
 * so service-specific vitals and answers can be stored and later displayed in history summaries
 * ([org.medtroniclabs.uhis.ui.household.adapter.MemberAssessmentHistoryAdapterUtil]).
 *
 * Each supported [menuId] has a dedicated builder that reads nested keys from [assessmentMap].
 * Returns `null` when the service is unsupported or no displayable observation values are found.
 */
object AssessmentObservationUtils {
    private const val ANY_COMPLICATIONS_DURING_DELIVERY = "anyComplicationsDuringDelivery"
    private const val COMPLICATIONS_DURING_DELIVERY = "complicationsDuringDelivery"
    private const val ANC_VISITS_OTHER_PROVIDERS = "ancVisitsOtherProviders"

    /**
     * Maps [assessmentMap] to [MemberAssessmentObservations] for [menuId].
     *
     * Supported services: NCD, cataract, pregnant women profile, pregnancy outcome,
     * ANC, PNC mother, and family planning. Unsupported or empty results return `null`.
     */
    fun buildMemberAssessmentObservations(
        assessmentMap: HashMap<String, Any>,
        menuId: String?,
        pregnancyEpisodeId: String?,
    ): MemberAssessmentObservations? {
        val service = menuId ?: return null
        return when (service.lowercase()) {
            MenuConstants.NCD_MENU_ID.lowercase(),
            MenuConstants.CATARACT_MENU_ID.lowercase(),
            -> {
                buildNCDCataractObservations(assessmentMap, service)
            }

            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase() ->
                buildPregnantWomenProfileObservations(assessmentMap)

            MenuConstants.PREGNANCY_OUTCOME.lowercase() ->
                buildPregnancyOutcomeObservations(assessmentMap, pregnancyEpisodeId)

            MenuConstants.ANC.lowercase() ->
                buildAncObservations(assessmentMap)

            MenuConstants.PNC_MOTHER.lowercase() ->
                buildPncObservations(assessmentMap)

            MenuConstants.FP_MENU_ID.lowercase() ->
                buildFamilyPlanningObservations(assessmentMap)

            else -> null
        }
    }

    /**
     * Height, weight, BP, and BG from NCD or cataract vitals sections.
     */
    private fun buildNCDCataractObservations(
        assessmentMap: HashMap<String, Any>,
        service: String,
    ): MemberAssessmentObservations? {
        val vitalsMap = resolveVitalsMap(assessmentMap, service) ?: return null
        val height = resolveObservationHeight(vitalsMap)
        val weight = resolveObservationWeight(vitalsMap)
        val bp = resolveObservationBloodPressure(vitalsMap)
        val bg = resolveObservationBloodGlucose(vitalsMap)
        val bgType = resolveObservationBloodGlucoseType(vitalsMap)

        if (height == null && weight == null && bp == null && bg == null) return null

        return MemberAssessmentObservations(
            height = height,
            weight = weight,
            bp = bp,
            bg = bg,
            bgType = bgType,
        )
    }

    /**
     * Gravida and parity from the pregnancy details and history group.
     */
    private fun buildPregnantWomenProfileObservations(assessmentMap: HashMap<String, Any>): MemberAssessmentObservations? {
        val profile = assessmentMap[MenuConstants.PREGNANT_WOMEN_PROFILE] as? Map<*, *> ?: return null
        val pregnancyHistory = profile[PregnantWomen.ID_PREGNANCY_DETAILS_AND_HISTORY] as? Map<*, *> ?: return null
        val gravida = formatNumericValue(pregnancyHistory[PregnantWomen.ID_GRAVIDA])
        val parity = formatNumericValue(pregnancyHistory[PregnantWomen.ID_PARITY])

        if (gravida == null && parity == null) return null

        return MemberAssessmentObservations(
            gravida = gravida,
            parity = parity,
        )
    }

    /**
     * Pregnancy outcome counts, flags, delivery details, and episode id.
     */
    private fun buildPregnancyOutcomeObservations(
        assessmentMap: HashMap<String, Any>,
        pregnancyEpisodeId: String?,
    ): MemberAssessmentObservations? {
        val outcome = assessmentMap[MenuConstants.PREGNANCY_OUTCOME] as? Map<*, *> ?: return null

        val ancGroup = outcome[GROUP_ANC_SERVICES_BIRTH_PREPAREDNESS] as? Map<*, *>
        val ancVisitsOtherProviders = formatNumericValue(ancGroup?.get(ANC_VISITS_OTHER_PROVIDERS))

        val deliveryOutcomes = outcome[ID_DELIVERY_OUTCOMES] as? Map<*, *>
        val liveBirthNumbers = formatNumericValue(deliveryOutcomes?.get(ID_LIVE_BIRTH_NUMBERS))
        val stillbirthNumbers = formatNumericValue(deliveryOutcomes?.get(ID_STILL_BIRTH_NUMBERS))
        val modeOfDelivery = stringValue(deliveryOutcomes?.get(ID_MODE_OF_DELIVERY))
        val anyComplications = stringValue(deliveryOutcomes?.get(ANY_COMPLICATIONS_DURING_DELIVERY))
        val complications = joinListValue(deliveryOutcomes?.get(COMPLICATIONS_DURING_DELIVERY))

        val maternalDeath = resolveYesNoFlag(isMaternalDeathOutcome(outcome))
        val abortion = resolveYesNoFlag(isAbortionOutcome(outcome))
        val newbornDeathNumbers = formatNewbornDeathCount(outcome)
        val episodeId = pregnancyEpisodeId?.trim()?.takeIf { it.isNotEmpty() }

        if (
            ancVisitsOtherProviders == null &&
            liveBirthNumbers == null &&
            stillbirthNumbers == null &&
            modeOfDelivery == null &&
            anyComplications == null &&
            complications == null &&
            maternalDeath == null &&
            abortion == null &&
            newbornDeathNumbers == null &&
            episodeId == null
        ) {
            return null
        }

        return MemberAssessmentObservations(
            modeOfDelivery = modeOfDelivery,
            anyComplicationsDuringDelivery = anyComplications,
            complicationsDuringDelivery = complications,
            ancVisitsOtherProviders = ancVisitsOtherProviders,
            liveBirthNumbers = liveBirthNumbers,
            stillbirthNumbers = stillbirthNumbers,
            maternalDeath = maternalDeath,
            abortion = abortion,
            newbornDeathNumbers = newbornDeathNumbers,
            pregnancyEpisodeId = episodeId,
        )
    }

    private fun resolveYesNoFlag(condition: Boolean): String? = if (condition) YES else null

    private fun isMaternalDeathOutcome(outcome: Map<*, *>): Boolean {
        if (pregnancyOutcomeType(outcome).equals(MATERNAL_DEATH, ignoreCase = true)) {
            return true
        }
        val maternalDeathMap = outcome[MATERNAL_DEATH] as? Map<*, *>
        return resolveOptionId(maternalDeathMap?.get(TIME_OF_DEATH)) != null
    }

    private fun isAbortionOutcome(outcome: Map<*, *>): Boolean {
        if (pregnancyOutcomeType(outcome).equals(ID_ABORTION, ignoreCase = true)) {
            return true
        }
        val abortionMap = outcome[ID_ABORTION] as? Map<*, *>
        val typeOfAbortion = stringValue(abortionMap?.get(TYPE_OF_ABORTION))
        val gestationMonth = formatNumericValue(abortionMap?.get(GESTATION_MONTH_AT_ABORTION))
        return typeOfAbortion != null || gestationMonth != null
    }

    @Suppress("UNCHECKED_CAST")
    private fun formatNewbornDeathCount(outcome: Map<*, *>): String? {
        val newbornDetailsList = AssessmentUtil.findNewbornDetailsFromMap(outcome as Map<String, Any?>) ?: return null
        val deathCount = newbornDetailsList.count { babyData ->
            babyData is Map<*, *> && isNewbornDead(babyData)
        }
        return if (deathCount > 0) formatNumericValue(deathCount) else null
    }

    private fun isNewbornDead(babyData: Map<*, *>): Boolean {
        return when (val isBabyAlive = babyData[IS_BABY_ALIVE]) {
            is String -> !isBabyAlive.equals(YES, ignoreCase = true)

            is Boolean -> !isBabyAlive

            else -> {
                val value = isBabyAlive?.toString() ?: return false
                !value.equals(DefinedParams.YES, ignoreCase = true)
            }
        }
    }

    private fun pregnancyOutcomeType(outcome: Map<*, *>): String? {
        val outcomeTypeCard = outcome[MenuConstants.PREGNANCY_OUTCOME] as? Map<*, *>
        return stringValue(outcomeTypeCard?.get(ID_PREGNANCY_OUTCOME_TYPE))
            ?: resolveOptionId(outcomeTypeCard?.get(ID_PREGNANCY_OUTCOME_TYPE))
    }

    private fun resolveOptionId(value: Any?): String? =
        when (value) {
            is String -> value.trim().takeIf { it.isNotEmpty() && it != DefinedParams.DEFAULT_ID }
            is Map<*, *> -> {
                val id = (value[DefinedParams.ID] ?: value["id"])?.toString()?.trim()
                id?.takeIf { it.isNotEmpty() && it != DefinedParams.DEFAULT_ID }
            }

            else -> null
        }

    /**
     * ANC visit number, weight, fundal height, and hemoglobin.
     */
    private fun buildAncObservations(assessmentMap: HashMap<String, Any>): MemberAssessmentObservations? {
        val ancMap = assessmentMap[RMNCH.ANC] as? Map<*, *> ?: return null
        val medicalExamination = ancMap[GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION] as? Map<*, *>
        val pointOfCare = ancMap[GROUP_POINT_OF_CARE_INVESTIGATIONS] as? Map<*, *>

        val visitNumber = formatNumericValue(ancMap[RMNCH.visitNo])
        val weight = formatNumericValue(medicalExamination?.get(WEIGHT))
        val fundalHeight = formatNumericValue(medicalExamination?.get(FUNDAL_HEIGHT))
        val hemoglobin = formatNumericValue(pointOfCare?.get(HEMOGLOBIN))

        if (visitNumber == null && weight == null && fundalHeight == null && hemoglobin == null) return null

        return MemberAssessmentObservations(
            ancVisitNumber = visitNumber,
            weight = weight,
            fundalHeight = fundalHeight,
            hemoglobin = hemoglobin,
        )
    }

    /**
     * PNC visit number and maternal hemoglobin.
     */
    private fun buildPncObservations(assessmentMap: HashMap<String, Any>): MemberAssessmentObservations? {
        val pncMap = assessmentMap[RMNCH.PNC] as? Map<*, *> ?: return null
        val maternalHealth = pncMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>

        val visitNumber = formatNumericValue(pncMap[RMNCH.visitNo])
        val hemoglobin = formatNumericValue(maternalHealth?.get(RMNCH.ID_HEMOGLOBIN))

        if (visitNumber == null && hemoglobin == null) return null

        return MemberAssessmentObservations(
            pncVisitNumber = visitNumber,
            hemoglobin = hemoglobin,
        )
    }

    /**
     * Family planning method, desire for children, and number of living children.
     */
    private fun buildFamilyPlanningObservations(assessmentMap: HashMap<String, Any>): MemberAssessmentObservations? {
        // getAssessmentDetails moves family_planning -> clientProfileAssessment content under familyPlanning.
        val fpDetails = (assessmentMap[FamilyPlanning] as? Map<*, *>)
            ?: ((assessmentMap[MenuConstants.FP_MENU_ID] as? Map<*, *>)?.get(FamilyPlanningDetails) as? Map<*, *>)
            ?: return null

        val familyPlanningMethod = firstListValue(fpDetails[FamilyPlanningMethods])
        val desireForChildren = stringValue(fpDetails[DESIRE_FOR_CHILDREN_IN_FUTURE])
        val numberOfLivingChildren = formatNumericValue(fpDetails[NUMBER_OF_LIVING_CHILDREN])

        if (familyPlanningMethod == null && desireForChildren == null && numberOfLivingChildren == null) return null

        return MemberAssessmentObservations(
            familyPlanningMethods = familyPlanningMethod,
            desireForChildrenInFuture = desireForChildren,
            numberOfLivingChildren = numberOfLivingChildren,
        )
    }

    /**
     * Locates the vitals subsection for NCD or cataract assessments.
     */
    private fun resolveVitalsMap(
        assessmentMap: HashMap<String, Any>,
        menuId: String,
    ): HashMap<String, Any>? =
        when (menuId.lowercase()) {
            MenuConstants.NCD_MENU_ID.lowercase() ->
                (assessmentMap[ncd] ?: assessmentMap[MenuConstants.NCD_MENU_ID]) as? HashMap<String, Any>

            MenuConstants.CATARACT_MENU_ID.lowercase() ->
                resolveCataractVitalsMap(assessmentMap)

            else -> null
        }

    /**
     * Merges cataract root with nested [ncd] vitals when NCD service is provided.
     */
    private fun resolveCataractVitalsMap(assessmentMap: HashMap<String, Any>): HashMap<String, Any>? {
        val cataractSection = (assessmentMap[CATARACT] ?: assessmentMap[MenuConstants.CATARACT_MENU_ID])
            as? HashMap<String, Any> ?: return null
        val ncdSection = (cataractSection[ncd] ?: cataractSection[MenuConstants.NCD_MENU_ID])
            as? HashMap<String, Any> ?: return cataractSection
        return HashMap<String, Any>().apply {
            putAll(cataractSection)
            putAll(ncdSection)
        }
    }

    private fun resolveObservationHeight(map: HashMap<String, Any>): String? = resolveObservationNumber(map, HEIGHT)

    private fun resolveObservationWeight(map: HashMap<String, Any>): String? = resolveObservationNumber(map, WEIGHT)

    private fun resolveObservationNumber(
        map: HashMap<String, Any>,
        key: String,
    ): String? {
        val bpLogs = map[BP_LOG] as? HashMap<*, *>
        (bpLogs?.get(key) as? Number)?.let { return formatObservationNumber(it) }
        for (familyKey in listOf(BIOMETRIC_FAMILY, BIO_METRICS)) {
            val family = map[familyKey] as? HashMap<*, *> ?: continue
            (family[key] as? Number)?.let { return formatObservationNumber(it) }
        }
        return (map[key] as? Number)?.let { formatObservationNumber(it) }
    }

    private fun formatObservationNumber(value: Number): String {
        val doubleValue = value.toDouble()
        return if (doubleValue == doubleValue.roundToInt().toDouble()) {
            doubleValue.roundToInt().toString()
        } else {
            doubleValue.toString()
        }
    }

    /**
     * Formats a [Number] or numeric [String] into a trimmed display value; null when not resolvable.
     */
    private fun formatNumericValue(value: Any?): String? =
        when (value) {
            is Number -> formatObservationNumber(value)
            is String -> {
                val trimmed = value.trim()
                trimmed.toDoubleOrNull()?.let { formatObservationNumber(it) }
                    ?: trimmed.takeIf { it.isNotEmpty() }
            }

            else -> null
        }

    private fun stringValue(value: Any?): String? = (value as? String)?.trim()?.takeIf { it.isNotEmpty() }

    /**
     * Returns the first option of a checkbox/spinner list value as a display string.
     */
    private fun firstListValue(value: Any?): String? =
        when (value) {
            is List<*> -> value.firstOrNull()?.let { listItemValue(it) }
            else -> stringValue(value)
        }

    /**
     * Joins all list option values into a comma separated display string.
     */
    private fun joinListValue(value: Any?): String? =
        when (value) {
            is List<*> ->
                value
                    .mapNotNull { listItemValue(it) }
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(", ")

            else -> stringValue(value)
        }

    private fun listItemValue(item: Any?): String? =
        when (item) {
            is String -> item.trim().takeIf { it.isNotEmpty() }
            is Map<*, *> -> (item[DefinedParams.Value] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            else -> item?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        }

    /**
     * Prefers stored averages from [BP_LOG]; falls back to [calculateAverageBloodPressure].
     */
    private fun resolveObservationBloodPressure(map: HashMap<String, Any>): String? {
        val bpLogs = map[BP_LOG] as? HashMap<String, Any> ?: return null

        (bpLogs[AVG_BLOOD_PRESSURE] as? String)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }

        val sys = (bpLogs[AVG_SYSTOLIC] as? Number)?.toInt()
        val dia = (bpLogs[AVG_DIASTOLIC] as? Number)?.toInt()
        if (sys != null && sys > 0 && dia != null && dia > 0) {
            return "$sys/$dia"
        }

        val (computedSys, computedDia) = calculateAverageBloodPressure(map)
        return if (computedSys > 0 && computedDia > 0) {
            "$computedSys/$computedDia"
        } else {
            null
        }
    }

    /**
     * Reads the latest glucose value from [GLUCOSE_LOG]; ignores zero or missing readings.
     */
    private fun resolveObservationBloodGlucose(map: HashMap<String, Any>): String? {
        val glucoseLogs = map[GLUCOSE_LOG] as? HashMap<*, *> ?: return null
        val glucose = glucoseLogs[GLUCOSE] as? Number ?: return null
        if (glucose.toDouble() <= 0) return null
        return formatObservationNumber(glucose)
    }

    private fun resolveObservationBloodGlucoseType(map: HashMap<String, Any>): String? {
        val glucoseLogs = map[GLUCOSE_LOG] as? HashMap<*, *>
        return resolveGlucoseTypeValue(glucoseLogs?.get(GLUCOSE_TYPE))
            ?: resolveGlucoseTypeValue(map[GLUCOSE_TYPE])
            ?: resolveGlucoseTypeValue(map["bgType"])
    }

    private fun resolveGlucoseTypeValue(value: Any?): String? =
        when (value) {
            is String -> value.trim().takeIf { it.isNotEmpty() }
            is Map<*, *> -> {
                sequenceOf(
                    value[DefinedParams.ID],
                    value["id"],
                    value["name"],
                    value[NAME],
                ).firstNotNullOfOrNull { resolveGlucoseTypeValue(it) }
            }

            else -> null
        }
}
