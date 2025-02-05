package com.medtroniclabs.opensource.formgeneration.utility

import android.content.Context
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.formgeneration.model.InformationModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.chestInDrawing
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.hasOedemaOfBothFeet
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isBreastfeed
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isConvulsionPastFewDays
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isUnusualSleepy
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.isVomiting
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams.muacCode

class InformationUtils {

    fun getMuacInformationListItem(context: Context): ArrayList<InformationModel> {
        val informationList = ArrayList<InformationModel>()

        informationList.add(
            InformationModel(
                inputText = context.getString(R.string.muac_info_1),
                type = muacCode
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.measuring_muac_1,
                inputText = context.getString(R.string.muac_info_2),
                type = muacCode
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.measuring_muac_2,
                inputText = context.getString(R.string.muac_info_3),
                type = muacCode
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.measuring_muac_3,
                inputText = context.getString(R.string.muac_info_4),
                type = muacCode
            )
        )
        informationList.add(
            InformationModel(
                inputText = context.getString(R.string.muac_info_5),
                type = muacCode
            )
        )
        informationList.add(
            InformationModel(
                inputText = context.getString(R.string.muac_info_6),
                type = muacCode
            )
        )
        return informationList
    }

    fun getOedemaInformationList(context: Context): ArrayList<InformationModel>{
        val informationList = ArrayList<InformationModel>()

        informationList.add(
            InformationModel(
                imageId = R.drawable.oedema_1,
                inputText = context.getString(R.string.oedema_info_1),
                type = hasOedemaOfBothFeet
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.oedema_2,
                inputText = context.getString(R.string.oedema_info_2),
                type = hasOedemaOfBothFeet
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.oedema_3,
                inputText = context.getString(R.string.oedema_info_3),
                type = hasOedemaOfBothFeet
            )
        )
        return informationList
    }

    fun getChestIndrawingInformation(context: Context) : ArrayList<InformationModel>{
        val informationList = ArrayList<InformationModel>()

        informationList.add(
            InformationModel(
                imageId = R.drawable.chest_in_drawing_1,
                inputText = context.getString(R.string.chest_info_1),
                type = chestInDrawing
            )
        )
        informationList.add(
            InformationModel(
                imageId = R.drawable.chest_in_drawing_2,
                inputText = context.getString(R.string.chest_info_2),
                type = chestInDrawing
            )
        )
        return informationList
    }

    fun getFastBreathingInstructions(context: Context): ArrayList<String> {
        val informationList = ArrayList<String>()

        informationList.add(
            context.getString(R.string.fb_info_1)
        )
        informationList.add(
            context.getString(R.string.fb_info_2)
        )
        informationList.add(
            context.getString(R.string.fb_info_3)
        )
        informationList.add(
            context.getString(R.string.fb_info_4)
        )
        informationList.add(
            context.getString(R.string.fb_info_5)
        )
        informationList.add(
            context.getString(R.string.fb_info_6)
        )

        return informationList

    }
    fun getDangerSignsInstructions(context: Context, id: String?): ArrayList<InformationModel> {
        val informationList = ArrayList<InformationModel>()
        when (id) {
            isUnusualSleepy -> {
                informationList.add(
                    InformationModel(
                        imageId = R.drawable.ic_unusefulsleep,
                        inputText = context.getString(R.string.ask_is_the_child_very_sleepy),
                        type = isUnusualSleepy
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.clap_your_hands_close_to_the_child),
                        type = isUnusualSleepy
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.if_the_child_does_not),
                        type = isUnusualSleepy
                    )
                )
            }

            isVomiting -> {
                informationList.add(
                    InformationModel(
                        imageId = R.drawable.ic_isvomiting,
                        inputText = context.getString(R.string.ask_does_the_child_vomit_everything_he_she_eats_or_drinks),
                        type = isVomiting
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.give_the_child_clean_water_or_ask_the_mother_to_offer_her_breast_to_the_child),
                        type = isVomiting
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.if_the_child_vomits_the_water_or_breastmilk_refer_to_the_phu_immediately),
                        type = isVomiting
                    )
                )
            }

            isConvulsionPastFewDays -> {
                informationList.add(
                    InformationModel(
                        imageId = R.drawable.ic_convulsions,
                        inputText = context.getString(R.string.look_is_the_child_convulsing_now),
                        type = isConvulsionPastFewDays
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.ask_did_the_child_have_convulsions_at_home),
                        type = isConvulsionPastFewDays
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.if_the_child_is_convulsing_now_or_if_the_child_had_convulsions_at_home_refer_to_the_phu_immediately),
                        type = isConvulsionPastFewDays
                    )
                )
            }

            isBreastfeed -> {
                informationList.add(
                    InformationModel(
                        imageId = R.drawable.ic_unablebreastfeed,
                        inputText = context.getString(R.string.ask_if_the_child_is_able_to_drink_or_breastfeed),
                        type = isBreastfeed
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.ask_did_the_child_have_convulsions_at_home),
                        type = isBreastfeed
                    )
                )
                informationList.add(
                    InformationModel(
                        inputText = context.getString(R.string.if_the_child_is_not_able_to_drink_or_breastfeed_refer_to_the_phu_immediately),
                        type = isBreastfeed
                    )
                )
            }
        }
        return informationList
    }

}