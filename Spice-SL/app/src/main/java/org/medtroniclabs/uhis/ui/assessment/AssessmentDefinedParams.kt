package org.medtroniclabs.uhis.ui.assessment

import org.medtroniclabs.uhis.common.DefinedParams

object AssessmentDefinedParams {
    const val isUnusualSleepy = "isUnusualSleepy"
    const val isConvulsionPastFewDays = "isConvulsionPastFewDays"
    const val isVomiting = "isVomiting"
    const val isBreastfeed = "isBreastfeed"
    const val MUAC = "muac"
    const val muacCode = "muacCode"
    const val hasCough = "hasCough"
    const val hasFever = "hasFever"
    const val hasDiarrhoea = "hasDiarrhoea"
    const val rdtTest = "rdtTest"
    const val Dispensed = "Dispensed"
    const val Amoxicillin = "Amoxicillin"
    const val ACT = "ACT"
    const val IsClinicTaken = "isTakenToClinic"
    const val AssessmentNotes = "notes"
    const val NextFollowupDate = "nextVisitDate"
    const val ReferredPHUSite = "referredSite"
    const val ReferredPHUSiteID = "referredSiteId"
    const val ReferralFacilityType = "referralFacilityType"
    const val Green = "Green"
    const val Red = "Red"
    const val Yellow = "Yellow"
    const val HasNightSweats = "has_night_sweats"
    const val CoughTBSummary = "Cough (>2 Weeks)"
    const val DrenchingNightSweats = "Drenching Night Sweats"
    const val Summary = "summary"
    const val OtherSymptoms = "OTHER SYMPTOMS"
    const val otherConcerningSymptoms = "otherConcerningSymptoms"
    const val muacStatus = "muacStatus"
    const val AmoxicillinStatus = "amoxicillinStatus"
    const val rootSuffix = "rootView"
    const val summaryKey = "summaryKey"
    const val General_Danger_Signs = "General Danger Signs"
    const val NoOfDaysOfCough = "noOfDaysOfCough"
    const val NoOfDaysOfFever = "noOfDaysOfFever"
    const val NoOfDaysDiarrhoea = "numberOfDaysDiarrhoea"
    const val BreathPerMinute = "breathPerMinute"
    const val FB_MIN_MONTH = 2
    const val FB_MAX_MONTH = 12
    const val FB_MIN_YEAR = 1
    const val FB_MAX_YEAR = 5
    const val FB_MAX_BREATHING = 50
    const val FB_MIN_BREATHING = 40
    const val RMNCH = "rmnch"
    const val OrsDispensedStatus = "orsDispensedStatus"
    const val ZincDispensedStatus = "zincDispensedStatus"
    const val JellyWaterDispensedStatus = "jellyWaterDispensedStatus"
    const val SssDispensedStatus = "sssDispensedStatus"
    const val ZincStatus = "zincStatus"
    const val ORSStatus = "orsStatus"
    const val ACTStatus = "actStatus"
    const val SunkenEyes = "Sunken eyes"
    const val NoTearsWhenCrying = "No tears when crying"
    const val LittleOrNoUrine = "Little or no urine"
    const val SkinPinch = "Skin pinch going back very slowly"
    const val SunkenFontanella = "Sunken fontanelle"
    const val DryMouthOrTongue = "Dry mouth / tongue"
    const val VeryThirsty = "Very thirsty"
    const val otherSymptoms = "otherSymptoms"
    const val symptoms = "symptoms"
    const val hasOedemaOfBothFeet = "hasOedemaOfBothFeet"
    const val chestInDrawing = "chestInDrawing"
    const val signsAndSymptoms = "signsAndSymptoms"
    const val infoSuffixText = "informationSuffixText"
    const val Fever = "fever"
    const val Signs = "Signs"
    const val Cough = "Cough"
    const val InformationLayoutFragment = "InformationLayoutFragment"
    const val MalnutritionCondition = "malnutritionCondition"
    const val CoughCondition = "coughCondition"
    const val FeverCondition = "feverCondition"
    const val DiarrheaCondition = "diarrheaCondition"
    const val SevereDehydration = "Severe Dehydration"
    const val ModerateDehydration = "Moderate Dehydration"
    const val MaternalHealth = "Maternal Health"
    const val MentalHealth = "Mental Health"
    const val CardView = "CardView"
    const val other_compliance: String = "otherCompliance"
    const val Compliance_Type_Diabetes = "Diabetes"
    const val Compliance_Type_Hypertension = "Hypertension"
    const val id = "id"
    const val other_symptom = "otherSymptom"
    const val NoSymptoms = "No symptoms"
    const val complianceId: String = "complianceId"
    const val is_child_exists: String = "isChildExist"
    const val SSP16 = 16
    const val Middle_Name = "middleName"
    const val DM_Diagnosis = "DM"
    const val Provisional_Diagnosis = "provisionalDiagnosis"
    const val HTN_Diagnosis = "HTN"
    const val UpperLimitSystolic = 140
    const val UpperLimitDiastolic = 90
    const val PHQ9 = "PHQ9"
    const val GAD7 = "GAD7"
    const val Fetch_MH_Questions = "Fetch_MH_Questions"
    const val PHQ9_Mental_Health = "phq9MentalHealth"
    const val GAD7_Mental_Health = "gad7MentalHealth"
    const val pregnancyAnc = "pregnancyAnc"
    const val phoneNumberCategory = "phoneNumberCategory"
    const val landmark = "landmark"
    const val assessmentOrganizationId = "assessmentOrganizationId"
    const val relatedPersonFhirId = "relatedPersonFhirId"
    const val ncd = "ncd"
    const val assessmentType = "assessmentType"
    const val assessmentProcessType = "requestFrom"
    const val assessmentTakenOn = "assessmentTakenOn"
    const val Temperature = "temperature"
    const val organizationId = "organizationId"
    const val compliance = "compliance"
    const val symptomsDTO = "ncdSymptoms"
    const val encounter = "encounter"
    const val userId = "userId"
    const val PHQ9_Score = "phq9Score"
    const val PHQ9_Risk_Level = "phq9RiskLevel"
    const val GAD7_Score = "gad7Score"
    const val GAD7_Risk_Level = "gad7RiskLevel"
    const val PregnancyStatus = "pregnancyStatus"
    const val PregnancySymptoms = "pregnancySymptoms"
    const val GestationalPeriod = "gestationalPeriod"
    const val PregnancyANCMaxValue = 40
    const val pregnancyOtherSymptoms = "pregnancyOtherSymptoms"
    const val africa_uppercase = "AFRICA"
    const val modifiedDate = "modifiedDate"
    const val spiceUserId = "spiceUserId"
    const val memberReference = "memberReference"
    const val patientReference = "patientReference"
    const val HBA1CUnit = "hba1cUnit"
    const val Glucose_Date_Time = "glucoseDateTime"
    const val hba1c = "hba1c"
    const val phq4 = "phq4"
    const val phq9 = "phq9"
    const val gad7 = "gad7"
    const val suicidalIdeation = "suicideScreener"
    const val cageAid = "substanceAbuse"
    const val ObservationID = "ObservationId"

