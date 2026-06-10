package org.medtroniclabs.uhis.ui.assessment.utils

import android.content.Context
import android.view.View
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentObservations
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.model.BPModel
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_BLOOD_PRESSURE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_DIASTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AVG_SYSTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIOMETRIC_FAMILY
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BIO_METRICS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.BP_LOG_DETAILS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CATARACT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FBS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.GLUCOSE_UNIT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Glucose_Date_Time
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HBA1C_DATE_TIME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MMOLL
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NCD_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SYMPTOMS_LOG
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.WEIGHT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ncd
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.statuslogic.AssessmentStatus
import java.util.Locale
import kotlin.math.roundToInt

object AssessmentUtil {
    fun calculateAverageBloodPressure(resultMap: HashMap<String, Any>): Pair<Int, Int> {
        val bpLogs = resultMap[BP_LOG] as HashMap<String, Any>

        val bpLogDetail = bpLogs[BP_LOG_DETAILS] as? List<*> ?: return Pair(0, 0)

        val validReadings = bpLogDetail.mapNotNull { entry ->
            val sys = getSystolicValue(entry)
            val dia = getDiastolicValue(entry)

            if (sys > 0 && dia > 0) {
                sys to dia
            } else {
                null
            }
        }

        if (validReadings.isEmpty()) {
            bpLogs.remove(BP_LOG_DETAILS)
            return Pair(0, 0)
        }

        val avgSys = validReadings.map { it.first }.average()
        val avgDia = validReadings.map { it.second }.average()

        val finalSys = avgSys.roundToInt()
        val finalDia = avgDia.roundToInt()

        bpLogs[AVG_SYSTOLIC] = finalSys
        bpLogs[AVG_DIASTOLIC] = finalDia
        bpLogs[AVG_BLOOD_PRESSURE] = "$finalSys/$finalDia"
        bpLogs[BP_LOG_DETAILS] = bpLogDetail

        resultMap[BP_LOG] = bpLogs

        return Pair(finalSys, finalDia)
    }

    private fun getSystolicValue(map: Any?): Double = (map as? BPModel)?.systolic ?: 0.0

    private fun getDiastolicValue(map: Any?): Double = (map as? BPModel)?.diastolic ?: 0.0

    fun addDateAndTimeForGlucose(resultMap: HashMap<String, Any>): Triple<String?, String?, Double?> {
        if (resultMap.containsKey(GLUCOSE_LOG)) {
            val glucoseLogs = resultMap[GLUCOSE_LOG] as HashMap<String, Any>

            var unitType = MMOLL // Default unit type
            glucoseLogs[GLUCOSE_UNIT]?.let {
                unitType = it as String
            }

            var bgType = FBS // Default bg type
            glucoseLogs[GLUCOSE_TYPE]?.let {
                bgType = it as String
            }

            var bgValue = 0.0 // Default bg value
            glucoseLogs[GLUCOSE]?.let {
                bgValue = it as Double
            }

            val dateTime = DateUtils.getTodayDateDDMMYYYY()
            glucoseLogs[Glucose_Date_Time] = dateTime
            glucoseLogs[HBA1C_DATE_TIME] = dateTime

            resultMap[GLUCOSE_LOG] = glucoseLogs

            return Triple(unitType, bgType, bgValue)
        }
        return Triple(null, null, null)
    }

    fun getSymptomsList(resultMap: HashMap<String, Any>): List<String> {
        val list = mutableListOf<String>()

        val symptomsLogs = resultMap[SYMPTOMS_LOG] as? HashMap<String, Any> ?: return list
        if (symptomsLogs.containsKey(NCD_SYMPTOMS)) {
            val ncdSymptoms = symptomsLogs[NCD_SYMPTOMS] as List<*>

            ncdSymptoms.forEach {
                val symptom = it as HashMap<String, Any>
                if (symptom.containsKey(NAME)) {
                    list.add(symptom[NAME] as String)
                }
            }
        }

        return list
    }

