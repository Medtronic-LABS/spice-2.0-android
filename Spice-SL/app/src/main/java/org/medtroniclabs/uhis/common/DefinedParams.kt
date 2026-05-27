package org.medtroniclabs.uhis.common

import android.provider.ContactsContract

object DefinedParams {
    const val ZERO = "0"

    const val ID = "id"
    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"
    const val GENDER_OTHER = "Other"
    const val BOTH = "both"
    const val AT_CHAR = "@"
    const val SPAN_COUNT_1 = 1
    const val SPAN_COUNT_3 = 3
    const val span_count_1 = 1
    const val SPAN_COUNT_THREE = 3
    const val SPAN_COUNT_2 = 2
    const val IS_MEMBER_REGISTRATION = "isMemberRegistration"
    const val MEMBER_ID = "memberID"
    const val MENU_ID = "MenuId"
    const val FEMALE = "Female"
    const val MALE = "Male"
    const val NAME = "name"
    const val CULTURE_VALUE = "cultureValue"
    const val id = ID
    const val ICCM = "ICCM"
    const val TB = "TB"
    const val TB_SCREENING = "tbScreening"
    const val NO = "No"
    const val YES = "Yes"
    const val yes = "yes"
    const val OTHER = "Other"
    const val ENABLED = "enabled"
    const val USERNAME = "username"
    const val PASSWORD = "password"
    const val AUTHORIZATION = "Authorization"
    const val ACTION_SESSION_EXPIRED =
        "${ContactsContract.Directory.PACKAGE_NAME}.action.SL_SESSION_EXPIRED"
    const val SL_SESSION = "sl_session"
    const val HOUSEHOLD_MEMBER_REGISTRATION = "household_member_registration"
    const val HOUSEHOLD_MEMBER_REGISTRATION_FORM = "member_registration"
    const val EXTERNAL_MEMBER_REGISTRATION = "external_member_registration"
    const val HOUSEHOLD_REGISTRATION = "household_registration"
    const val DEFAULT_SELECT_ID = -1L
    const val DEFAULT_ID = "-1"
    const val DEFAULT_ID_LABEL = "--Select--"
    const val LIST_LIMIT = 15
    const val PAGE_INDEX = 0
    const val LABEL = "label"
    const val color = "color"
    const val Value = "value"
    const val EnrollmentType = "EnrollmentType"
    const val MenuTitle = "MenuTitle"
    const val PatientId = "PatientId"
    const val CHILD_PATIENT_ID = "childPatientId"
    const val TIME_OF_DELIVERY = "timeOfDelivery"
    const val TIME_OF_LABOUR_ONSET = "timeOfLabourOnset"
    const val HouseholdHead = "HouseholdHead"
    const val DateOfDelivery = "DateOfDelivery"
    const val CHIEF_DOM_CODE_LENGTH = 3
    const val VILLAGE_CODE_LENGTH = 4
    const val PATIENT_NUMBER_LENGTH = 4
    const val StateOfPerineum = "stateOfPerineum"
    const val Tear = "tear"
    const val EPISIOTOMY = "episiotomy"
    const val None = "none"
    const val GENDER = "gender"
    const val StateOfBaby = "stateOfBaby"
    const val REFERRED = "Referred"
    const val CALL_RESULT = "CallResult"
    const val PATIENT_STATUS = "PatientStatus"
    const val UN_SUCCESSFUL = "UnSuccessful"
    const val SUCCESSFUL = "successful"

    const val UNSUCCESSFUL = "unSuccessful"
    const val BaseUrl = "base_url"

    const val OnTreatment = "On Treatment"
    const val OnHold = "on-hold"
    const val Active = "active"
    const val PregnancyANC = "PregnancyANC"
    const val PREGNANCY_PNC = "PregnancyPNC"
    const val ENCOUNTER_ID = "encounterId"
    const val ChildEncounterId = "childEncounterId"
    const val IsNeonate = "isNeonate"
    const val True = "true"