    const val TakingMinimumMealsPerDay = "takingMinimumMealsPerDay"
    const val ExclusivelyBreastfeeding = "exclusivelyBreastfeeding"
    const val FedFrom4FoodGroups = "fedFrom4FoodGroups"
    const val Measles1Given = "measles1Given"
    const val YellowFeverVacineGiven = "yellowFeverVacineGiven"
    const val Measles2Given = "measles2Given"

    const val errorSuffix = "errorMessageView"

    const val DEATH_OF_MOTHER_KEY = "deathOfMother"
    const val NA = "NA"
    const val NotApplicable = "Not Applicable"
    const val IsBloodyDiarrhoea = "isBloodyDiarrhoea"

    // TB Items
    const val HasCoughLastedLonger = "hasCoughLastedLonger"
    const val HasWeightLoss = "hasWeightLoss"
    const val HasNightSweatsTB = "hasNightSweats"
    const val DateOfOnset = "dateOfOnset"
    const val RelationshipToIC = "relationshipToIC"
    const val SleepLocation = "sleepLocation"
    const val PreviouslyTreatedForTB = "hasPreviouslyTreatedForTB"

    const val MemberUsingAnyFamilyPlanning = "isUsingFamilyPlanningMethod"
    const val IsAnySideEffects = "hasSideEffects"
    const val NeedOfOtherFamilyPlanning = "needsOtherFamilyPlanningMethod"
    const val WhichMethod = "familyPlanningMethod"
    const val SpecifySideEffects = "sideEffectsDescription"
    const val Contraceptive = "contraceptiveStatus"
    const val Family_Planning = "family_planning"
    const val FamilyPlanning = "familyPlanning"

