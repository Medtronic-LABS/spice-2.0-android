package com.medtroniclabs.opensource.common

import android.provider.ContactsContract

object DefinedParams {
    const val ZERO = "0"

    const val ID = "id"
    const val male = "male"
    const val female = "female"
    const val both = "both"
    const val AT_CHAR = "@"
    const val span_count_1 = 1
    const val span_count_3 = 3
    const val isMemberRegistration = "isMemberRegistration"
    const val MemberID = "memberID"
    const val MenuId = "MenuId"
    const val NAME = "name"
    const val cultureValue = "cultureValue"
    const val CULTURE_VALUE = "cultureValue"
    const val id = "id"
    const val ICCM = "ICCM"
    const val TB = "TB"
    const val No = "No"
    const val Yes = "Yes"
    const val yes = "yes"
    const val Other = "Other"
    const val ENABLED = "enabled"
    const val Username = "username"
    const val Password = "password"
    const val Authorization = "Authorization"
    const val ACTION_SESSION_EXPIRED =
        "${ContactsContract.Directory.PACKAGE_NAME}.action.SL_SESSION_EXPIRED"
    const val SL_SESSION = "sl_session"
    const val HOUSEHOLD_MEMBER_REGISTRATION = "household_member_registration"
    const val DefaultSelectID = -1L
    const val DefaultID = "-1"
    const val DefaultIDLabel = "--Select--"
    const val LIST_LIMIT = 15
    const val PAGE_INDEX = 0
    const val label = "label"
    const val color = "color"
    const val Value = "value"
    const val EnrollmentType = "EnrollmentType"
    const val MenuTitle = "MenuTitle"
    const val PatientId = "PatientId"
    const val ChildPatientId="childPatientId"
    const val TimeOfDelivery = "timeOfDelivery"
    const val TimeOfLabourOnset = "timeOfLabourOnset"
    const val HouseholdHead = "HouseholdHead"
    const val DateOfDelivery="DateOfDelivery"
    const val CHIEF_DOM_CODE_LENGTH = 3
    const val VILLAGE_CODE_LENGTH = 4
    const val PATIENT_NUMBER_LENGTH = 4
    const val StateOfPerineum = "stateOfPerineum"
    const val Tear = "tear"
    const val Episiotomy ="episiotomy"
    const val None ="none"
    const val Gender = "gender"
    const val StateOfBaby = "stateOfBaby"
    const val iccm = "ICCM"
    const val REFERRED = "Referred"
    const val CallResult = "CallResult"
    const val PatientStatus = "PatientStatus"
    const val UnSuccessful = "UnSuccessful"
    const val SUCCESSFUL = "successful"
    const val UNSUCCESSFUL = "unSuccessful"
    const val BaseUrl="base_url"

    const val OnTreatment = "On Treatment"
    const val OnHold = "on-hold"
    const val Active = "active"
    const val PregnancyANC = "PregnancyANC"
    const val PregnancyPNC = "PregnancyPNC"
    const val EncounterId = "encounterId"
    const val ChildEncounterId = "childEncounterId"
    const val IsNeonate="isNeonate"
    const val True = "true"

    const val ExclusiveBreastCondition = "ExclusiveBreastCondition"
    const val BreastCondition = "BreastCondition"
    const val UterusCondition ="UterusCondition"
    const val CordExamination = "CordExamination"
    const val CongenitalDetect = "CongenitalDetect"

    const val TenantId = "tenantId"
    const val FhirId = "fhirId"
    const val RMNCH = "RMNCH"
    const val Frequency = "frequency"
    const val Description = "description"
    const val DisplayOrder = "displayOrder"

    const val SIGN_DIR = "sign"
    const val SIGN_SUFFIX = "_signature"

    const val DOB = "DOB"
    const val LMB = "LMB"
    const val Recovered = "Recovered"
    const val PatientReference = "patientReference"
    const val MemberReference = "memberReference"
    const val ProgramId = "programId"
    const val valueColor = "color"
    const val Above5MedicalReview = "Above5MedicalReview"
    const val Neonate_Birth_Review = "NEONATE_BIRTH_REVIEW"
    const val MotherDeliveryReview="MOTHER_DELIVERY_REVIEW"
    const val ICCMUNDER2MONTHS = "iccm-under-2M"
    const val ICCM_ABOVE_2M_5Y = "iccm-2M-5Y"
    const val PregnancyAncMedicalReview = "pregnancyAncMedicalReview"

    const val Anaemia = "anaemia"
    const val Hiv = "hivRDT"
    const val Cough = "cough"
    const val CoughOrDifficultBreathing = "Cough or difficult breathing"
    const val MalnutritionOrAnaemia = "Malnutrition / Anaemia"
    const val HivAndAids = "HIV / AIDS RDT"
    const val Pregnant = "Pregnant"
    const val Postpartum = "Postpartum"
    const val Lactating = "Lactating"
    const val REFRESH_FRAGMENT = "REFRESH_FRAGMENT"

