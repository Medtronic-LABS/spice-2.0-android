package org.medtroniclabs.uhis.ui.assessment.referrallogic

import android.content.Context
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DefinedParams.CBS_Referral
import org.medtroniclabs.uhis.common.DefinedParams.Referred_NCD
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.NO_SYMPTOMS
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.ncd.screening.utils.ReferredReason
import org.medtroniclabs.uhis.ui.MenuConstants.NCD_MENU_ID
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.AlcoholConsumption
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DiagnosedWithDiabetes
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.Dispensed
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.DryMouthOrTongue
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_COMMUNITY_CLINIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FACILITY_TYPE_UPAZILA
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FBS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FBS_MAXIMUM_MGDL_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FBS_MAXIMUM_VALUE_BD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FOLLOW_UP_FBS_MAXIMUM_MGDL_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FOLLOW_UP_FBS_MAXIMUM_VALUE_BD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FOLLOW_UP_RBS_MAXIMUM_MGDL_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FOLLOW_UP_RBS_MAXIMUM_VALUE_BD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MAX_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_BREATHING
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.FB_MIN_MONTH
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasCoughLastedLonger
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasNightSweatsTB
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.HasWeightLoss
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.LittleOrNoUrine
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MGDL
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NoTearsWhenCrying
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RBS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RBS_MAXIMUM_MGDL_VALUE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RBS_MAXIMUM_VALUE_BD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.REFERRAL_FACILITY_TYPE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RegularSmoker
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.RelationshipToIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SkinPinch
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SleepLocation
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SunkenEyes
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.SunkenFontanella
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UPAZILA_FBS_RBS_MAXIMUM_VALUE_BD
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UPAZILA_UPPER_LIMIT_DIASTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UPAZILA_UPPER_LIMIT_SYSTOLIC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UpperLimitDiastolic
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.UpperLimitSystolic
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.VeryThirsty
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.otherSymptoms
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.symptomsDTO
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.ACT
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Amoxicillin
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.BreathPerMinute
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.ChestInDrawing
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Diarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.DiarrhoeaSigns
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.HasFever
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.IsBloodyDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.MaxDaysCough
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.MaxDaysOfDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.MaxDaysOfFever
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.MaxTemperature
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.NA
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.NoOfDaysOfCough
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.NoOfDaysOfDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.NoOfDaysOfFever
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.OrsDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtNegative
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtPositive
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.RdtTest
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Red
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Temperature
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Yellow
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.Yes
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.ZincDispensedStatus
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.hasCough
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.hasDiarrhoea
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.hasOedemaOfBothFeet
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.isBreastfeed
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.isConvulsionPastFewDays
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.isUnusualSleepy
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.isVomiting
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams.muacCode
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralReasons
import org.medtroniclabs.uhis.ui.assessment.referrallogic.utils.ReferralStatus
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.common.DefinedParams as CommonDefinedParams

class ReferralResultGenerator {
    private var patientStatus = HashMap<String, Any>()
    private val referralReason = arrayListOf<String>()

    fun calculateIccmReferralResult(
        map: HashMap<String, Any>,
        memberDetails: AssessmentMemberDetails?,
    ): Pair<String?, ArrayList<String>> {
        calculateDangerSignsResult(map)
        calculateNutritionalStatusResult(map)
        calculateCoughStatus(map, memberDetails)
        calculateFeverStatus(map, ReferralReasons.Fever.name)
        calculateDiarrhoeaStatus(map)
        return Pair(checkStatus(), referralReason)
    }