    const val DiagnosedWithDiabetes = "isFamilyDHDiagnosed"
    const val RegularSmoker = "isSmoker"
    const val AlcoholConsumption = "isAlcoholic"
    const val WaistCircumference = "waistCircumference"
    const val NCDDetails = "ncdDetails"
    const val FamilyPlanningMethods = "familyPlanningMethods"
    const val FamilyPlanningDetails = "clientProfileAssessment"
    const val OtherFamilyPlanningMethod = "otherFamilyPlanningMethod"
    const val CondomsStatus = "condomsStatus"
    const val Not_Available = "Not Available"

    const val TBContactTracing = "contactTracing"
    const val TBScreening = "tbScreening"
    const val TBRxBuddyRegister = "rxBuddy"
    const val TBRxBuddyFollowUp = "rxBuddyFollowUp"

    // Medical Review
    const val PreTestCounselling = "preTestCounselling"
    const val PostTestCounselling = "postTestCounselling"
    const val TestForHiv = "testForHiv"
    const val otherRelationshipIC = "otherRelationshipIC"

    // Family Planning Field IDs
    const val NumberOfLivingChildren = "numberOfLivingChildren"
    const val DesireForChildrenInFuture = "desireForChildrenInFuture"

    // Family Planning Desire Values
    const val DesireYesWithin2Yrs = "yesWithin2Yrs"
    const val DesireYesAfter2Yrs = "yesAfter2Yrs"
    const val DesireNoMore = "noMoreChildren"
    const val DesireUnsure = "unsure"

    // Child Health Field Ids
    const val WHAT_FED_LAST_24_HRS = "childFeedLast24Hrs"
    const val HOURS_BREAST_FEED_AFTER_BIRTH = "hrsBreastFed"
    const val ADDITIONAL_FOOD_GIVEN_MONTHS = "monthAdditionalFeedGiven"
    const val BREAST_FEEDING = "childBreastFeeding"
    const val ADDITIONAL_FOOD_GIVEN_LAST_24_HRS = "additionalFood24Hrs"
    const val VACCINE_RECEIVED = "receivedVaccine"
    const val DEWORMING_MEDICINE = "dewormingMedicine"

    const val NAME = "name"

    const val NCD_SYMPTOMS = "ncdSymptoms"

    const val ANY_NEW_OR_WORSENING_SYMPTOMS = "Any new or worsening symptoms"

    const val NEW_WORSENING_SYMPTOMS = "newWorseningSymptoms"

    /**
     * Spinner : Are you taking medication regularly? (symptomsLog)
     */
    const val ID_NCD_SYMPTOMS_MEDICATION = "ncdSymptomsMedication"

    /**
     * SingleSelectionView : Have you been diagnosed with High blood pressure or hypertension before?
     */
    const val ID_DIAGNOSED_BP = "diagnosedBP"

    /**
     * SingleSelectionView : Have you been diagnosed with High blood Sugar or diabetes before?
     */
    const val ID_DIAGNOSED_GLUCOSE = "diagnosedGlucose"

    /**
     * Spinner : Have the glasses been sold?
     */
    const val ID_HAVE_THE_GLASSES_BEEN_SOLD = "haveTheGlassesBeenSold"