    /**
     * Maps given service to respective service name, if no mapping found then returns upper case of service
     *
     * e.g, pwProfile -> Pregnant Women Registration
     */
    fun mapServiceToServiceName(
        service: String,
        context: Context,
    ): String =
        when (service.lowercase()) {
            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase() -> context.getString(R.string.pregnant_women_profile)
            MenuConstants.FP_MENU_ID.lowercase() -> context.getString(R.string.family_planning)
            MenuConstants.EYE_CARE_MENU_ID.lowercase() -> context.getString(R.string.eye_care)
            MenuConstants.CATARACT_MENU_ID.lowercase() -> context.getString(R.string.cataract)
            MenuConstants.NCD_MENU_ID.lowercase() -> context.getString(R.string.ncd)
            MenuConstants.PREGNANCY_OUTCOME.lowercase() -> context.getString(R.string.pregnancy_outcome)
            RMNCH.ANC.lowercase() -> context.getString(R.string.anc)
            RMNCH.PNC_MOTHER_MENU.lowercase() -> context.getString(R.string.pnc)
            RMNCH.CHILD_MENU.lowercase() -> context.getString(R.string.child_health)
            else -> service.uppercase(Locale.ENGLISH)
        }

    /**
     * Returns referral status based on service
     */
    fun getReferralStatus(
        context: Context,
        service: String,
        referralStatus: String?,
    ): String =
        when (service.lowercase()) {
            RMNCH.ANC.lowercase(),
            RMNCH.PNC_MOTHER_MENU.lowercase(),
            RMNCH.CHILD_MENU.lowercase(),
            -> {
                if (ReferralStatus.Referred.name.equals(referralStatus, true)) {
                    referralStatus?.uppercase(Locale.ENGLISH)
                } else {
                    context.getString(R.string.na)
                }
            }

            MenuConstants.NCD_MENU_ID, MenuConstants.CATARACT_MENU_ID -> {
                referralStatus ?: context.getString(R.string.na)
            }

            else -> context.getString(R.string.na)
        } ?: run { context.getString(R.string.separator_double_hyphen) }

    fun shouldShowNextFollowUpDate(service: String): Boolean =
        when (service.lowercase()) {
            MenuConstants.NCD_MENU_ID.lowercase(),
            MenuConstants.CATARACT_MENU_ID.lowercase(),
            MenuConstants.EYE_CARE_MENU_ID.lowercase(),
            -> false
            else -> true
        }

