package org.medtroniclabs.uhis.ui.assessment

import android.content.Context
import org.medtroniclabs.uhis.R

object FamilyPlanning {
    val DesireForChildren by lazy {
        listOf(
            "yesWithin2Yrs",
            "yesAfter2Yrs",
            "noMoreChildren",
            "unsure",
        )
    }

    val FamilyPlanningMethods by lazy {
        listOf(
            "pills",
            "injectables",
            "implant",
            "iud",
            "condoms",
            "sterilizationFemale",
            "sterilizationMale",
            "none",
        )
    }

    /**
     * Returns the recommended family planning method counseling message based on the
     * client's desire for children and number of living children, or `null` when the
     * client uses a sterilization method or no recommendation applies.
     */
    fun getRecommendedFamilyPlanningMethod(
        context: Context,
        desireForChildren: String?,
        noOfChildren: Int,
        selectedMethod: String?,
    ): String? {
        if (AssessmentDefinedParams.FP_METHOD_STERILIZATION_MALE.equals(selectedMethod, true) ||
            AssessmentDefinedParams.FP_METHOD_STERILIZATION_FEMALE.equals(selectedMethod, true)
        ) {
            return null
        }
        return when {
            FamilyPlanning.DesireForChildren[0].equals(desireForChildren, true) ||
                (FamilyPlanning.DesireForChildren[3].equals(desireForChildren, true) && noOfChildren == 0) ->
                context.getString(R.string.short_acting_message)

            FamilyPlanning.DesireForChildren[2].equals(desireForChildren, true) ||
                (FamilyPlanning.DesireForChildren[3].equals(desireForChildren, true) && noOfChildren >= 2) ->
                context.getString(R.string.permanent_acting_message)

            FamilyPlanning.DesireForChildren[1].equals(desireForChildren, true) ||
                (FamilyPlanning.DesireForChildren[3].equals(desireForChildren, true) && noOfChildren >= 1) ->
                context.getString(R.string.long_acting_message)

            else -> null
        }
    }
}