    // RMNCH Field IDs
    const val BLOOD_SUGAR = "bloodSugar"
    const val BLOOD_SUGAR_FASTING = "bloodSugarFasting"
    const val BLOOD_SUGAR_RANDOM = "bloodSugarRandom"
    const val SYSTOLIC = "systolic"
    const val DIASTOLIC = "diastolic"
    const val PREGNANT_WOMAN_EXISTING_ILLNESS = "pregnantWomanExistingIllness"
    const val PREGNANT_WOMAN_ON_TREATMENT = "pregnantWomanOnTreatment"
    const val PREVIOUS_PREGNANCY_COMPLICATIONS = "previousPregnancyComplications"
    const val EDEMA = "edema"
    const val URINARY_ALBUMIN = "urinaryAlbumin"
    const val FOLIC_ACID_TOTAL_CONSUMED = "folicAcidTotalConsumed"
    const val FOLIC_ACID_TABLETS = "folicAcidTablets"
    const val FOLIC_ACID_PROVIDED = "folicAcidProvided"
    const val IFA_TOTAL_CONSUMED = "ifaTotalConsumed"
    const val IFA_TABLETS = "ifaTablets"
    const val IFA_PROVIDED = "ifaProvided"
    const val CALCIUM_TOTAL_CONSUMED = "calciumTotalConsumed"
    const val CALCIUM_TABLETS = "calciumTablets"
    const val CALCIUM_PROVIDED = "calciumProvided"
    const val ANC_FROM_MEDICAL_DOCTOR = "ancFromMedicalDoctor"
    const val ULTRASOUND = "ultrasound"
    const val TEMPERATURE = "temperature"
    const val PULSE = "pulse"
    const val FUNDAL_HEIGHT = "fundalHeight"
    const val HEMOGLOBIN = "hemoglobin"
    const val URINARY_SUGAR = "urinarySugar"
    const val URINARY_BILIRUBIN = "urinaryBilirubin"
    const val DANGER_SIGNS_EXPERIENCED_12 = "dangerSignsExperienced12"
    const val DANGER_SIGNS_EXPERIENCED_13_27 = "dangerSignsExperienced13To27"
    const val DANGER_SIGNS_EXPERIENCED_28_40 = "dangerSignsExperienced28To40"
    const val HEIGHT = "height"

    const val WEIGHT = "weight"
    const val BMI = "bmi"

    // RMNCH Form Group IDs
    const val GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION = "medicalHistoryPhysicalExamination"
    const val GROUP_POINT_OF_CARE_INVESTIGATIONS = "pointOfCareInvestigations"
    const val GROUP_VACCINATION_AND_SUPPLEMENTS = "vaccinationAndSupplements"
    const val GROUP_ANC_SERVICES_BIRTH_PREPAREDNESS = "ancServicesBirthPreparedness"
    const val GROUP_DANGER_SIGNS_RISK_IDENTIFICATION = "dangerSignsRiskIdentification"
    const val GROUP_SUMMARY = "summary"
    const val GROUP_COUNSELLING = "counselling"
    const val GROUP_COUNSELLING_FOLLOW_UP = "counsellingFollowUp"

    // RMNCH Form Group IDs (for nested field access)
    val ANC_FORM_GROUPS = listOf(
        GROUP_MEDICAL_HISTORY_PHYSICAL_EXAMINATION,
        GROUP_POINT_OF_CARE_INVESTIGATIONS,
        GROUP_VACCINATION_AND_SUPPLEMENTS,
        GROUP_ANC_SERVICES_BIRTH_PREPAREDNESS,
        GROUP_DANGER_SIGNS_RISK_IDENTIFICATION,
        GROUP_SUMMARY,
        GROUP_COUNSELLING,
        GROUP_COUNSELLING_FOLLOW_UP,
    )

    // RMNCH Status Values
    const val STATUS_HIGH_RISK = "High Risk"
    const val BN_STATUS_HIGH_RISK = "উচ্চ ঝুঁকি"
    const val STATUS_HIGH_FEVER = "High Fever"
    const val BN_STATUS_HIGH_FEVER = "তীব্র জ্বর"
    const val STATUS_FEVER = "Fever"
    const val BN_STATUS_FEVER = "জ্বর"
    const val STATUS_ABNORMAL = "Abnormal"
    const val BN_STATUS_ABNORMAL = "অস্বাভাবিক"
    const val STATUS_MODERATE_ANEMIA = "Moderate Anemia"
    const val BN_STATUS_MODERATE_ANEMIA = "মাঝারি রক্তসল্পতা"
    const val STATUS_SEVERE_ANEMIA = "Severe Anemia"
    const val BN_STATUS_SEVERE_ANEMIA = "মারাত্মক রক্তসল্পতা"
    const val STATUS_MILD_ANEMIA = "Mild Anemia"
    const val BN_STATUS_MILD_ANEMIA = "মৃদু রক্তসল্পতা"
    const val STATUS_GAP = "Gap"
    const val BN_STATUS_GAP = "ঘাটতি"
    const val VALUE_PRESENT = "present"
    const val VALUE_NOT_DONE = "notDone"
    const val VALUE_NO = "no"
    const val VALUE_FASTING = "fasting"
    const val VALUE_RANDOM = "random"