    const val EXCLUSIVE_BREAST_CONDITION = "ExclusiveBreastCondition"
    const val BREAST_CONDITION = "BreastCondition"
    const val UterusCondition = "UterusCondition"
    const val CORD_EXAMINATION = "CordExamination"
    const val CONGENITAL_DETECT = "CongenitalDetect"

    const val TENANT_ID = "tenantId"
    const val FhirId = "fhirId"
    const val RMNCH = "RMNCH"
    const val FREQUENCY = "frequency"
    const val DISPLAY_ORDER = "displayOrder"

    const val SIGN_DIR = "sign"
    const val SIGN_SUFFIX = "_signature"

    const val DOB = "DOB"
    const val LMB = "LMB"
    const val Recovered = "Recovered"
    const val PatientReference = "patientReference"
    const val MemberReference = "memberReference"
    const val ReferenceId = "referenceId"
    const val ProgramId = "programId"
    const val valueColor = "color"
    const val ABOVE_5_MEDICAL_REVIEW = "Above5MedicalReview"
    const val NEONATE_BIRTH_REVIEW = "NEONATE_BIRTH_REVIEW"
    const val MOTHER_DELIVERY_REVIEW = "MOTHER_DELIVERY_REVIEW"
    const val IMMUNIZATION = "Immunization"
    const val ICCMUNDER2MONTHS = "iccm-under-2M"
    const val ICCM_ABOVE_2M_5Y = "iccm-2M-5Y"
    const val PregnancyAncMedicalReview = "pregnancyAncMedicalReview"
    const val TB_REVIEW = "TB_REVIEW"

    const val Anaemia = "anaemia"
    const val Hiv = "hivRDT"
    const val Cough = "cough"
    const val CoughOrDifficultBreathing = "Cough or difficult breathing"
    const val MalnutritionOrAnaemia = "Malnutrition / Anaemia"
    const val HivAndAids = "HIV / AIDS RDT"
    const val PREGNANT = "Pregnant"
    const val POSTPARTUM = "Postpartum"
    const val Lactating = "Lactating"
    const val REFRESH_FRAGMENT = "REFRESH_FRAGMENT"

    const val NeonatePatientIdPrefix = "HM-"
    const val NeonateBabyNamePrefix = "Baby of"
    const val FOLLOW_UP_ID = "FollowUpId"
    const val IsReferredScreen = "isReferredScreen"
    const val SearchLength = 2
    const val SEARCH_LENGTH_PRESCRIPTION = 1
    const val OTHER_NOTES = "otherNotes"

    const val Postnatal = "Postnatal"

    const val TestedOn = "TestedOn"
    const val UNIT = "_unit"
    const val POST_NATAL = "Post Natal"
    const val Neonate = "Neonate"
    const val PNC_HISTORY = "PncHistory"

    val changeFacility = "changeFacility"
    const val VillageList = "VillageList"

    const val PASSWORD_REGEX_PATTERN = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{6,64}$"

    const val LowBirthWeight = 2.0
    const val OTHERS_SPECIFY = "others"

    const val KeySignature = "KeySignature"
    const val KEY_INITIAL = "KeyInitial"

    const val LAST_SYNC_DATE = "lastSyncDate"
    const val FollowUpStartTiming = "followUpStartTiming"

    const val HOUSE_HOLD_LINK_START_TIMING = "houseHoldLinkStartTiming"

    const val BuildConfigs = "BuildConfiguration"
    const val HOSUE_HOLD_HEAD = "HouseholdHead"

    const val ASSIGNED = "Assigned"
    const val UnAssigned = "Unassigned"

    const val FhirMemberID = "FhirmemberID"

    const val name = "name"

    const val AGE = "age"