    fun calculateRMNCHReferralResult(map: HashMap<String, Any>): Pair<String?, ArrayList<String>> {
        if (map.containsKey(RMNCH.ANC)) {
            // Check for highRiskPregnantWoman or gapsInAnc in summary group
            val ancMap = map[RMNCH.ANC] as? Map<*, *>
            val summaryGroup = ancMap?.get(AssessmentDefinedParams.GROUP_SUMMARY) as? Map<*, *>

            // If highRiskPregnantWoman has values, add referral reason
            if (summaryGroup?.containsKey(AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN) == true) {
                addResultMap(
                    AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN,
                    ReferralStatus.Referred.name,
                )
                addReferralReason(
                    AssessmentDefinedParams.LABEL_HIGH_RISK_PREGNANT_WOMAN,
                )
            }

            // If gapsInAnc has values, add referral reason
            if (summaryGroup?.containsKey(AssessmentDefinedParams.GAPS_IN_ANC) == true) {
                addResultMap(
                    AssessmentDefinedParams.GAPS_IN_ANC,
                    ReferralStatus.Referred.name,
                )
                addReferralReason(
                    AssessmentDefinedParams.LABEL_GAPS_IN_ANC,
                )
            }

            updateVisitCount(map, RMNCH.ANC)
        } else if (map.containsKey(RMNCH.ChildHoodVisit)) {
            val childVisitMap = map[RMNCH.ChildHoodVisit] as Map<String, Any>
            if (CommonDefinedParams.YES_SMALL
                    .equals(childVisitMap[AssessmentDefinedParams.ID_CHILD_REFERRAL] as? String, true)
            ) {
                addResultMap(ReferralReasons.aliasOf(ReferralReasons.childhoodVisitSigns), ReferralStatus.Referred.name)
                addReferralReason(ReferralReasons.aliasOf(ReferralReasons.childhoodVisitSigns))
            }
            updateVisitCount(map, RMNCH.ChildHoodVisit)
        } else {
            val pncMap = map[RMNCH.PNC] as Map<String, Any>
            if (pncMap.containsKey(RMNCH.ID_MOTHER_RISKS)) {
                addResultMap(
                    RMNCH.ID_MOTHER_RISKS,
                    ReferralStatus.Referred.name,
                )
                addReferralReason(AssessmentDefinedParams.LABEL_HIGH_RISK_MOTHER)
            }

            if (pncMap.containsKey(RMNCH.ID_PNC_GAPS)) {
                addResultMap(
                    RMNCH.ID_PNC_GAPS,
                    ReferralStatus.Referred.name,
                )
                addReferralReason(AssessmentDefinedParams.LABEL_GAPS_IN_PNC)
            }

            updateVisitCount(map, RMNCH.PNC)
        }
        return Pair(checkStatus(), referralReason)
    }

    private fun updateVisitCount(
        map: HashMap<String, Any>,
        workFlow: String,
    ) {
        var visitInfo = ""
        val workflowMap = map[workFlow]
        if (workflowMap is Map<*, *> && workflowMap.containsKey(RMNCH.visitNo)) {
            val visitNo = workflowMap[RMNCH.visitNo]
            visitInfo = "${getVisitLabel(workFlow)} $visitNo"
        }

        while (referralReason.isNotEmpty() && referralReason[0].isEmpty()) {
            referralReason.removeAt(0)
        }

        if (referralReason.isEmpty()) {
            referralReason.add(visitInfo)
            return
        }

        for (i in referralReason.size - 1 downTo 0) {
            if (referralReason[i].isNotEmpty()) {
                referralReason[i] = "${referralReason[i]} - $visitInfo"
                return
            }
        }

        referralReason.add(visitInfo)
    }

    private fun getVisitLabel(workFlow: String): String {
        when (workFlow) {
            RMNCH.ANC -> {
                return RMNCH.ANC_VISIT_NO
            }

            RMNCH.ChildHoodVisit -> {
                return RMNCH.CHILDHOOD_VISIT_NO
            }

            RMNCH.PNC -> {
                return RMNCH.PNC_VISIT_NO
            }
        }

        return "Visit No: "
    }

    fun calculateOtherSymptomsReferralResult(map: HashMap<String, Any>): Pair<String?, ArrayList<String>> {
        calculateSymptomsStatus(map, otherSymptoms, ReferralReasons.Symptoms.name, ReferralReasons.Symptoms.name.lowercase())
        calculateFeverStatus(map, ReferralReasons.Fever.name)
        return Pair(checkStatus(), referralReason)
    }