    // RMNCH Illness Identifiers
    const val ILLNESS_DM = "DM"

    // RMNCH Blood Pressure Thresholds
    const val BP_SYSTOLIC_THRESHOLD = 140.0
    const val BP_DIASTOLIC_THRESHOLD = 90.0

    // RMNCH Blood Sugar Thresholds
    const val BLOOD_SUGAR_FASTING_THRESHOLD = 5.1
    const val BLOOD_SUGAR_RANDOM_THRESHOLD = 8.5

    // RMNCH Temperature Thresholds (Fahrenheit)
    const val TEMP_HIGH_FEVER_THRESHOLD = 102.0
    const val TEMP_FEVER_MIN_THRESHOLD = 100.0
    const val TEMP_FEVER_MAX_THRESHOLD = 101.9

    // RMNCH Pulse Thresholds
    const val PULSE_HIGH_THRESHOLD = 90.0
    const val PULSE_LOW_THRESHOLD = 60.0

    // RMNCH Fundal Height Tolerance
    const val FUNDAL_HEIGHT_TOLERANCE_CM = 2.0

    // RMNCH Hemoglobin Thresholds
    const val HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD = 8.0
    const val HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD = 10.0
    const val HEMOGLOBIN_MILD_ANEMIA_THRESHOLD = 11.0

    // RMNCH Blood sugar
    const val LOW_SUGAR_THRESHOLD = 4

    // ANC BMI
    const val BMI_NORMAL_WEIGHT_THRESHOLD = 18.5
    const val BMI_OVER_WEIGHT_THRESHOLD = 25.0
    const val BMI_OBSESS_WEIGHT_THRESHOLD = 30.0

    // RMNCH Tablet Consumption Threshold
    const val TABLET_CONSUMPTION_THRESHOLD = 30

    // RMNCH Gestational Age Thresholds (weeks)
    const val GESTATIONAL_AGE_WEEK_12 = 12.0
    const val GESTATIONAL_AGE_WEEK_13 = 13.0
    const val GESTATIONAL_AGE_WEEK_24 = 24.0
    const val GESTATIONAL_AGE_WEEK_20 = 20.0
    const val GESTATIONAL_AGE_WEEK_27 = 27.0
    const val GESTATIONAL_AGE_WEEK_28 = 28.0
    const val GESTATIONAL_AGE_WEEK_36 = 36.0
    const val GESTATIONAL_AGE_WEEK_40 = 40.0

    // RMNCH Other Constants
    const val DAYS_PER_WEEK = 7.0
    const val ANC_VISIT_NUMBER_1 = 1

    const val GRAVIDA_THRESHOLD_FOR_PARITY = 2
    const val PARITY_HIGH_RISK_THRESHOLD = 4
    const val NUMBER_OF_LIVING_CHILDREN_THRESHOLD = 1
    const val AGE_OF_LAST_CHILD_HIGH_RISK_YEARS = 2
    const val WEEKS_SINGULAR_THRESHOLD = 1.0
    const val MONTHS_FOR_YEARS_DISPLAY = 12
    const val MONTHS_YEARS_SINGULAR_THRESHOLD = 1

    // RMNCH Summary Field IDs
    const val HIGH_RISK_PREGNANT_WOMAN = "highRiskPregnantWoman"
    const val GAPS_IN_ANC = "gapsInAnc"
    const val REFERRAL_FACILITY = "referralFacility"
    const val FACILITY_IDENTIFIED_FOR_DELIVERY = "facilityIdentifiedForDelivery"

    const val ANC_VISIT_DATE = "ancVisitDate"

    const val LABEL_HIGH_RISK_PREGNANT_WOMAN = "High risk pregnant woman"

