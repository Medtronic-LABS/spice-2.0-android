package org.medtroniclabs.uhis.ui.assessment.statuslogic

import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.ncd.screening.utils.ReferredReason.bloodGlucose
import org.medtroniclabs.uhis.ncd.screening.utils.ReferredReason.bloodPressure
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_CARE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_HAVE_THE_GLASSES_BEEN_SOLD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NCD_SERVICE_PROVIDED
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.YES
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil

/**
 * Object class to evaluate status for different work flows
 */
object AssessmentStatusGenerator {
    private fun getCataractFieldSection(map: HashMap<String, Any>): Map<*, *>? {
        val wrapped = map[MenuConstants.CATARACT_MENU_ID] as? Map<*, *> ?: return null
        val inner = wrapped[AssessmentDefinedParams.CATARACT] as? Map<*, *>
        return inner ?: wrapped
    }

    private fun isNcdServiceProvidedInCataract(cataractSection: Map<*, *>?): Boolean = YES.equals(cataractSection?.get(NCD_SERVICE_PROVIDED)?.toString(), true)

    private fun ArrayList<AssessmentStatus>.toStatusStrings(extraTokens: List<String> = emptyList()): ArrayList<String> =
        ArrayList(map { it.name } + extraTokens)

    private fun extractEyeProblemIds(section: Map<*, *>?): List<String> {
        if (section == null) return emptyList()
        val ids = mutableListOf<String>()
        (section[AssessmentDefinedParams.EYE_DISEASE] as? List<*>)?.forEach { item ->
            when (item) {
                is String -> ids.add(item)
                is Map<*, *> -> (item[DefinedParams.ID] as? String)?.let { ids.add(it) }
            }
        }
        (section[AssessmentDefinedParams.EYE_TEST_OUTCOME] as? String)?.takeIf { it.isNotBlank() }?.let { ids.add(it) }
        (section[AssessmentDefinedParams.EYE_TEST_OUTCOMES] as? List<*>)?.forEach { item ->
            when (item) {
                is String -> ids.add(item)
                is Map<*, *> -> (item[DefinedParams.ID] as? String)?.let { ids.add(it) }
            }
        }
        return ids.distinct()
    }

    private fun mapEyeProblemIdToStatus(id: String): AssessmentStatus? =
        when (id) {
            AssessmentDefinedParams.EYE_PROBLEM_CATARACTS -> AssessmentStatus.CATARACTS
            AssessmentDefinedParams.EYE_PROBLEM_LECRIMAL_TEAR_DUCT -> AssessmentStatus.LECRIMAL_TEAR_DUCT_PROBLEM
            AssessmentDefinedParams.EYE_PROBLEM_PTERYGIUM -> AssessmentStatus.PTERYGIUM
            AssessmentDefinedParams.EYE_PROBLEM_GLAUCOMA -> AssessmentStatus.GLAUCOMA
            AssessmentDefinedParams.EYE_PROBLEM_MYOPIA -> AssessmentStatus.MYOPIA
            AssessmentDefinedParams.EYE_PROBLEM_PRESBYOPIA -> AssessmentStatus.PRESBYOPIA
            AssessmentDefinedParams.EYE_PROBLEM_OTHER -> AssessmentStatus.OTHER_EYE_PROBLEM
            AssessmentDefinedParams.EYE_PROBLEM_NONE -> AssessmentStatus.NO_EYE_PROBLEM
            else -> null
        }

    private fun addEyeProblemStatuses(
        statusList: ArrayList<AssessmentStatus>,
        section: Map<*, *>?,
    ) {
        extractEyeProblemIds(section).forEach { id ->
            mapEyeProblemIdToStatus(id)?.let { statusList.add(it) }
        }
    }

    private fun addGlassPowerToken(
        extraTokens: MutableList<String>,
        section: Map<*, *>?,
    ) {
        val power = section?.get(AssessmentDefinedParams.GLASS_POWER)?.toString()?.trim()?.takeIf { it.isNotEmpty() }
        if (power != null) {
            extraTokens.add("${AssessmentDefinedParams.GLASS_POWER_STATUS_PREFIX}$power")
        }
    }

    /** Adds High BP / High BG statuses only when referral reasons include elevated vitals. */
    private fun addHighBpBgStatusesFromReferral(
        statusList: ArrayList<AssessmentStatus>,
        referralReasons: List<String>,
    ) {
        if (referralReasons.contains(bloodPressure)) {
            statusList.add(AssessmentStatus.UNCONTROLLED_BP)
        }
        if (referralReasons.contains(bloodGlucose)) {
            statusList.add(AssessmentStatus.UNCONTROLLED_BG)
        }
    }