    fun calculateTBReferralResult(map: HashMap<String, Any>): Pair<String?, ArrayList<String>> {
        val hasCough = map[hasCough] is Boolean && map[hasCough] == true
        val hasCoughLastLonger = map.containsKey(HasCoughLastedLonger) && map[HasCoughLastedLonger] is Boolean && map[HasCoughLastedLonger] == true
        val isContactTracing = map.containsKey(RelationshipToIC) && map[RelationshipToIC] is String && map[RelationshipToIC] != ""
        if (hasCough && hasCoughLastLonger || isContactTracing) {
            if ((map[HasNightSweatsTB] is Boolean && map[HasNightSweatsTB] == true) ||
                (map[HasFever] is Boolean && map[HasFever] == true) ||
                (map[HasWeightLoss] is Boolean && map[HasWeightLoss] == true) ||
                (map[RelationshipToIC] is String && map[RelationshipToIC] != "") ||
                (map[SleepLocation] is String && map[SleepLocation] != "")
            ) {
                addResultMap("TB Symptoms", ReferralStatus.Referred.name)
                addReferralReason("TB Symptoms")
            }
        }
        return Pair(checkStatus(), referralReason)
    }

    fun calculateCBSReferralResult(map: HashMap<String, Any>): Pair<String?, ArrayList<String>> {
        addResultMap(CBS_Referral, ReferralStatus.Referred.name)
        addReferralReason(CBS_Referral)
        return Pair(checkStatus(), referralReason)
    }

    private fun addResultMap(
        key: String,
        value: String?,
    ) {
        if (value is String) {
            patientStatus[key] = value
        }
    }

    /**
     * Referral Logic for ICCM General Danger Signs
     * If any of the signs is true, then the patient status is referred
     */
    private fun calculateDangerSignsResult(map: HashMap<String, Any>) {
        val generalDangerSignsList =
            listOf(isUnusualSleepy, isConvulsionPastFewDays, isVomiting, isBreastfeed)
        for (key in generalDangerSignsList) {
            if (map.containsKey(key) && ((map[key] is Boolean && map[key] == true) || map[key] is String && map[key] == Yes)) {
                addResultMap(
                    ReferralReasons.GeneralDangerSigns.name.lowercase(),
                    ReferralStatus.Referred.name,
                )
                addReferralReason(
                    ReferralReasons.aliasOf(ReferralReasons.GeneralDangerSigns),
                )
                break
            }
        }
    }

    /**
     * Referral Logic for ICCM Nutritional Status
     * If nutritional code is red or yellow, then patientStatus is referred
     * If nutritional code is green and odema of both feet is true or Yes, then patientStatus is referred
     */
    private fun calculateNutritionalStatusResult(map: HashMap<String, Any>) {
        if (map.containsKey(muacCode)) {
            val muacCodeValue = map[muacCode]
            if (muacCodeValue is String && (muacCodeValue.lowercase() == Red.lowercase() || muacCodeValue.lowercase() == Yellow.lowercase())) {
                addResultMap(ReferralReasons.MUAC.name.lowercase(), ReferralStatus.Referred.name)
                addReferralReason(ReferralReasons.MUAC.name)
            }
        }
        if (map.containsKey(hasOedemaOfBothFeet) &&
            (
                (map[hasOedemaOfBothFeet] is String && map[hasOedemaOfBothFeet] == Yes) ||
                    (map[hasOedemaOfBothFeet] is Boolean && map[hasOedemaOfBothFeet] == true)
            )
        ) {
            addResultMap(ReferralReasons.MUAC.name.lowercase(), ReferralStatus.Referred.name)
            addReferralReason(ReferralReasons.MUAC.name)
        }
    }