    const val Landing = "landing"
    const val Screening = "screening"
    const val Registration = "registration"
    const val Assessment = "assessment"
    const val MyPatients = "my_patients"
    const val MEDICAL_REVIEW = "medicalReview"
    const val Workflow = "workflow"
    const val BioData = "bioData"
    const val COUNTRY = "country"
    const val District = "district"
    const val CHIEFDOM = "chiefdom"
    const val VILLAGE = "village"
    const val PROGRAM = "program"
    const val HEALTH_FACILITY_ID = "healthFacilityId"
    const val HealthFacilityFhirId = "healthFacilityFhirId"
    const val Provenance = "provenance"
    const val ORIGIN = "origin"
    const val EMPOWER_HEALTH_NCD = "Empower Health NCD"
    const val NCD_REGISTER = "NCD Register"
    const val PATIENT_ID = "patientId"
    const val RELATED_PERSON_FHIR_ID = "relatedPersonFhirId"
    const val ASSESSMENT_ORGANIZATION_ID = "assessmentOrganizationId"
    const val FORM_TYPE_ID = "formTypeId"
    const val BP_LOG = "bpLog"
    const val GLUCOSE_LOG = "glucoseLog"
    const val DESCRIPTION = "description"

    const val QUANTITY = "quantity"
    const val PRESCRIPTION = "PRESCRIPTION"
    const val TABLET = "Tablet"
    const val LIQUID_ORAL = "Liquid"
    const val INJECTION_INJECTABLE_SOLUTION = "Injection"
    const val CAPSULE = "Capsule"
    const val AFRICA = "AFRICA"
    const val PATIENT_VISIT_ID = "PatientVisitId"
    const val TYPE_REFILL = "REFILL"
    const val ACTIVITY = "Activity"
    const val TITLE = "Title"
    const val Count = "Count"
    const val SCREENED = "SCREENED"
    const val ASSESSED = "ASSESSED"
    const val ENROLLED = "ENROLLED"
    const val REGISTERED = "REGISTERED"
    const val REFERREDD = "REFERRED"
    const val DISPENSE = "dispense"
    const val Nutritionlifestyle = "nutritionlifestyle"
    const val Investigation = "investigation"

    const val RED_RISK_LOW = "Low"
    const val RedRiskModerate = "Moderate"
    const val RedRiskHigh = "High"

    const val RiskColorCode = "riskColorCode"
    const val RiskLevel = "riskLevel"
    const val RISK_MESSAGE = "riskMessage"

    const val PROVISIONAL_TREATMENT_PLAN = "treatmentPlanResponse"
    const val TREATMENT_PLAN = "treatmentPlan"
    const val FormInput = "formInput"
    const val ViewScreens = "viewScreens"
    const val IntentPatientDetails = "IntentPatientDetails"

    const val FBS = "FBS"
    const val RBS = "RBS"
    const val RBS_FBS = "RBS & FBS"
    const val rbs = "rbs"
    const val fbs = "fbs"
    const val DialogWidth = 720f
    const val COMMUNITY = "COMMUNITY"
    const val NON_COMMUNITY = "NON_COMMUNITY"
    const val DIRECT_PNC_FLOW = "DirectPNCFlow"
    const val LabourDeliveryData = "LabourDeliveryData"
    const val NEONATE_OUTCOME = "NeonateOutcome"
    const val STATUS = "status"
    const val COMMENTS = "comments"
    const val YEAR_OF_DIAGNOSIS = "yearOfDiagnosis"
    const val MENTAL_HEALTH_DISORDER = "mentalHealthDisorder"

    const val OTHERS = "Others"

    const val RED_MAX_MUAC = 11.5
    const val YELLOW_MAX_MUAC = 12.5
    const val GREEN_MAX_MUAC = 26.5

    const val BOLD = "bold"
    const val ITALIC = "italic"
    const val BOLD_ITALIC = "bold_italic"
    const val APP_TYPE = "appType"

    const val EN_LOCALE = "English"
    const val SW_LOCALE = "Swahili"
    const val BN_LOCALE = "বাংলা"
    const val EN = "en"
    const val SW = "sw"
    const val BN = "bn"
    const val COMMUNITY_PROFILE = "community_profile"
    const val COMMUNITY_ID = "community_id"

