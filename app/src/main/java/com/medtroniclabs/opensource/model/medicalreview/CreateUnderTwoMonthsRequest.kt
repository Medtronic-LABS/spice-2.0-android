package com.medtroniclabs.opensource.model.medicalreview

import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter

data class CreateUnderTwoMonthsRequest(
    val id: String? = null,
    val clinicalNotes: String?,
    val clinicalSummaryAndSigns: ClinicalSummaryAndSigns?,
    val examination: Examination?,
    val presentingComplaints: String?,
    val encounter: MedicalReviewEncounter? = null
)


data class ClinicalSummaryAndSigns(
    val albendazole: Boolean? = null,
    val height: Double? = null,
    val heightUnit: String? = null,
    val immunisationStatus: String? = null,
    val respirationRate: List<Int?>? = null,
    val temperature: Int? = null,
    val temperatureUnit: String? = null,
    val vitAForMother: Boolean? = null,
    val breastFeeding: Boolean? = null,
    val exclusiveBreastFeeding: Boolean? = null,
    val waz: Double? = null,
    val weight: Double? = null,
    val weightUnit: String? = null,
    val whz: Double? = null,
    val muacStatus:String? = null,
    val muacInCentimeter: Double? = null
) {
    fun isNotEmpty(): Boolean {
        return albendazole != null
                || height != null
                || heightUnit != null
                || immunisationStatus != null
                || respirationRate != null
                || temperature != null
                || temperatureUnit != null
                || vitAForMother != null
                || waz != null
                || weight != null
                || weightUnit != null
                || whz != null
                || breastFeeding != null
                || exclusiveBreastFeeding != null
                || muacInCentimeter != null
    }
}

data class Examination(
    val breastfeedingProblem: BreastfeedingProblem? = null,
    val diarrhoea: Diarrhoea? = null,
    val hivInfection: Hiv? = null,
    val jaundice: Jaundice? = null,
    val nonBreastfeedingProblem: NonBreastfeedingProblem? = null,
    val verySevereDisease: VerySevereDisease? = null
)

data class BreastfeedingProblem(
    val anyBreastfeedingDifficulty: Boolean? = null,
    val attachment: String? = null,
    val lessThan8BreastfeedIn24hrs: Boolean? = null,
    val mouthUlcersOrThrush: Boolean? = null,
    val noFeedingProblem: String? = null,
    val notIncreasingBFInIllness: Boolean? = null,
    val positioning: String? = null,
    val receivesOtherFoodsOrDrinks: Boolean? = null,
    val suckling: String? = null,
    val switchingBreastFrequently: Boolean? = null,
    val underweight: Boolean? = null
)

data class Diarrhoea(
    val bloodInStool: Boolean? = null,
    val movementOnStimulation: Boolean? = null,
    val noMovementOnStimulation: Boolean? = null,
    val restlessOrIrritable: Boolean? = null,
    val skinPinch: String? = null,
    val sunkenEyes: Boolean? = null,
    val timePeriod: Int? = null
)

data class Jaundice(
    val jaundiceAppearing: Boolean? = null,
    val noJaundice: Boolean? = null,
    val solesNotYellow: Boolean? = null,
    val yellowPalmsAndSoles: Boolean? = null,
    val yellowSkinLessThan24hrs: Boolean? = null
)

data class NonBreastfeedingProblem(
    val bottleFeeding: Boolean? = null,
    val feedFormHIVPositiveMother: Boolean? = null,
    val inappropriateReplacementFeeds: Boolean? = null,
    val incorrectlyPreparedMilk: Boolean? = null,
    val insufficientReplacementFeeds: Boolean? = null,
    val lowWeightForAge: Boolean? = null,
    val thrush: String? = null,
    val useOfFeedingBottle: Boolean? = null
)

data class Hiv(
    val hasPositiveVirologicalTestForInfant: Boolean? = null,
    val isMotherPostiveAndChildNegative: Boolean? = null,
    val hasPositiveAntibodyTestForInfant: Boolean? = null,
    val isMotherPostiveAndInfantNotTested: Boolean? = null,
    val hasNegativeForMotherAndChild: Boolean? = null
)

data class VerySevereDisease(
    val convulsions: Boolean? = null,
    val lowBodyTemperature: Boolean? = null,
    val movementOnStimulation: Boolean? = null,
    val severeChestIndrawing: Boolean? = null,
    val skinPustules: Boolean? = null,
    val stoppedFeeding: Boolean? = null,
    val umbilicusRedOrDrainingPus: Boolean? = null
)