    /**
     * Referral Logic for ICCM Cough
     * If no of days is >= 21, then patientStatus is referred
     * If no of days is < 21,
     *  1. If chest indrawing is yes, then patientStatus is referred
     *  2. If patient has Fast breathing & Amoxicillin is NA, then patientStatus is referred
     *  3. If patient has Fast breathing & Amoxicillin is Dispensed, then patientStatus is on-treatment
     */
    private fun calculateCoughStatus(
        map: HashMap<String, Any>,
        memberDetails: AssessmentMemberDetails?,
    ) {
        if (map.containsKey(hasCough)) {
            if ((map[hasCough] is String && map[hasCough] == Yes) || (map[hasCough] is Boolean && map[hasCough] == true)) {
                // Condition for fast breathing
                if (map.containsKey(BreathPerMinute) && map[BreathPerMinute] is Int) {
                    val bpmValue = map[BreathPerMinute] as Int
                    memberDetails?.let { details ->
                        DateUtils.dateToMonths(details.dateOfBirth).let { month ->
                            month?.let {
                                if ((month in FB_MIN_MONTH..11) && bpmValue >= FB_MAX_BREATHING) {
                                    addResultMap(
                                        ReferralReasons.Pneumonia.name.lowercase(),
                                        getMedicationStatus(map, Amoxicillin),
                                    )
                                    addReferralReason(
                                        ReferralReasons.Pneumonia.name,
                                    )
                                    return
                                } else if (month in FB_MAX_MONTH..60 && bpmValue >= FB_MIN_BREATHING) {
                                    addResultMap(
                                        ReferralReasons.Pneumonia.name.lowercase(),
                                        getMedicationStatus(map, Amoxicillin),
                                    )
                                    addReferralReason(
                                        ReferralReasons.Pneumonia.name,
                                    )
                                    return
                                }
                            }
                        }
                    }
                }

                // Condition for No of days cough
                if (map.containsKey(NoOfDaysOfCough) && map[NoOfDaysOfCough] is Int) {
                    val noOfDays = map[NoOfDaysOfCough] as Int
                    if (noOfDays >= MaxDaysCough) {
                        addResultMap(
                            ReferralReasons.Pneumonia.name.lowercase(),
                            ReferralStatus.Referred.name,
                        )
                        addReferralReason(ReferralReasons.Cough.name)
                        return
                    }
                }

                // Condition for Chest In Drawing
                if (map.containsKey(ChestInDrawing) &&
                    ((map[ChestInDrawing] is String && map[ChestInDrawing] == Yes) || (map[ChestInDrawing] is Boolean && map[ChestInDrawing] == true))
                ) {
                    addResultMap(
                        ReferralReasons.Pneumonia.name.lowercase(),
                        ReferralStatus.Referred.name,
                    )
                    addReferralReason(ReferralReasons.Cough.name)
                    return
                }
            }
        }
    }

    /**
     * Referral Logic for ICCM Fever
     * If fever is yes no of days is >= 7, then patientStatus is referred
     * If no of days is < 7,
     *  1. If RDT test is -Ve or NA, then patientStatus is referred
     *  2. If RDT test is positive & ACT is NA, then patientStatus is referred
     *  3. If RDT test is positive & ACT is Dispensed, then patientStatus is on-treatment
     * else If temperature is >=37.5, then patientStatus is referred
     */
    private fun calculateFeverStatus(
        map: HashMap<String, Any>,
        referralKey: String,
    ) {
        if (map.containsKey(HasFever)) {
            if ((map[HasFever] is String && map[HasFever] == Yes) || (map[HasFever] is Boolean && map[HasFever] == true)) {
                if (map.containsKey(NoOfDaysOfFever) && map[NoOfDaysOfFever] is Int) {
                    val noOfDays = map[NoOfDaysOfFever] as Int
                    if (noOfDays >= MaxDaysOfFever) {
                        addResultMap(referralKey.lowercase(), ReferralStatus.Referred.name)
                        rdtReferralStatus(map, referralKey)
                    } else if (map.containsKey(Temperature) && map[Temperature] is Double && (map[Temperature] as Double) >= MaxTemperature) {
                        addResultMap(referralKey.lowercase(), ReferralStatus.Referred.name)
                        rdtReferralStatus(map, referralKey)
                    } else {
                        if (map.containsKey(RdtTest) && map[RdtTest] == RdtPositive) {
                            addResultMap(referralKey.lowercase(), getMedicationStatus(map, ACT))
                            addReferralReason(ReferralReasons.Malaria.name)
                        } else if (map.containsKey(RdtTest) && (map[RdtTest] == RdtNegative || map[RdtTest] == NA)) {
                            addResultMap(referralKey.lowercase(), ReferralStatus.Referred.name)
                            addReferralReason(referralKey)
                        }
                    }
                }
            }
        }
    }

    fun rdtReferralStatus(
        map: HashMap<String, Any>,
        referralKey: String,
    ) {
        if (map.containsKey(RdtTest) && map[RdtTest] == RdtPositive) {
            addReferralReason(ReferralReasons.Malaria.name)
        } else {
            addReferralReason(referralKey)
        }
    }

