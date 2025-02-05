package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter

data class CreateUnderFiveYearsRequest(
    val id:String?=null,
    val clinicalNotes: String? = null,
    val clinicalSummaryAndSigns: ClinicalSummaryAndSigns? = null,
    val examination: UnderFiveExamination? = null,
    val presentingComplaints: String? = null,
    val encounter: MedicalReviewEncounter? = null,
    var systemicExamination: List<String?>? = null,
    var systemicExaminationNotes: String? = null
)

data class UnderFiveExamination(
    val generalDangerSigns: GeneralDangerSings? = null,
    val diarrhoea: UnderFiveDiarrhoea? = null,
    val cough: CoughOrDifficultBreathing? = null,
    val fever: Fever? = null,
    val earProblem: EarProblem? = null,
    val anaemia: MalnutritionOrAnaemia? = null,
    val hivRDT: HivAndAids? = null
)

data class UnderFiveDiarrhoea(
    val hasDiarrhoea: Boolean? = null,
    val bloodyDiarrhoea: Boolean? = null,
    val signs: List<String?>? = null,
    val timePeriod: Int? = null
)

data class GeneralDangerSings(
    val unableToDrinkOrBreastfeed: Boolean? = null,
    val historyOfConvulsion: Boolean? = null,
    val lethargicOrUnconscious: Boolean? = null,
    val vomitingEverything: Boolean? = null,
    val convulsingNow: Boolean? = null
)

data class CoughOrDifficultBreathing(
    val coughOrDIfficultBreathing: Boolean? = null,
    val chestIndrawing: Boolean? = null,
    val stridor: Boolean? = null,
    val noOfDays: Int? = null
)

data class Fever(
    val hasFever: Boolean? = null,
    val isMotherHasFever: String? = null,
    val signs: List<String>? = null,
    var microscopyResult: String? = null,
    val noOfDays: Int? = null
)

data class EarProblem(
    val noOfDays: Int? = null,
    val hasEarPain: Boolean? = null,
    val earDischarge: String? = null
)

data class MalnutritionOrAnaemia(
    val appetiteTest: Boolean? = null,
    val signs: List<String?>? = null
)

data class HivAndAids(
    val mother: String? = null,
    val child: String? = null
)