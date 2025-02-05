package com.medtroniclabs.opensource.mappingkey

object UnderTwoExaminationKeyMapping {

    object DiseaseName {
        const val verySevereDisease = "Very Severe Disease (PSBI)"
        const val jaundice = "Jaundice"
        const val diarrhoea = "Diarrhoea"
        const val breastFeeding = "Feeding Problem (For all Breastfeeding)"
        const val hiv = "Check for HIV Infection"
        const val nonBreastFeeding = "Feeding Problem (For all Non-Breastfeeding)"
    }

    object Hiv {
        const val hasPositiveVirologicalTestForInfant = "hasPositiveVirologicalTestForInfant"
        const val isMotherPostiveAndChildNegative = "isMotherPostiveAndChildNegative"
        const val hasPositiveAntibodyTestForInfant = "hasPositiveAntibodyTestForInfant"
        const val isMotherPostiveAndInfantNotTested = "isMotherPostiveAndInfantNotTested"
        const val hasNegativeForMotherAndChild = "hasNegativeForMotherAndChild"
    }

    object VerySevereDisease {
        const val stoppedFeeding = "stoppedFeeding"
        const val convulsion = "convulsions"
        const val severeChestIndrawing = "severeChestIndrawing"
        const val movementOnStimulation = "movementOnStimulation"
        const val lowBodyTemperature = "lowBodyTemperature"
        const val skinPustules = "skinPustules"
        const val umbilicusRedOrDrainingPus = "umbilicusRedOrDrainingPus"
    }

    object Jaundice {
        const val yellowSkinLessThan24hrs = "yellowSkinLessThan24hrs"
        const val yellowPalmsAndSoles = "yellowPalmsAndSoles"
        const val jaundiceAppearing = "jaundiceAppearing"
        const val solesNotYellow = "solesNotYellow"
        const val noJaundice = "noJaundice"
    }

    object Diarrhoea {
        const val timePeriod = "timePeriod"
        const val bloodInStool = "bloodInStool"
        const val movementOnStimulation = "movementOnStimulation"
        const val noMovementOnStimulation = "noMovementOnStimulation"
        const val restlessOrIrritable = "restlessOrIrritable"
        const val sunkenEyes = "sunkenEyes"
        const val skinPinch = "skinPinch"
    }

    object BreastFeedingProblem {
        const val anyBreastfeedingDifficulty = "anyBreastfeedingDifficulty"
        const val lessThan8BreastfeedIn24hrs = "lessThan8BreastfeedIn24hrs"
        const val switchingBreastFrequently = "switchingBreastFrequently"
        const val notIncreasingBFInIllness = "notIncreasingBFInIllness"
        const val receivesOtherFoodsOrDrinks = "receivesOtherFoodsOrDrinks"
        const val mouthUlcersOrThrush = "mouthUlcersOrThrush"
        const val underweight = "underweight"
        const val positioning = "positioning"
        const val attachment = "attachment"
        const val suckling = "suckling"
        const val noFeedingProblem = "noFeedingProblem"
    }

    object NonBreastFeedingProblem {
        const val inappropriateReplacementFeeds = "inappropriateReplacementFeeds"
        const val insufficientReplacementFeeds = "insufficientReplacementFeeds"
        const val incorrectlyPreparedMilk = "incorrectlyPreparedMilk"
        const val useOfFeedingBottle = "useOfFeedingBottle"
        const val feedFormHIVPositiveMother = "feedFormHIVPositiveMother"
        const val bottleFeeding = "bottleFeeding"
        const val lowWeightForAge = "lowWeightForAge"
        const val thrush = "thrush"
    }
}