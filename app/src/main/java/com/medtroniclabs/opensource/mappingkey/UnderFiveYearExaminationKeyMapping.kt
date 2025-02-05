package com.medtroniclabs.opensource.mappingkey

object UnderFiveYearExaminationKeyMapping {

    object DiseaseName {
        const val generalDangerSigns = "General Danger Signs"
        const val cough = "Cough / Difficult Breathing"
        const val diarrhoea = "Diarrhoea"
        const val fever = "Fever"
        const val earProblem = "Ear Problem"
        const val anaemia = "Malnutrition / Anemia"
        const val hivRDT = "HIV / AIDS RDT"
    }

    object GeneralDangerSigns {
        const val unableToDrinkOrBreastfeed = "unableToDrinkOrBreastfeed"
        const val vomitingEverything = "vomitingEverything"
        const val historyOfConvulsion = "historyOfConvulsion"
        const val convulsingNow = "convulsingNow"
        const val lethargicOrUnconscious = "lethargicOrUnconscious"
    }

    object Cough {
        const val coughOrDifficultBreathing = "coughOrDIfficultBreathing"
        const val noOfDays = "noOfDays"
        const val chestIndrawing = "chestIndrawing"
        const val stridor = "stridor"
    }

    object Diarrhoea {
        const val hasDiarrhoea = "hasDiarrhoea"
        const val timePeriod = "timePeriod"
        const val bloodyDiarrhoea = "bloodyDiarrhoea"
        const val signs = "diarrhoeaSigns"
    }

    object Fever {
        const val hasFever = "hasFever"
        const val noOfDays = "noOfDays"
        const val isMotherHasFever = "isMotherHasFever"
        const val microscopyResult = " microscopyResult"
        const val signs = "feverSigns"
    }

    object EarProblem {
        const val hasEarPain = "hasEarPain"
        const val noOfDays = "noOfDays"
        const val earDischarge = "earDischarge"
    }

    object MalnutritionAndAnaemia {
        const val appetiteTest = "appetiteTest"
        const val signs = "malnutritionSigns"
    }

    object HivAndAids {
        const val mother = "mother"
        const val child = "child"
    }


}