    const val LABEL_GAPS_IN_ANC = "Gaps in ANC"

    const val LABEL_HIGH_RISK_MOTHER = "High risk mother"

    const val LABEL_GAPS_IN_PNC = "Gaps in PNC"

    // Facility Identified For Delivery option IDs
    const val FACILITY_NOT_IDENTIFIED = "notIdentified"
    const val FACILITY_HOME_DELIVERY = "homeDelivery"

    const val ILLNESS_HEART_DISEASE = "heartDisease"
    const val ILLNESS_TUBERCULOSIS = "tuberculosis"
    const val ILLNESS_ASTHMA = "asthma"
    const val ILLNESS_THYROID = "thyroidDisease"
    const val ILLNESS_KIDNEY_DISEASE = "kidneyDisease"

    // Age Thresholds for High Risk Pregnancy
    const val AGE_MIN_THRESHOLD = 18
    const val AGE_MAX_THRESHOLD = 35
    const val BIRTH_SPACING_THRESHOLD_YEARS = 2
    const val MULTIPARA_THRESHOLD = 3

    const val CONDITION_ANY_OTHER = "Other Danger Signs"
    const val BN_CONDITION_ANY_OTHER = "অন্যান্য বিপদ চিহ্ন"

    // Pregnancy Complication IDs
    const val COMPLICATION_CONVULSIONS = "convulsions"
    const val COMPLICATION_POSTPARTUM_HEMORRHAGE = "postpartum_hemorrhage"
    const val COMPLICATION_SEVERE_ANEMIA = "severe_anemia"
    const val COMPLICATION_GESTATIONAL_DIABETES = "gestational_diabetes"

    // Gap Condition Field IDs
    const val TT_TD_COMPLETED = "ttTdCompleted"
    const val MIN_ANC_VISITS_REQUIRED = 3

    // Pregnancy Outcome Field IDs
    const val TIME_OF_DEATH = "timeOfDeath"
    const val MATERNAL_DEATH = "maternalDeath"
    const val GESTATION_MONTH_AT_DEATH = "gestationMonthAtDeath"
    const val CAUSE_OF_DEATH = "causeOfDeath"
    const val GESTATION_MONTH_AT_ABORTION = "gestationMonthAtAbortion"
    const val TYPE_OF_ABORTION = "typeOfAbortion"
    const val DELIVERY_OUTCOME = "deliveryOutcome"
    const val PLACE_OF_DELIVERY = "placeOfDelivery"
    const val DATE_OF_DELIVERY = "dateOfDelivery"
    const val COUNSELLING_ABORTION = "counsellingAbortion"
    const val COUNSELLING_STILL_BIRTH = "counsellingStillBirth"
    const val COUNSELLING_NEONATAL_DEATH = "counsellingNeonatalDeath"
    const val COUNSELLING_EMOTIONAL_SUPPORT = "counsellingEmotionalSupport"
    const val COUNSELLING_FUTURE_PREGNANCY_PLANNING = "counsellingFuturePregnancyPlanning"
    const val STILLBIRTH_NUMBERS = "stillbirthNumbers"
    const val NEWBORN_DETAILS = "newbornDetails"
    const val IS_BABY_ALIVE = "isBabyAlive"
    const val SEX = "sex"
    const val BIRTH_WEIGHT = "birthWeight"
    const val CAUSE_OF_NEONATAL_DEATH = "causeOfNeonatalDeath"

    const val YES = "Yes"
    const val NO = "No"

    /** Newborn sex options (pregnancy outcome); ids match saved JSON. */
    val pregnancyOutcomeNewbornSexOptions: ArrayList<Map<String, Any>> = arrayListOf(
        mapOf(DefinedParams.ID to "male", DefinedParams.NAME to "Male", DefinedParams.CULTURE_VALUE to "পুরুষ"),
        mapOf(DefinedParams.ID to "female", DefinedParams.NAME to "Female", DefinedParams.CULTURE_VALUE to "নারী"),
        mapOf(DefinedParams.ID to "other", DefinedParams.NAME to "Other", DefinedParams.CULTURE_VALUE to "অন্যান্য"),
    )