    /**
     * Referral Logic for ICCM Diarrhoea
     * If Bloody diarrhoea is yes, then patientStatus is referred
     * If no of days is >= 14, then patientStatus is referred
     * If no of days is < 14,
     *  1. If Any one of the respective dehydration signs is present, then patientStatus is referred
     *  2. If ORS or Zinc is NA, then patientStatus is referred
     *  3. If ORS and Zinc is Dispensed, then patientStatus is On Treatment
     */
    private fun calculateDiarrhoeaStatus(map: HashMap<String, Any>) {
        if (map.containsKey(hasDiarrhoea)) {
            if ((map[hasDiarrhoea] is String && map[hasDiarrhoea] == Yes) || (map[hasDiarrhoea] is Boolean && map[hasDiarrhoea] == true)) {
                if (map.containsKey(IsBloodyDiarrhoea) &&
                    (
                        (map[IsBloodyDiarrhoea] is String && map[IsBloodyDiarrhoea] == Yes) ||
                            (map[IsBloodyDiarrhoea] is Boolean && map[IsBloodyDiarrhoea] == true)
                    )
                ) {
                    addResultMap(
                        ReferralReasons.Diarrhoea.name.lowercase(),
                        ReferralStatus.Referred.name,
                    )
                    addReferralReason(ReferralReasons.Diarrhoea.name)
                } else if (map.containsKey(NoOfDaysOfDiarrhoea) && map[NoOfDaysOfDiarrhoea] is Int) {
                    val noOfDays = map[NoOfDaysOfDiarrhoea] as Int
                    if (noOfDays >= MaxDaysOfDiarrhoea) {
                        addResultMap(
                            ReferralReasons.Diarrhoea.name.lowercase(),
                            ReferralStatus.Referred.name,
                        )
                        addReferralReason(ReferralReasons.Diarrhoea.name)
                    } else {
                        if ((map.containsKey(DiarrhoeaSigns) && map[DiarrhoeaSigns] is ArrayList<*>) &&
                            (
                                getDiarrhoeaSignsStatus(
                                    map[DiarrhoeaSigns],
                                ) != null
                            )
                        ) {
                            addResultMap(
                                ReferralReasons.Diarrhoea.name.lowercase(),
                                getDiarrhoeaSignsStatus(map[DiarrhoeaSigns]),
                            )
                            if (patientStatus.containsKey(Diarrhoea) && patientStatus[Diarrhoea] == ReferralStatus.Referred.name) {
                                addReferralReason(
                                    ReferralReasons.Diarrhoea.name,
                                )
                            }
                        } else if ((map.containsKey(OrsDispensedStatus) && map[OrsDispensedStatus] == NA) ||
                            (
                                map.containsKey(
                                    ZincDispensedStatus,
                                ) &&
                                    map[ZincDispensedStatus] == NA
                            )
                        ) {
                            addResultMap(
                                ReferralReasons.Diarrhoea.name.lowercase(),
                                ReferralStatus.Referred.name,
                            )
                            addReferralReason(ReferralReasons.Diarrhoea.name)
                        } else if ((map.containsKey(OrsDispensedStatus) && map[OrsDispensedStatus] == Dispensed) &&
                            (
                                map.containsKey(
                                    ZincDispensedStatus,
                                ) &&
                                    map[ZincDispensedStatus] == Dispensed
                            )
                        ) {
                            addResultMap(
                                ReferralReasons.Diarrhoea.name.lowercase(),
                                ReferralStatus.OnTreatment.name,
                            )
                            addReferralReason(ReferralReasons.Diarrhoea.name)
                        }
                    }
                }
            }
        }
    }

    private fun getDiarrhoeaSignsStatus(value: Any?): String? {
        var status: String? = null
        val signsList = listOf(
            SunkenEyes.lowercase(),
            NoTearsWhenCrying.lowercase(),
            LittleOrNoUrine.lowercase(),
            SkinPinch.lowercase(),
            VeryThirsty.lowercase(),
            DryMouthOrTongue.lowercase(),
            SunkenFontanella.lowercase(),
        )
        val selectedSignsList = ArrayList<String>()
        if (value is ArrayList<*>) {
            value.forEach { map ->
                CommonUtils.getListActual(map)?.let {
                    selectedSignsList.add(it.lowercase())
                }
            }
        }
        for (item in signsList) {
            if (selectedSignsList.contains(item)) {
                status = ReferralStatus.Referred.name
                break
            }
        }
        return status
    }

    private fun addReferralReason(key: String) {
        if (!referralReason.contains(key)) referralReason.add(key)
    }

