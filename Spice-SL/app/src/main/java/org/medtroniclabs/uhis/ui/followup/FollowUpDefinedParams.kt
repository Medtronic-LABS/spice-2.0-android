package org.medtroniclabs.uhis.ui.followup

import org.medtroniclabs.uhis.ui.MenuConstants

object FollowUpDefinedParams {
    const val FILTER_TODAY = "Today"
    const val FILTER_TOMORROW = "Tomorrow"
    const val FILTER_CUSTOMIZE = "Customize"

    const val FU_TYPE_HH_VISIT = "HH_VISIT"
    const val FU_TYPE_REFERRED = "REFERRED"
    const val FU_TYPE_MEDICAL_REVIEW = "MEDICAL_REVIEW"
    const val FU_ENCOUNTER_TYPE_RMNCH = "RMNCH"

    const val WRONG_NUMBER = "WRONG_NUMBER"

    const val FILTER_ANC = MenuConstants.ANC
    const val FILTER_PNC = MenuConstants.PNC_MOTHER
    const val FILTER_NCD = MenuConstants.NCD_MENU_ID
    const val FILTER_CHILD_HEALTH = MenuConstants.CHILDHOOD_VISIT

    const val HIGH_BP = "high bp"
    const val HIGH_BG = "high bg"
    const val BOTH = "both"
}