    const val COMMUNITY_NAME = "name"
    const val COMMUNITY_DESC = "description"
    const val COMMUNITY_REGISTERED_DATE = "communityRegisteredDate"
    const val VILLAGE_ID = "villageId"

    const val MARKET_DAYS = "marketDays"
    const val NEAREST_PHU = "nearestPHU"
    const val COMMUNITY_REGISTERED = "community_registered"
    const val CBS = "CBS"
    const val IS_DEFAULT = "isDefault"
    const val PHONE_NUMBER = "phoneNumber"
    const val surveillanceDetails = "surveillanceDetails"
    const val ASSESSMENT_ID = "AssessmentId"
    const val type = "type"
    const val ps = "ps"
    const val phu = "phu"
    const val CBS_Referral = "CBS Referral"

    const val NOTIFIABLE_CONDITIONS = "notifiableConditions"
    const val ICCM_DIARRHEA_NOTIFIABLE_CONDITION = "iccmDiarrheaNotifiableCondition"
    const val ICCM_FEVER_NOTIFIABLE_CONDITION = "iccmFeverNotifiableCondition"
    const val OtherNotifiableConditionsForFever = "otherNotifiableConditionsForFever"
    const val OtherNotifiableConditionsForDiarrhoea = "otherNotifiableConditionsForDiarrhoea"
    const val CbsNotifiableCondition = "cbsNotifiableCondition"
    const val RmnchNotifiableCondition = "rmnchNotifiableCondition"
    const val OTHER_NOTIFIABLE_CONDITIONS = "otherNotifiableConditions"

    const val IS_DEEP_LINK = "isDeepLink"

    const val TestName = "testName"
    const val Result = "result"
    const val Uom = "uom"
    const val Test_Name = "Test Name"
    const val BIRTH = "birth"
    const val ANC_CBS = "anc_cbs"
    const val BOY = "Boy"
    const val GIRL = "Girl"
    const val IS_FAMILY_PLAN_SUMMARY = "isFamilyPlanSummary"
    const val notifiableConditions = "notifiableConditions"
    const val otherNotifiableConditions = "otherNotifiableConditions"
    const val PEER_SUPERVISOR = "PeerSupervisor"
    const val PHU = "PHU"
    const val FollowUp = "followUp"
    const val FollowUpDetails = "followUpDetails"

    const val CONTACT_TRACING = "contactTracing"
    const val CONTACT_TRACE_UPDATED = 3
    const val still_birth = "Still Birth"
    const val MOTHER_ID = "MOTHER_ID"
    const val householdId = "HOUSEHOLD_ID"
    const val villageId = "VILLAGE_ID"
    const val Referred_NCD = "NCD"
    const val isTbPatient = "isTbPatient"
    const val HOUSEHOLD_ID = "HouseholdId"

    const val TB_TYPE = "TbType"
    const val RxBuddyId = "rxBuddyId"
    const val RxBuddyName = "rxBuddyName"
    const val RxRelationShip = "rxRelationShip"
    const val RxPhoneNo = "rxPhoneNo"
    const val RxMonitoringSheetProvided = "hasProvidedMonitoringSheet"

    const val RX_BUDDY_FOLLOW_UP = "rxBuddyFollowUp"
    const val RX_BUDDY_FOLLOW_UP_VALUES = "rxBuddyFollowUpValues"
    const val MonitoringSheetDate = "MonitoringSheetDate"
    const val SYMPTOMS_FOLLOW_UP = "SymptomsFollowUp"
    const val MedicationFollowUp = "MedicationFollowUp"
    const val isRxBuddyFollowUp = "isRxBuddyFollowUp"
    const val DrugSensitiveTB = "drugSensitiveTB"
    const val ExtraPulmonary = "extraPulmonary"
    const val SITE_OF_DISEASE = "siteOfDisease"
    const val OrganAffected = "organAffected"
    const val IS_SUMMARY = "isSummary"

    const val IS_HOUSE_HOLD = "houseHold"
    const val RIF_RESISTANCE = "RIF Resistance"
    const val MTB_Detected = "MTB Detected"
    const val GENE_EXPERT = "gene expert"