    /** Is baby alive options; ids match [YES] / [NO] in saved data. */
    val pregnancyOutcomeBabyAliveOptions: ArrayList<Map<String, Any>> = arrayListOf(
        mapOf(DefinedParams.ID to YES, DefinedParams.NAME to YES, DefinedParams.CULTURE_VALUE to "হ্যাঁ"),
        mapOf(DefinedParams.ID to NO, DefinedParams.NAME to NO, DefinedParams.CULTURE_VALUE to "না"),
    )

    /**
     * Resolves a saved option id to display [DefinedParams.NAME] or [DefinedParams.CULTURE_VALUE].
     */
    fun pregnancyOutcomeOptionDisplayLabel(
        options: List<Map<String, Any>>,
        savedId: String?,
        translationEnabled: Boolean,
    ): String? {
        if (savedId.isNullOrBlank()) return null
        val idNorm = savedId.trim()
        options.forEach { opt ->
            val id = (opt[DefinedParams.ID] as? String)?.trim() ?: return@forEach
            if (id.equals(idNorm, ignoreCase = true)) {
                return if (translationEnabled) {
                    opt[DefinedParams.CULTURE_VALUE] as? String ?: opt[DefinedParams.NAME] as? String
                } else {
                    opt[DefinedParams.NAME] as? String
                }
            }
        }
        return null
    }

    const val NUTRITION_COUNSELLING = "nutritionCounselling"
    const val CARE_DURING_ANTENATAL_PERIOD = "careDuringAntenatalPeriod"
    const val BIRTH_PREPAREDNESS = "birthPreparedness"
    const val NEW_BORN_CARE_EDUCATION = "newbornCareEducation"

    const val AVG_SYSTOLIC = "avgSystolic"
    const val AVG_DIASTOLIC = "avgDiastolic"

    const val BP_LOG = "bpLog"
    const val BP_LOG_DETAILS = "bpLogDetails"
    const val AVG_BLOOD_PRESSURE = "avgBloodPressure"

    const val CVD_RISK = "cvdRisk"

    const val GLUCOSE_LOG = "glucoseLog"
    const val SYMPTOMS_LOG = "symptomsLog"

    const val HBA1C_DATE_TIME = "hba1cDateTime"

    const val GLUCOSE_UNIT = "glucoseUnit"
    const val GLUCOSE_TYPE = "glucoseType"
    const val GLUCOSE = "glucose"
    const val BMI_CATEGORY = "bmiCategory"
    const val MMHG = "mmHg"
    const val MMOLL = "mmol/L"
    const val MGDL = "mg/dL"

    const val FBS = "fbs"
    const val RBS = "rbs"

    const val REFERRAL_FACILITY_TYPE = "referralFacilityType"

    const val REFERRED_SITE = "referredSite"

    const val FACILITY_TYPE_UPAZILA = "Upazila Health Complex"

    const val FACILITY_TYPE_COMMUNITY_CLINIC = "Community Clinic"

    const val STATUS_FACILITY_TYPE_UPAZILA = "Referred To Upazila Health Complex"

    const val STATUS_FACILITY_TYPE_COMMUNITY_CLINIC = "Referred To Community Clinic"

    const val CULTURE_VALUE = DefinedParams.CULTURE_VALUE

    const val IS_REGULAR_SMOKER = "isRegularSmoker"

    const val FBS_MAXIMUM_MGDL_VALUE = 110
    const val RBS_MAXIMUM_MGDL_VALUE = 140
    const val FBS_MAXIMUM_VALUE_BD = 7.0
    const val RBS_MAXIMUM_VALUE_BD = 11.1

    const val UPAZILA_FBS_RBS_MAXIMUM_VALUE_BD = 15

    const val UPAZILA_UPPER_LIMIT_SYSTOLIC = 160
    const val UPAZILA_UPPER_LIMIT_DIASTOLIC = 100

    const val COUNSELLING_ADVERSE_EVENT = "counsellingAdverseEvent"

    // Pregnancy Outcome Cause of Death Option IDs
    const val CAUSE_OF_DEATH_OBSTRUCTED_LABOR = "obstructedLabor"
    const val CAUSE_OF_DEATH_UNSAFE_ABORTION = "unsafeAbortion"

