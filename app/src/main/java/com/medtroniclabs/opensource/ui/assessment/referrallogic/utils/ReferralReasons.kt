package com.medtroniclabs.opensource.ui.assessment.referrallogic.utils

import com.medtroniclabs.opensource.mappingkey.UnderFiveYearExaminationKeyMapping.DiseaseName.generalDangerSigns
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH

enum class ReferralReasons {
    GeneralDangerSigns,
    Fever,
    Pneumonia,
    Malaria,
    Symptoms,
    Diarrhoea,
    MUAC,
    ANCSigns,
    PNCMotherSigns,
    childhoodVisitSigns,
    PNCNeonateSigns,
    Cough,
    Miscarriage;

    companion object {
        fun aliasOf(value: ReferralReasons): String {
            return when(value) {
                ANCSigns -> RMNCH.ancSignsLabel
                PNCMotherSigns -> RMNCH.pncMotherSignsLabel
                childhoodVisitSigns -> RMNCH.childhoodVisitSignsLabel
                PNCNeonateSigns -> RMNCH.pncNeonateSignsLabel
                GeneralDangerSigns -> generalDangerSigns
                else -> value.name
            }
        }
    }
}