    const val isCreateHouseholdForPhu = "isCreateHouseholdForPhu"
    const val FP = "FP"
    const val HIV = "HIV"
    const val HIV_IMR_CMR = "HIV_IMR_CMR"
    const val ENTRY_POINT = "EntryPoint"
    const val Symptoms = "Symptoms"
    const val HIV_MEDICAL_REVIEW = "HIV_MEDICAL_REVIEW"

    const val OCCUPATION = "occupation"
    const val MARITAL_STATUS = "maritalStatus"

    const val MARRIED = "Married"

    const val SINGLE = "Single"
    const val HINT = "Hint"
    const val LENGTH = "Length"
    const val INPUT_TYPE = "inputType"

    const val EMTCT_HIV_MEDICAL_SCREENING = "EMTCT_HIV_MEDICAL_SCREENING"
    const val EMTCT_HIV_MEDICAL_REVIEW = "EMTCT_HIV_MEDICAL_REVIEW"
    const val HIV_MEDICAL_SCREENING = "HIV_MEDICAL_SCREENING"
    const val EMTCT = "EMTCT"
    const val EMTCTMR = "EMTCTMR"
    const val EMTCT_SUMMARY = "EMTCT_SUMMARY"
    const val isPregnant = "isPregnant"
    const val VIRAL_LOAD = "VIRAL_LOAD"
    const val IS_CD4 = "isCD4"
    const val IS_CD4_PERCENTAGE = "isCD4Percentage"
    const val Post_Partum = "Post Partum"
    const val POST_PARTUM = "postPartum"
    const val EMTCT_VISIT_STATUS = "emtct_visit_status"
    const val CD4 = "CD4"
    const val CD4_PERCENTAGE = "CD4 Percentage"
    const val HIV_TESTED_POSITIVE = "hivTestedPositive"
    const val NO_SMALL = "no"
    const val NOT_APPLICABLE = "not-applicable"
    const val MEDICATIONS = "medications"
    const val REGIMEN = "regimen"
    const val PRESCRIBED_MEDICINE = "PrescribedMedicine"
    const val IS_MENU_TYPE_HIV = "isMenutypeHiv"

    const val CVD_RISK_SCORE = "cvdRiskScore"
    const val CVD_RISK_LEVEL = "cvdRiskLevel"
    const val CVD_RISK_SCORE_DISPLAY = "cvdRiskScoreDisplay"

    const val UNIT_MEASUREMENT_KEY = "unitMeasurement"
    const val UNIT_MEASUREMENT = "unit_measurement"

    const val UNIT_MEASUREMENT_METRIC_TYPE = "metric"
    const val UNIT_MEASUREMENT_IMPERIAL_TYPE = "imperial"
    const val HBA1C = "HbA1c"

    const val IS_OFFLINE = "isOffline"

    const val CALL_START_TIME = "call_start_time"

    const val OVERDUE = "OVERDUE"
    const val MISSEDVISIT = "MISSED_VISIT"
    const val REDRISK = "RED_RISK"
    const val LTF = "LTF"
    const val CALL_TYPE = "Call_Type"

    /**
     * Unsuccessful call reason
     */
    const val WRONG_NUMBER = "Wrong Number"
    const val UNREACHABLE = "Unreachable"

    /**
     * Not willing to visit reasons
     */
    const val TREATMENT_FROM_OTHER_FACILITY = "Treatment from other facility"
    const val NO_MEDICINE = "No Medicine"
    const val LONG_DISTANCE = "Long Distance"
    const val TRANSPORTATION_AND_UNSUPPLIED_MEDICINE_COST = "Transportation and unsupplied medicine cost."
    const val LONG_WAITING_QUEUE = "Long waiting queue"
    const val MIGRATED_TO_OTHER_PLACE = "Migrated to other places"
    const val DIED = "Died"
    const val COMPLIANCE_TYPE_OTHER = "Other"
}