    /**
     * Returns display follow-up date based on service
     */
    fun getNextFollowUpDate(
        context: Context,
        service: String,
        followUpDate: String?,
    ): String =
        when (service.lowercase()) {
            RMNCH.ANC.lowercase(),
            RMNCH.PNC_MOTHER_MENU.lowercase(),
            -> {
                followUpDate?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DATE_ddMMyyyy,
                    )
                }
            }

            else -> context.getString(R.string.na)
        } ?: run { context.getString(R.string.separator_double_hyphen) }

    /**
     * Formats saved custom status values for Service History, combining High BP/BG when both apply
     * and omitting legacy controlled vitals statuses.
     */
    fun formatServiceHistoryCurrentStatus(
        customStatus: List<String>?,
        context: Context,
        serviceProvided: String? = null,
        referralStatus: String? = null,
    ): String? {
        val effectiveStatus = applyNcdNormalStatusFallback(customStatus, serviceProvided, referralStatus)
        if (effectiveStatus.isEmpty()) return null

        val hasHighBp = effectiveStatus.contains(AssessmentStatus.UNCONTROLLED_BP.name)
        val hasHighBg = effectiveStatus.contains(AssessmentStatus.UNCONTROLLED_BG.name)
        val otherStatuses = effectiveStatus.filter { status ->
            status != AssessmentStatus.UNCONTROLLED_BP.name &&
                status != AssessmentStatus.UNCONTROLLED_BG.name &&
                status != AssessmentStatus.CONTROLLED_BP.name &&
                status != AssessmentStatus.CONTROLLED_BG.name
        }

        val displayParts = mutableListOf<String>()
        when {
            hasHighBp && hasHighBg ->
                displayParts.add(context.getString(R.string.high_both))
            hasHighBp ->
                displayParts.add(context.getString(R.string.high_bp))
            hasHighBg ->
                displayParts.add(context.getString(R.string.high_bg))
        }
        otherStatuses.forEach { status ->
            mapAssessmentStatus(status, context).takeIf { it.isNotBlank() }?.let { displayParts.add(it) }
        }
        return displayParts.takeIf { it.isNotEmpty() }?.joinToString()
    }

    private fun applyNcdNormalStatusFallback(
        customStatus: List<String>?,
        serviceProvided: String?,
        referralStatus: String?,
    ): List<String> {
        val statuses = customStatus?.toMutableList() ?: mutableListOf()
        if (serviceProvided?.lowercase() != MenuConstants.NCD_MENU_ID.lowercase()) return statuses
        if (ReferralStatus.Referred.name.equals(referralStatus, true)) return statuses
        if (statuses.contains(AssessmentStatus.UNCONTROLLED_BP.name) ||
            statuses.contains(AssessmentStatus.UNCONTROLLED_BG.name)
        ) {
            return statuses
        }
        if (!statuses.contains(AssessmentStatus.NORMAL_NCD.name)) {
            statuses.add(0, AssessmentStatus.NORMAL_NCD.name)
        }
        return statuses
    }

    /**
     * Maps corresponding status to display value
     */
    fun mapAssessmentStatus(
        status: String,
        context: Context,
    ): String {
        if (status.startsWith(AssessmentDefinedParams.GLASS_POWER_STATUS_PREFIX)) {
            val power = status.removePrefix(AssessmentDefinedParams.GLASS_POWER_STATUS_PREFIX)
            return context.getString(R.string.assessment_status_glass_power, power)
        }
        val assessmentStatus = try {
            AssessmentStatus.valueOf(status)
        } catch (_: Exception) {
            AssessmentStatus.DEFAULT
        }
        return when (assessmentStatus) {
            AssessmentStatus.NORMAL_PREGNANCY -> {
                context.getString(R.string.normal_pregnancy)
            }

            AssessmentStatus.HIGH_RISK_PW -> {
                context.getString(R.string.high_risk_pw)
            }

            AssessmentStatus.USING_MODERN_FP -> {
                context.getString(R.string.using_modern_family_planning_methods)
            }

            AssessmentStatus.NOT_USING_MODERN_FP -> {
                context.getString(R.string.not_using_modern_family_planning_methods)
            }

            AssessmentStatus.GAPS_IN_ANC -> {
                context.getString(R.string.gaps_in_anc)
            }

            AssessmentStatus.C_SECTION -> {
                context.getString(R.string.c_section)
            }

            AssessmentStatus.ASSISTED_DELIVERY -> {
                context.getString(R.string.assisted_delivery)
            }

            AssessmentStatus.NORMAL_DELIVERY -> {
                context.getString(R.string.normal_delivery)
            }

            AssessmentStatus.NEONATAL_DEATH -> {
                context.getString(R.string.neo_natal_death)
            }

            AssessmentStatus.ABORTION -> {
                context.getString(R.string.abortion)
            }

            AssessmentStatus.STILL_BIRTH -> {
                context.getString(R.string.still_birth)
            }

            AssessmentStatus.LIVE_BIRTH -> {
                context.getString(R.string.live_birth)
            }

            AssessmentStatus.HIGH_RISK_PNC -> {
                context.getString(R.string.high_risk_pnc)
            }

            AssessmentStatus.NORMAL_PNC -> {
                context.getString(R.string.normal_pnc)
            }

            AssessmentStatus.GAPS_IN_PNC -> {
                context.getString(R.string.gaps_in_pnc)
            }

            AssessmentStatus.CONTROLLED_BP,
            AssessmentStatus.CONTROLLED_BG,
            -> ""

            AssessmentStatus.UNCONTROLLED_BP -> {
                context.getString(R.string.high_bp)
            }
            AssessmentStatus.UNCONTROLLED_BG -> {
                context.getString(R.string.high_bg)
            }

            AssessmentStatus.NORMAL_NCD -> {
                context.getString(R.string.normal)
            }

            AssessmentStatus.GLASSES_SOLD -> {
                context.getString(R.string.assessment_status_glasses_sold)
            }

            AssessmentStatus.NCD_SERVICE_IN_CATARACT_CAMP -> {
                context.getString(R.string.assessment_status_ncd_service_cataract_camp)
            }

            AssessmentStatus.REFERRED_FOR_OPERATION -> {
                context.getString(R.string.assessment_status_referred_for_operation)
            }

            AssessmentStatus.CATARACTS -> {
                context.getString(R.string.assessment_status_cataracts)
            }

            AssessmentStatus.LECRIMAL_TEAR_DUCT_PROBLEM -> {
                context.getString(R.string.assessment_status_lacrimal_tear_duct_problem)
            }

            AssessmentStatus.PTERYGIUM -> {
                context.getString(R.string.assessment_status_pterygium)
            }

            AssessmentStatus.GLAUCOMA -> {
                context.getString(R.string.assessment_status_glaucoma)
            }

            AssessmentStatus.MYOPIA -> {
                context.getString(R.string.assessment_status_myopia)
            }

            AssessmentStatus.PRESBYOPIA -> {
                context.getString(R.string.assessment_status_presbyopia)
            }

            AssessmentStatus.OTHER_EYE_PROBLEM -> {
                context.getString(R.string.assessment_status_other_eye_problem)
            }

            AssessmentStatus.NO_EYE_PROBLEM -> {
                context.getString(R.string.assessment_status_no_eye_problem)
            }

            AssessmentStatus.DEFAULT,
            -> {
                status.uppercase(Locale.ENGLISH)
            }
        }
    }

    /**
     * Returns newborn details from the map as list
     */
    fun findNewbornDetailsFromMap(map: Map<String, Any?>): List<*>? {
        val directList = map[AssessmentDefinedParams.NEWBORN_DETAILS]
        if (directList is List<*>) return directList

        for (entry in map.entries) {
            if (entry.value is Map<*, *>) {
                val nestedMap = entry.value as Map<*, *>
                val nestedList = nestedMap[AssessmentDefinedParams.NEWBORN_DETAILS]
                if (nestedList is List<*>) return nestedList
            }
        }
        return null
    }

    /**
     * Returns services icon for the given service
     */
    fun mapServiceToServiceIcon(service: String): Int =
        when (service.lowercase()) {
            MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase() -> R.drawable.ic_services_anc
            RMNCH.ANC.lowercase() -> R.drawable.ic_services_anc
            MenuConstants.FP_MENU_ID.lowercase() -> R.drawable.ic_services_family_planning
            RMNCH.CHILD_MENU.lowercase() -> R.drawable.ic_services_child_health
            MenuConstants.PREGNANCY_OUTCOME.lowercase() -> R.drawable.ic_services_pnc
            RMNCH.PNC_MOTHER_MENU.lowercase() -> R.drawable.ic_services_pnc
            MenuConstants.NCD_MENU_ID.lowercase() -> R.drawable.ic_services_ncd
            MenuConstants.EYE_CARE_MENU_ID.lowercase() -> R.drawable.ic_services_eye_care
            MenuConstants.CATARACT_MENU_ID.lowercase() -> R.drawable.ic_cataract
            else -> {
                View.NO_ID
            }
        }

    fun getLocalServiceProvidedByName(): String? {
        val user = SecuredPreference.getUserDetails() ?: return null
        val name = listOfNotNull(user.firstName?.trim(), user.lastName?.trim()).joinToString(" ")
        return name.takeIf { it.isNotBlank() } ?: user.username?.trim()?.takeIf { it.isNotBlank() }
    }

    fun getLocalServiceProvidedByRole(): String? = SecuredPreference.getRole().takeIf { it.isNotBlank() }

    fun formatServiceProviderDisplay(
        context: Context,
        name: String?,
        role: String?,
    ): String {
        val providerName = name?.takeIf { it.isNotBlank() }
        val providerRole = role?.takeIf { it.isNotBlank() }
        return when {
            providerName != null && providerRole != null -> "$providerName ($providerRole)"
            providerName != null -> providerName
            providerRole != null -> providerRole
            else -> context.getString(R.string.separator_double_hyphen)
        }
    }

    fun getHeightWeightFromHistory(history: MemberAssessmentHistoryEntity): Pair<String?, String?> {
        val observations = history.observations
        val height = observations?.height?.trim()?.takeIf { it.isNotEmpty() }
        val weight = observations?.weight?.trim()?.takeIf { it.isNotEmpty() }
        return height to weight
    }

    fun getLatestHeightWeightFromHistories(histories: List<MemberAssessmentHistoryEntity>): Pair<String?, String?>? =
        histories
            .mapNotNull { history ->
                val (height, weight) = getHeightWeightFromHistory(history)
                if (height == null && weight == null) return@mapNotNull null
                val visitMillis = DateUtils.getLastMenstrualDate(history.visitDate ?: "").timeInMillis
                Triple(visitMillis, height, weight)
            }.maxByOrNull { it.first }
            ?.let { it.second to it.third }

    fun prefillHeightAndWeight(
        formGenerator: FormGenerator,
        height: String?,
        weight: String?,
        isHeightReadOnly: Boolean = false,
    ) {
        height?.let { value ->
            formGenerator.getViewByTag(Screening.Height)?.let { view ->
                formGenerator.setValueForView(value, view)
                if (isHeightReadOnly) {
                    view.isEnabled = false
                }
            }
        }
        weight?.let { value ->
            formGenerator.getViewByTag(Screening.Weight)?.let { view ->
                formGenerator.setValueForView(value, view)
            }
        }
    }

    fun buildMemberAssessmentObservations(
        assessmentMap: HashMap<String, Any>,
        menuId: String?,
    ): MemberAssessmentObservations? {
        val service = menuId ?: return null
        if (!shouldShowServiceObservations(service)) return null

        val vitalsMap = resolveVitalsMap(assessmentMap, service) ?: return null
        val height = resolveObservationHeight(vitalsMap)
        val weight = resolveObservationWeight(vitalsMap)
        val bp = resolveObservationBloodPressure(vitalsMap)
        val bg = resolveObservationBloodGlucose(vitalsMap)

        if (height == null && weight == null && bp == null && bg == null) return null

        return MemberAssessmentObservations(
            height = height,
            weight = weight,
            bp = bp,
            bg = bg,
        )
    }

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

    /** Merges cataract root with nested [ncd] vitals when NCD service is provided. */
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

    private fun resolveObservationBloodGlucose(map: HashMap<String, Any>): String? {
        val glucoseLogs = map[GLUCOSE_LOG] as? HashMap<*, *> ?: return null
        val glucose = glucoseLogs[GLUCOSE] as? Number ?: return null
        if (glucose.toDouble() <= 0) return null
        return formatObservationNumber(glucose)
    }

    fun shouldShowServiceObservations(service: String): Boolean =
        when (service.lowercase()) {
            MenuConstants.NCD_MENU_ID.lowercase(),
            MenuConstants.CATARACT_MENU_ID.lowercase(),
            -> true
            else -> false
        }

    fun formatServiceHistoryBloodPressure(
        context: Context,
        bp: String?,
    ): String {
        val value = bp?.trim()?.takeIf { it.isNotEmpty() }
        return value?.let {
            if (it.contains("mmHg", ignoreCase = true)) {
                it
            } else {
                "$it ${context.getString(R.string.mmHg)}"
            }
        } ?: context.getString(R.string.separator_double_hyphen)
    }

    fun formatServiceHistoryBloodGlucose(
        context: Context,
        bg: String?,
    ): String {
        val value = bg?.trim()?.takeIf { it.isNotEmpty() }
        return value?.let {
            if (it.contains("mmol", ignoreCase = true)) {
                it
            } else {
                "$it $MMOLL"
            }
        } ?: context.getString(R.string.separator_double_hyphen)
    }
}