    const val NeonatePatientIdPrefix = "HM-"
    const val NeonateBabyNamePrefix = "Baby of"
    const val FollowUpId = "FollowUpId"
    const val IsReferredScreen = "isReferredScreen"
    const val SearchLength = 2
    const val OtherNotes = "otherNotes"
    const val Postnatal="Postnatal"

    const val TestedOn = "TestedOn"
    const val Unit = "_unit"
    const val PostNatal="Post Natal"
    const val Neonate="Neonate"
    const val PncHistory="PncHistory"

    val changeFacility = "changeFacility"
    const val VillageList = "VillageList"

    const val passwordRegexPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=\\S+$).{6,64}$"


    const val LowBirthWeight=  2.0
    const val Others_Specify="others"

    const val KeySignature = "KeySignature"
    const val KeyInitial = "KeyInitial"

    const val LastSyncDate = "lastSyncDate"
    const val FollowUpStartTiming= "followUpStartTiming"

    const val houseHoldLinkStartTiming = "houseHoldLinkStartTiming"

    const val BuildConfigs="BuildConfiguration"
    const val HosueHoldHead="HouseholdHead"

    const val Assigned = "Assigned"
    const val UnAssigned = "Unassigned"

    const val FhirMemberID = "FhirmemberID"

    const val name = "name"

    const val Age = "age"

    const val Landing = "landing"
    const val Screening = "screening"
    const val Registration = "registration"
    const val Assessment = "assessment"
    const val MyPatients = "my_patients"
    const val MedicalReview = "medicalReview"
    const val Workflow = "workflow"
    const val BioData = "bioData"
    const val Country = "country"
    const val District = "district"
    const val Chiefdom = "chiefdom"
    const val Village = "village"
    const val Program = "program"
    const val HealthFacilityId = "healthFacilityId"
    const val HealthFacilityFhirId = "healthFacilityFhirId"
    const val Provenance = "provenance"
    const val ORIGIN = "origin"
    const val EMPOWER_HEALTH_NCD = "Empower Health NCD"
    const val NCD_REGISTER = "NCD Register"
    const val PATIENT_ID = "patientId"
    const val RelatedPersonFhirId = "relatedPersonFhirId"
    const val AssessmentOrganizationId = "assessmentOrganizationId"
    const val FORM_TYPE_ID = "formTypeId"
    const val BP_LOG = "bpLog"
    const val GLUCOSE_LOG = "glucoseLog"
    const val DESCRIPTION = "description"
    const val QUANTITY = "quantity"
    const val PRESCRIPTION = "PRESCRIPTION"
    const val Tablet = "Tablet"
    const val Liquid_Oral = "Liquid"
    const val Injection_Injectable_Solution = "Injection"
    const val Capsule = "Capsule"
    const val Africa = "AFRICA"
    const val PatientVisitId = "PatientVisitId"
    const val TYPE_REFILL = "REFILL"
    const val displayValue = "displayValue"
    const val Activity = "Activity"
    const val Title = "Title"
    const val Count = "Count"
    const val SCREENED = "SCREENED"
    const val ASSESSED = "ASSESSED"
    const val ENROLLED = "ENROLLED"
    const val REGISTERED = "REGISTERED"
    const val REFERREDD = "REFERRED"
    const val Dispense = "dispense"
    const val Nutritionlifestyle = "nutritionlifestyle"
    const val Investigation = "investigation"


    const val RedRiskLow = "Low"
    const val RedRiskModerate = "Moderate"
    const val RedRiskHigh = "High"

    const val RiskColorCode = "riskColorCode"
    const val RiskLevel = "riskLevel"
    const val RiskMessage = "riskMessage"

    const val ProvisionalTreatmentPlan = "treatmentPlanResponse"
    const val TreatmentPlan = "treatmentPlan"
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
    const val DirectPNCFlow = "DirectPNCFlow"
    const val LabourDeliveryData="LabourDeliveryData"
    const val NeonateOutcome = "NeonateOutcome"
    const val Status = "status"
    const val Comments = "comments"
    const val YearOfDiagnosis = "yearOfDiagnosis"
    const val MentalHealthDisorder = "mentalHealthDisorder"


    const val Others = "Others"

    const val RED_MAX_MUAC = 11.5
    const val YELLOW_MAX_MUAC = 12.5
    const val GREEN_MAX_MUAC = 26.5

    const val BOLD = "bold"
    const val ITALIC = "italic"
    const val BOLD_ITALIC = "bold_italic"
    const val appType = "appType"

    const val EN_Locale = "English"
    const val SW_Locale = "Swahili"
    const val EN = "en"
    const val SW = "sw"
}