    /**
     * This method is to determine the medication status, according to ICCM
     * 1. If Status is NA, then status is under referred
     * 2. If Status is Dispensed, then status is under On Treatment
     * 3. Else it will be null
     */
    private fun getMedicationStatus(
        map: HashMap<String, Any>,
        key: String,
    ): String? {
        val status = if (map.containsKey(key) && map[key] is String) {
            if (map[key] == NA) {
                ReferralStatus.Referred.name
            } else if (map[key] == Dispensed) {
                ReferralStatus.OnTreatment.name
            } else {
                null
            }
        } else {
            null
        }
        return status
    }

    private fun calculateSymptomsStatus(
        map: HashMap<String, Any>,
        symptomType: String,
        referedReason: String,
        referralKey: String,
    ) {
        val selectedSignsList = ArrayList<String>()
        if (map.containsKey(symptomType) && map[symptomType] is ArrayList<*>) {
            (map[symptomType] as ArrayList<*>).forEach { result ->
                CommonUtils.getListActual(result)?.let {
                    selectedSignsList.add(it.lowercase())
                }
            }
        }
        if (!selectedSignsList.contains(NO_SYMPTOMS.lowercase())) {
            addResultMap(referralKey, ReferralStatus.Referred.name)
            addReferralReason(referedReason)
        }
    }

    private fun checkStatus(): String? {
        if (patientStatus.containsValue(ReferralStatus.Referred.name)) {
            return ReferralStatus.Referred.name
        } else if (patientStatus.containsValue(ReferralStatus.OnTreatment.name)) {
            return ReferralStatus.OnTreatment.name
        }
        return null
    }

    fun calculatePregnancyOutcomeStatus(map: HashMap<String, Any>): Pair<String?, ArrayList<String>> {
        addResultMap("Family Planning", ReferralStatus.Recovered.name)
        addReferralReason("Family Planning Consult")
        return Pair(checkStatus(), referralReason)
    }

    fun calculateNCDStatus(
        context: Context,
        map: HashMap<String, Any>,
    ): Pair<String?, ArrayList<String>> {
        val diagnosedWithDiabetes =
            map[DiagnosedWithDiabetes] is Boolean && map[DiagnosedWithDiabetes] == true
        val regularSmoker = map[RegularSmoker] is Boolean && map[RegularSmoker] == true
        val alcoholConsumption =
            map[AlcoholConsumption] is Boolean && map[AlcoholConsumption] == true
        val bmiReferral = getBmiReferral(CommonUtils.getBMIInformation(context, map[Screening.BMI] as? Double), context)
        calculateSymptomsStatus(map, symptomsDTO, Referred_NCD, NCD_MENU_ID)
        if ((!patientStatus.containsKey(NCD_MENU_ID)) && (diagnosedWithDiabetes || regularSmoker || alcoholConsumption || bmiReferral)) {
            addResultMap(NCD_MENU_ID, ReferralStatus.Referred.name)
            addReferralReason(Referred_NCD)
        }
        return Pair(checkStatus(), referralReason)
    }

    private fun getBmiReferral(
        bmiInformation: Pair<String, Int>?,
        context: Context,
    ): Boolean {
        return if (bmiInformation?.first.equals(
                context.getString(R.string.under_weight),
                true,
            ) ||
            bmiInformation?.first.equals(
                context.getString(R.string.over_weight),
                true,
            ) ||
            bmiInformation?.first.equals(
                context.getString(R.string.obese),
                true,
            ) ||
            bmiInformation?.first.equals(context.getString(R.string.extremely_obese), true)
        ) {
            return true
        } else {
            false
        }
    }

    fun computeReferralResultForBDNCD(
        map: HashMap<String, Any>,
        bpResult: Pair<Int, Int>,
        bgResult: Triple<String?, String?, Double?>,
        symptomList: List<String>,
        isFollowUpVisit: Boolean = false,
    ): Pair<String, ArrayList<String>>? {
        val referredReasonList = ArrayList<String>()

        if (symptomList.isNotEmpty()) {
            referredReasonList.add(ReferredReason.SYMPTOMS)
        }

        if (bpResult.first >= UpperLimitSystolic ||
            bpResult.second >= UpperLimitDiastolic
        ) {
            referredReasonList.add(ReferredReason.bloodPressure)
        }

        if (isBloodGlucoseReferralRequired(bgResult, isFollowUpVisit)) {
            referredReasonList.add(ReferredReason.bloodGlucose)
        }

        if (referredReasonList.isNotEmpty()) {
            map[REFERRAL_FACILITY_TYPE] = resolveReferralFacilityType(
                referredReasonList = referredReasonList,
                bpResult = bpResult,
                bgResult = bgResult,
                isFollowUpVisit = isFollowUpVisit,
            )
        }

        return if (referredReasonList.isNotEmpty()) {
            Pair(ReferralStatus.Referred.name, referredReasonList)
        } else {
            null
        }
    }

