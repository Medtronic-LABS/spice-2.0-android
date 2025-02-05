package com.medtroniclabs.opensource.ui.mypatients.viewmodel

import androidx.lifecycle.ViewModel
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import javax.inject.Inject

class MedicalReviewBaseViewModel @Inject constructor() : ViewModel() {

    val timeOfDeliveryMap= HashMap<String, Any>()
    val timeOfLabourOnsetMap= HashMap<String, Any>()

    val perineumStateMap= HashMap<String, Any>()
    val genderFlow = HashMap<String, Any>()
    val stateOfBaby = HashMap<String, Any>()
    fun getComplaintsList(): ArrayList<ChipViewItemModel> {
        val listItem =  listOf("Headache", "Abdominal Pain", "Difficulty in breathing", "Chest Pain", "Body Swelling", "Joint/Back Ache", "Injury/cut/bruise", "Eye pain/discharge/itchiness/difficulty seeing","Skin rash/itchiness", "Burns", "Genital pain/swelling/discharge/bleeding", "Pain on passing urine", "Toothache/gum swelling", "None", "Other")
        var complaintList = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices){
            complaintList.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Complaints"
                )
            )
        }
        return complaintList
    }

    fun getExamsList(): List<ChipViewItemModel> {
        val listItem = listOf("Eye Exam", "Oral Exam", "Cardiovascular", "Respiratory system", "Abdominal Pelvic", "Foot Exam", "Neurological Exam")
        var examList = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices){
            examList.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Examinations"
                )
            )
        }
        return examList
    }

    fun getNeonateOutcome(): List<ChipViewItemModel> {
        val listItem = listOf("Live birth", "Macerated still birth", "Fresh still birth")
        val neonateOutcomeList = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateOutcomeList.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Neonate"
                )
            )
        }
        return neonateOutcomeList
    }

    fun getSignSymptomsObserved(): ArrayList<ChipViewItemModel> {
        val listItem = listOf("Delayed crying", "Difficulty breathing", "Bread-fed within 1hour", "Still alive after 24 hours" ,"Rescussitated (only shown if delayed and/or difficult breathing present)", "HIV Exposed", "If HIV exposed,nevirapine syrup administered", "Kangaroo mother care initiated")
        val neonateSignsSymptomsObserved = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateSignsSymptomsObserved.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Neonate"
                )
            )
        }
        return neonateSignsSymptomsObserved
    }

    fun getSignSymptomsObservedMother(): ArrayList<ChipViewItemModel> {
        val listItem = listOf("None", "Anemia", "Eclampsia", "other")
        val neonateSignsSymptomsObserved = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateSignsSymptomsObserved.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Mother"
                )
            )
        }
        return neonateSignsSymptomsObserved
    }
    fun getGeneralConditionOfMother(): ArrayList<ChipViewItemModel> {
        val listItem = listOf("Good", "Fair", "Poor", "Very Poor")
        val neonateSignsSymptomsObserved = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateSignsSymptomsObserved.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Mother"
                )
            )
        }
        return neonateSignsSymptomsObserved
    }

    fun getStatusMother(): ArrayList<ChipViewItemModel> {
        val listItem = listOf("Lactation established", "Mother eating normally", "Educated about FP")
        val neonateSignsSymptomsObserved = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateSignsSymptomsObserved.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Mother"
                )
            )
        }
        return neonateSignsSymptomsObserved
    }

    fun getRiskFactor(): List<ChipViewItemModel> {
        val listItem = listOf("Placenta & membranes complete", "Difficult expulsive placenta", "Excessive IPH (>=15mls)", "Post-partum hemorrhage (bleeding>=500mls)")
        val neonateOutcomeList = ArrayList<ChipViewItemModel>()
        for (i in listItem.indices) {
            neonateOutcomeList.add(
                ChipViewItemModel(
                    name = listItem[i],
                    cultureValue = listItem[i],
                    type = "Neonate"
                )
            )
        }
        return neonateOutcomeList
    }
}