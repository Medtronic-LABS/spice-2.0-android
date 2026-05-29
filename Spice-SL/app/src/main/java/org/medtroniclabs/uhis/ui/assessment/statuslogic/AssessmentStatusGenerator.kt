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

    fun evaluateStatus(
        map: HashMap<String, Any>,
        memberDetails: AssessmentMemberDetails?,
        referralResult: Pair<String?, ArrayList<String>>? = null,
    ): ArrayList<String>? {
        val statusList = when {
            map.containsKey(MenuConstants.PREGNANT_WOMEN_PROFILE) -> {
                val riskFactors = PregnantWomen.computeRiskFactors(
                    map[MenuConstants.PREGNANT_WOMEN_PROFILE] as Map<String, Any?>,
                    memberDetails?.dateOfBirth ?: "",
                )
                if (riskFactors.isEmpty()) {
                    arrayListOf(AssessmentStatus.NORMAL_PREGNANCY)
                } else {
                    arrayListOf(AssessmentStatus.HIGH_RISK_PW)
                }
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
                statusList
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
                statusList
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
                statusList
            }

            map.containsKey(MenuConstants.FP_MENU_ID) -> {
                val fpMap = map[MenuConstants.FP_MENU_ID.lowercase()] as Map<*, *>
                val assessmentMap = fpMap[AssessmentDefinedParams.FamilyPlanningDetails] as Map<*, *>
                val familyPlanningMethod = assessmentMap[AssessmentDefinedParams.FamilyPlanningMethods] as? List<*>
                if (familyPlanningMethod.isNullOrEmpty() || familyPlanningMethod.first() == DefinedParams.None) {
                    arrayListOf(AssessmentStatus.NOT_USING_MODERN_FP)
                } else {
                    arrayListOf(AssessmentStatus.USING_MODERN_FP)
                }
            }

            map.containsKey(MenuConstants.NCD_MENU_ID) -> {
                val results = referralResult?.second ?: listOf()
                val statusList = arrayListOf<AssessmentStatus>()
                if (results.isNotEmpty()) {
                    if (results.contains(bloodPressure) && results.contains(bloodGlucose)) {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BP)
                        statusList.add(AssessmentStatus.UNCONTROLLED_BG)
                    } else if (results.contains(bloodPressure)) {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BP)
                    } else {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BG)
                    }
                } else {
                    statusList.add(AssessmentStatus.CONTROLLED_BP)
                    statusList.add(AssessmentStatus.CONTROLLED_BG)
                }
                val ncdMap = map[MenuConstants.NCD_MENU_ID] as Map<*, *>
                val eyeCareMap = ncdMap[EYE_CARE] as? Map<*, *>
                if (YES.equals(eyeCareMap?.get(ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
                    statusList.add(AssessmentStatus.GLASSES_SOLD)
                }
                statusList
            }

            map.containsKey(MenuConstants.CATARACT_MENU_ID) -> {
                val results = referralResult?.second ?: listOf()
                val statusList = arrayListOf<AssessmentStatus>()
                if (results.isNotEmpty()) {
                    if (results.contains(bloodPressure) && results.contains(bloodGlucose)) {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BP)
                        statusList.add(AssessmentStatus.UNCONTROLLED_BG)
                    } else if (results.contains(bloodPressure)) {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BP)
                    } else {
                        statusList.add(AssessmentStatus.UNCONTROLLED_BG)
                    }
                } else {
                    statusList.add(AssessmentStatus.CONTROLLED_BP)
                    statusList.add(AssessmentStatus.CONTROLLED_BG)
                }
                val cataractSection = getCataractFieldSection(map)
                if (YES.equals(cataractSection?.get(AssessmentDefinedParams.ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
                    statusList.add(AssessmentStatus.GLASSES_SOLD)
                }
                if (YES.equals(cataractSection?.get(AssessmentDefinedParams.NCD_SERVICE_PROVIDED)?.toString(), true)) {
                    statusList.add(AssessmentStatus.NCD_SERVICE_IN_CATARACT_CAMP)
                }
                if (YES.equals(cataractSection?.get(AssessmentDefinedParams.PATIENT_REFERRED_FOR_OPERATION)?.toString(), true)) {
                    statusList.add(AssessmentStatus.REFERRED_FOR_OPERATION)
                }
                statusList
            }

            map.containsKey(MenuConstants.EYE_CARE_MENU_ID) -> {
                val map = map[MenuConstants.EYE_CARE_MENU_ID] as Map<*, *>
                val eyeCareMap = map[EYE_CARE] as? Map<*, *>
                val statusList = arrayListOf<AssessmentStatus>()
                if (YES.equals(eyeCareMap?.get(ID_HAVE_THE_GLASSES_BEEN_SOLD)?.toString(), true)) {
                    statusList.add(AssessmentStatus.GLASSES_SOLD)
                }
                statusList
            }

            else -> {
                null
            }
        }?.map { it.name }
        return statusList?.let {
            ArrayList(it)
        }
    }
}