    private fun resolveReferralFacilityType(
        referredReasonList: ArrayList<String>,
        bpResult: Pair<Int, Int>,
        bgResult: Triple<String?, String?, Double?>,
        isFollowUpVisit: Boolean,
    ): String {
        if (isFollowUpVisit || CommonUtils.isFoOrPo()) {
            return FACILITY_TYPE_UPAZILA
        }
        val hasBpOrBgReason = ReferredReason.bloodPressure in referredReasonList ||
            ReferredReason.bloodGlucose in referredReasonList
        val shouldReferUpazila = hasBpOrBgReason &&
            (
                isBPReferredForUpazila(bpResult.first, bpResult.second) ||
                    isBGReferredForUpazila(bgResult.third)
            )
        return if (shouldReferUpazila) {
            FACILITY_TYPE_UPAZILA
        } else {
            FACILITY_TYPE_COMMUNITY_CLINIC
        }
    }

    private fun isBloodGlucoseReferralRequired(
        bgResult: Triple<String?, String?, Double?>,
        isFollowUpVisit: Boolean,
    ): Boolean {
        val unit = bgResult.first ?: return false
        val glucoseType = bgResult.second
        val bgValue = bgResult.third ?: return false

        if (unit == MGDL) {
            return if (isFollowUpVisit) {
                isGlucoseAtOrAboveThreshold(
                    glucoseType,
                    bgValue,
                    FOLLOW_UP_FBS_MAXIMUM_MGDL_VALUE.toDouble(),
                    FOLLOW_UP_RBS_MAXIMUM_MGDL_VALUE.toDouble(),
                )
            } else {
                isGlucoseAboveThreshold(
                    glucoseType,
                    bgValue,
                    FBS_MAXIMUM_MGDL_VALUE.toDouble(),
                    RBS_MAXIMUM_MGDL_VALUE.toDouble(),
                )
            }
        }

        return if (isFollowUpVisit) {
            isGlucoseAtOrAboveThreshold(
                glucoseType,
                bgValue,
                FOLLOW_UP_FBS_MAXIMUM_VALUE_BD,
                FOLLOW_UP_RBS_MAXIMUM_VALUE_BD,
            )
        } else {
            isGlucoseAboveThreshold(
                glucoseType,
                bgValue,
                FBS_MAXIMUM_VALUE_BD,
                RBS_MAXIMUM_VALUE_BD,
            )
        }
    }

    private fun isGlucoseAtOrAboveThreshold(
        glucoseType: String?,
        bgValue: Double,
        fbsThreshold: Double,
        rbsThreshold: Double,
    ): Boolean =
        when (glucoseType?.lowercase()) {
            FBS -> bgValue >= fbsThreshold
            RBS -> bgValue >= rbsThreshold
            else -> bgValue >= fbsThreshold || bgValue >= rbsThreshold
        }

    private fun isGlucoseAboveThreshold(
        glucoseType: String?,
        bgValue: Double,
        fbsThreshold: Double,
        rbsThreshold: Double,
    ): Boolean =
        when (glucoseType?.lowercase()) {
            FBS -> bgValue > fbsThreshold
            RBS -> bgValue > rbsThreshold
            else -> bgValue > fbsThreshold || bgValue > rbsThreshold
        }

    private fun isBPReferredForUpazila(
        avgSys: Int,
        avgDia: Int,
    ): Boolean =
        avgSys >= UPAZILA_UPPER_LIMIT_SYSTOLIC ||
            avgDia >= UPAZILA_UPPER_LIMIT_DIASTOLIC

    private fun isBGReferredForUpazila(bg: Double?): Boolean = bg != null && bg >= UPAZILA_FBS_RBS_MAXIMUM_VALUE_BD
}