    private fun buildCataractStatuses(
        map: HashMap<String, Any>,
        referralResult: Pair<String?, ArrayList<String>>?,
    ): ArrayList<String> {
        val statusList = arrayListOf<AssessmentStatus>()
        val extraTokens = mutableListOf<String>()
        val cataractSection = getCataractFieldSection(map)
        addEyeProblemStatuses(statusList, cataractSection)
        addGlassPowerToken(extraTokens, cataractSection)
        if (isNcdServiceProvidedInCataract(cataractSection)) {
            addHighBpBgStatusesFromReferral(statusList, referralResult?.second ?: listOf())
        }
        if (YES.equals(cataractSection?.get(AssessmentDefinedParams.ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
            statusList.add(AssessmentStatus.GLASSES_SOLD)
        }
        if (YES.equals(cataractSection?.get(AssessmentDefinedParams.NCD_SERVICE_PROVIDED)?.toString(), true)) {
            statusList.add(AssessmentStatus.NCD_SERVICE_IN_CATARACT_CAMP)
        }
        if (YES.equals(cataractSection?.get(AssessmentDefinedParams.PATIENT_REFERRED_FOR_OPERATION)?.toString(), true)) {
            statusList.add(AssessmentStatus.REFERRED_FOR_OPERATION)
        }
        return statusList.toStatusStrings(extraTokens)
    }

    private fun buildEyeCareStatuses(map: HashMap<String, Any>): ArrayList<String> {
        val eyeCareMainMap = map[MenuConstants.EYE_CARE_MENU_ID] as Map<*, *>
        val eyeCareMap = eyeCareMainMap[EYE_CARE] as? Map<*, *>
        val statusList = arrayListOf<AssessmentStatus>()
        val extraTokens = mutableListOf<String>()
        addEyeProblemStatuses(statusList, eyeCareMap)
        addGlassPowerToken(extraTokens, eyeCareMap)
        if (YES.equals(eyeCareMap?.get(ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
            statusList.add(AssessmentStatus.GLASSES_SOLD)
        }
        return statusList.toStatusStrings(extraTokens)
    }

    fun evaluateStatus(
        map: HashMap<String, Any>,
        memberDetails: AssessmentMemberDetails?,
        referralResult: Pair<String?, ArrayList<String>>? = null,
    ): ArrayList<String>? {
        return when {
            map.containsKey(MenuConstants.PREGNANT_WOMEN_PROFILE) -> {
                val riskFactors = PregnantWomen.computeRiskFactors(
                    map[MenuConstants.PREGNANT_WOMEN_PROFILE] as Map<String, Any?>,
                    memberDetails?.dateOfBirth ?: "",
                )
                if (riskFactors.isEmpty()) {
                    arrayListOf(AssessmentStatus.NORMAL_PREGNANCY)
                } else {
                    arrayListOf(AssessmentStatus.HIGH_RISK_PW)
                }.toStatusStrings()
            }

            map.containsKey(RMNCH.ANC) -> {
                val ancMap = map[RMNCH.ANC] as Map<*, *>
                val statusList = arrayListOf<AssessmentStatus>()
                val summaryGroup = ancMap[AssessmentDefinedParams.GROUP_SUMMARY] as? Map<*, *>
                if (summaryGroup?.containsKey(AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN) == true) {
                    statusList.add(AssessmentStatus.HIGH_RISK_PW)
                } else {
                    statusList.add(AssessmentStatus.NORMAL_PREGNANCY)
                }
                if (summaryGroup?.containsKey(AssessmentDefinedParams.GAPS_IN_ANC) == true) {
                    statusList.add(AssessmentStatus.GAPS_IN_ANC)
                }
                statusList.toStatusStrings()
            }

            map.containsKey(RMNCH.PNC) -> {
                val pncMap = map[RMNCH.PNC] as Map<*, *>
                val statusList = arrayListOf<AssessmentStatus>()
                if (pncMap.containsKey(RMNCH.ID_MOTHER_RISKS)) {
                    statusList.add(AssessmentStatus.HIGH_RISK_PNC)
                } else {
                    statusList.add(AssessmentStatus.NORMAL_PNC)
                }
                if (pncMap.containsKey(RMNCH.ID_PNC_GAPS)) {
                    statusList.add(AssessmentStatus.GAPS_IN_PNC)
                }
                statusList.toStatusStrings()
            }

            map.containsKey(MenuConstants.PREGNANCY_OUTCOME) -> {
                val statusList = arrayListOf<AssessmentStatus>()
                val mapData = map[MenuConstants.PREGNANCY_OUTCOME] as Map<*, *>
                if (mapData.containsKey(AssessmentDefinedParams.ID_ABORTION)) {
                    statusList.add(AssessmentStatus.ABORTION)
                } else {
                    val deliveryOutcomes = mapData[AssessmentDefinedParams.ID_DELIVERY_OUTCOMES] as? Map<*, *>
                    deliveryOutcomes?.let {
                        val modeOfDelivery = deliveryOutcomes[AssessmentDefinedParams.ID_MODE_OF_DELIVERY] as? String
                        when (modeOfDelivery) {
                            AssessmentDefinedParams.ModeOfDelivery.NORMAL_DELIVERY.value -> {
                                statusList.add(AssessmentStatus.NORMAL_DELIVERY)
                            }

                            AssessmentDefinedParams.ModeOfDelivery.ASSISTED_DELIVERY.value -> {
                                statusList.add(AssessmentStatus.ASSISTED_DELIVERY)
                            }

                            AssessmentDefinedParams.ModeOfDelivery.CESAREAN_SECTION.value -> {
                                statusList.add(AssessmentStatus.C_SECTION)
                            }
                        }
                        val liveBirthNumbers = CommonUtils.getDouble(deliveryOutcomes[AssessmentDefinedParams.ID_LIVE_BIRTH_NUMBERS])
                        if (liveBirthNumbers > 0) {
                            val newbornDetailsList = AssessmentUtil.findNewbornDetailsFromMap(map)
                            val isAnyBabyDead = newbornDetailsList?.any { babyData ->
                                babyData is Map<*, *> && !(DefinedParams.YES_SMALL.equals(babyData[AssessmentDefinedParams.IS_BABY_ALIVE]?.toString(), true))
                            }
                            if (isAnyBabyDead == true) {
                                statusList.add(AssessmentStatus.NEONATAL_DEATH)
                            }
                        }
                        val stillBirthNumbers = CommonUtils.getDouble(deliveryOutcomes[AssessmentDefinedParams.ID_STILL_BIRTH_NUMBERS])
                        if (stillBirthNumbers > 0) {
                            statusList.add(AssessmentStatus.STILL_BIRTH)
                        }
                        if (liveBirthNumbers > 0) {
                            statusList.add(AssessmentStatus.LIVE_BIRTH)
                        }
                    }
                }
                statusList.toStatusStrings()
            }

            map.containsKey(MenuConstants.FP_MENU_ID) -> {
                val fpMap = map[MenuConstants.FP_MENU_ID.lowercase()] as Map<*, *>
                val assessmentMap = fpMap[AssessmentDefinedParams.FamilyPlanningDetails] as Map<*, *>
                val familyPlanningMethod = assessmentMap[AssessmentDefinedParams.FamilyPlanningMethods] as? List<*>
                if (familyPlanningMethod.isNullOrEmpty() || familyPlanningMethod.first() == DefinedParams.None) {
                    arrayListOf(AssessmentStatus.NOT_USING_MODERN_FP)
                } else {
                    arrayListOf(AssessmentStatus.USING_MODERN_FP)
                }.toStatusStrings()
            }

            map.containsKey(MenuConstants.NCD_MENU_ID) -> {
                val statusList = arrayListOf<AssessmentStatus>()
                addHighBpBgStatusesFromReferral(statusList, referralResult?.second ?: listOf())
                val ncdMap = map[MenuConstants.NCD_MENU_ID] as Map<*, *>
                val eyeCareMap = ncdMap[EYE_CARE] as? Map<*, *>
                if (YES.equals(eyeCareMap?.get(ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
                    statusList.add(AssessmentStatus.GLASSES_SOLD)
                }
                statusList.toStatusStrings()
            }

            map.containsKey(MenuConstants.CATARACT_MENU_ID) -> buildCataractStatuses(map, referralResult)

            map.containsKey(MenuConstants.EYE_CARE_MENU_ID) -> buildEyeCareStatuses(map)

            else -> {
                null
            }
        }
    }
}