    // Pregnancy Outcome Time of Death Values
    const val TIME_OF_DEATH_BEFORE_DELIVERY = "beforeDelivery"
    const val TIME_OF_DEATH_DURING_CHILDBIRTH = "duringChildbirth"
    const val TIME_OF_DEATH_WITHIN_42_DAYS_AFTER_DELIVERY = "within42DaysAfterDelivery"

    /**
     * DialogCheckbox : Child complications
     */
    const val ID_CHILD_ILLNESS_TYPE = "childIllnessType"

    /**
     * SingleSelectionView : Has referral been made?
     */
    const val ID_CHILD_REFERRAL = "childReferral"

    /**
     * Spinner : Please select Referral Facility
     */
    const val ID_CHILD_REFERRAL_FACILITY_TYPE = "childReferralFacilityType"

    /**
     * SingleSelectionView : Does the child have any congenital defect?
     */
    const val ID_CONGENITAL_DEFECT = "congenitalDefect"

    /**
     * SingleSelectionView : Has the child received vaccines?
     */
    const val ID_RECEIVED_VACCINE = "receivedVaccine"

    const val FP_METHOD_STERILIZATION_MALE = "sterilizationMale"
    const val FP_METHOD_STERILIZATION_FEMALE = "sterilizationFemale"

    /**
     * CardView : Abortion
     */
    const val ID_ABORTION = "abortion"

    /**
     * CardView : Delivery Outcomes
     */
    const val ID_DELIVERY_OUTCOMES = "deliveryOutcomes"

    /**
     * EditText : Live Birth
     */
    const val ID_LIVE_BIRTH_NUMBERS = "liveBirthNumbers"

    /**
     * EditText : Stillbirth Numbers
     */
    const val ID_STILL_BIRTH_NUMBERS = "stillbirthNumbers"

    /**
     * SingleSelectionView : Mode of Delivery
     */
    const val ID_MODE_OF_DELIVERY = "modeOfDelivery"

    enum class ModeOfDelivery(val value: String) {
        NORMAL_DELIVERY("normalDelivery"),
        ASSISTED_DELIVERY("assistedDelivery"),
        CESAREAN_SECTION("cesareanSection"),
    }

    const val FAMILY_PLANNING_FORM = "family_planning_form"
    const val PREGNANT_WOMEN_PROFILE_FORM = "pregnancy_woman_profile"
    const val RMNCH_ANC_FORM = "rmnch_anc_visit"
    const val PREGNANCY_OUTCOME_FORM = "pregnancy_outcome_workflow_form"
    const val RMNCH_PNC_FORM = "rmnch_pnc_visit"
    const val RMNCH_CHILD_VISIT_FORM = "rmnch_childhood_visit"

    const val EYE_CARE = "eyeCare"

    const val CATARACT = "cataract"

    const val EYE_TEST_OUTCOME = "eyeTestOutcome"

    const val EYE_TEST_OUTCOMES = "eyeTestOutcomes"

    const val TYPE_OF_GLASS = "typeOfGlass"

    const val HISTORY_OF_OTHER_DISEASES_ = "historyOfOtherDiseases_"

    const val OPERATION_NAME_ = "operationName_"

    const val EYE_DISEASE_ = "eyeDisease_"

    const val REASON_ = "reason_"

    const val BIO_METRICS = "bioMetrics"

    const val TITLE_SUFFIX = "titleTextView"
    const val SUMMARY_VALUE = "summaryValue"
    const val SUMMARY_ROOT = "summaryRoot"

    const val EYE_CARE_FORM = "eye_care"
    const val CATARACT_FORM = "cataract"

    const val GENERAL_INFORMATION = "generalInformation"
    const val CAMP_DATE = "camp_date"
    const val CAMP_TYPE = "camp_type"
    
    const val EYE_DISEASE = "eyeDisease"

    const val HISTORY_OF_OTHER_DISEASES = "historyOfOtherDiseases"

    const val OPERATION_NAME = "operationName"

    const val REASON = "reason"

    const val IS_BEFORE_DIABETES_DIAGNOSIS = "isBeforeDiabetesDiagnosis"

    const val IS_BEFORE_HTN_DIAGNOSIS = "isBeforeHtnDiagnosis"
}
