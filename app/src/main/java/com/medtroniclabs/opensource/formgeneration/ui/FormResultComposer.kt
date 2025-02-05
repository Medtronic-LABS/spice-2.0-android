package com.medtroniclabs.opensource.formgeneration.ui

import android.content.Context
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.mappingkey.Screening

class FormResultComposer {

    private var groupedResultMap: HashMap<String, Any> = HashMap()

    fun groupValues(
        context: Context,
        serverData: List<FormLayout?>,
        resultMap: HashMap<String, *>,
        menuType: String? = null,
        bmiCategoryGroupId: String? = null
    ): Pair<String?, HashMap<String, Any>> {
        val customWorkflowList = ArrayList<Pair<String,Double?>>()
        serverData.forEach { serverViewModel ->
            if (serverViewModel?.isCustomWorkflow == true) {
                serverViewModel.id.let { cWorkflowId ->
                    if (cWorkflowId.isNotBlank())
                        customWorkflowList.add(Pair(cWorkflowId, serverViewModel.customizedWorkflowId))
                }
            }
            when (serverViewModel?.viewType) {
                ViewType.VIEW_TYPE_FORM_CARD_FAMILY -> createGroup(serverViewModel.id)
                else -> {
                    addToGroup(
                        serverViewModel?.family,
                        serverViewModel?.id!!,
                        resultMap[serverViewModel.id]
                    )
                    resultMap.remove(serverViewModel.id)
                }
            }
        }

        for (key in resultMap.keys) {
            groupResultsForNCD(serverData, key, resultMap[key])
        }

        if (customWorkflowList.size > 0) {
            val list = ArrayList<Any>()
            customWorkflowList.forEach { entryMap ->
                if (groupedResultMap.containsKey(entryMap.first)) {
                    groupedResultMap.remove(entryMap.first)?.let { value ->
                        if (value is Map<*, *> && value.isNotEmpty()) {
                            val map = HashMap<String, Any>()
                            map[entryMap.first] = value
                            map[DefinedParams.id] = entryMap.second ?: 0.0
                            list.add(map)
                        }
                    }
                }
            }
            if (list.isNotEmpty())
                groupedResultMap[Screening.CustomizedWorkflows] = list
        }

        if (groupedResultMap.containsKey(Screening.PCMentalHealth))
            (groupedResultMap[Screening.PCMentalHealth] as? HashMap<*, *>)?.let { pcMap ->
                val pcMH = HashMap<String, Any>()
                pcMH[Screening.PCMentalHealth] = pcMap
                (groupedResultMap[Screening.PIScore] as? String?)?.let { score ->
                    pcMH[Screening.PIScore] = score
                }
                groupedResultMap.remove(Screening.PIScore)
                groupedResultMap.remove(Screening.PCMentalHealth)
                groupedResultMap[Screening.PHQ4.lowercase()] = pcMH
            }

        bmiCategoryGroupId?.let { groupId ->
            if (groupedResultMap.containsKey(Screening.BMI_CATEGORY) && groupedResultMap.containsKey(
                    groupId
                )
            ) {
                (groupedResultMap[groupId] as? HashMap<String, Any>)?.let {
                    it[Screening.BMI_CATEGORY] =
                        groupedResultMap[Screening.BMI_CATEGORY] as String
                    groupedResultMap.remove(Screening.BMI_CATEGORY)
                }
            }
        }

        return Pair(
            StringConverter.convertGivenMapToString(groupedResultMap),
            addToMenuGroup(groupedResultMap, menuType)
        )
    }

    fun addToMenuGroup(
        groupedResultMap: HashMap<String, Any>,
        menuType: String?
    ): HashMap<String, Any> {
        val menuGroupMap = HashMap<String, Any>()
        menuType?.let {
            menuGroupMap[menuType] = groupedResultMap
            return menuGroupMap
        }
        val (valueHashMap, valueNotHashMap) = groupedResultMap.entries.partition { it.value is HashMap<*, *> }
        val validValueHashMap = valueHashMap.filter {
            val value = it.value
            value is HashMap<*, *> && value.isNotEmpty()
        }
        return HashMap((validValueHashMap + valueNotHashMap).associate { it.toPair() })
    }

    private fun createGroup(id: String) {
        val tempMap = HashMap<String, Any>()
        if (!groupedResultMap.containsKey(id))
            groupedResultMap[id] = tempMap
    }

    private fun addToGroup(family: String?, id: String, any: Any?) {

        family?.let {
            if (!groupedResultMap.containsKey(it))
                createGroup(it)
            val subMap = groupedResultMap[it] as HashMap<String, Any>
            any?.let { value ->
                subMap.put(id, value)
            }
        }
    }

    private fun groupResultsForNCD(serverData: List<FormLayout?>, id: String, any: Any?) {

        val familyGroup = findGroupIdForNCD(serverData, id)

        var subMap: HashMap<String, Any>? = null

        if (familyGroup != null) {
            if (!groupedResultMap.containsKey(familyGroup))
                createGroup(familyGroup)
            subMap = groupedResultMap[familyGroup] as HashMap<String, Any>?
        }

        any?.let { value ->
            if (subMap != null)
                subMap.put(id, value)
            else groupedResultMap.put(id, value)
        }

    }

    companion object {
        fun findGroupIdForNCD(serverData: List<FormLayout?>, id: String): String? {
            val baseId = when (id) {
                Screening.BMI -> Screening.Height
                Screening.Glucose_Value, Screening.Glucose_Type, Screening.HbA1c_Date_Time, Screening.Glucose_Date_Time, Screening.GlucoseId, Screening.GlucoseUnit, Screening.GlucoseLogId, Screening.BGTakenOn -> Screening.BloodGlucoseID
                Screening.MentalHealthDetails, Screening.PHQ4_Result, Screening.PHQ4_Score, Screening.RiskLevel -> Screening.MentalHealthDetails
                Screening.PHQ9_Result, Screening.PHQ9_Score, Screening.PHQ9_Risk_Level -> Screening.PHQ9_Mental_Health
                Screening.GAD7_Score, Screening.GAD7_Risk_Level -> Screening.GAD7_Mental_Health
                Screening.Avg_Blood_pressure, Screening.Avg_Pulse, Screening.Avg_Systolic, Screening.Avg_Diastolic, Screening.bp_log_id, Screening.BPTakenOn -> Screening.BPLog_Details
                Screening.Initial -> Screening.First_Name
                Screening.isDiabetesDiagnosis -> Screening.DiabetesPatientType
                Screening.diabetes -> Screening.diabetes
                else -> {
                    if (id.endsWith(Screening.unitMeasurement_KEY)) {
                        val parts = id.split(Screening.unitMeasurement_KEY)
                        if (parts.isNotEmpty()) {
                            parts[0]
                        } else {
                            id
                        }
                    } else {
                        id
                    }
                }
            }
            return serverData.find { it?.id == baseId }?.family
        }
    }
}