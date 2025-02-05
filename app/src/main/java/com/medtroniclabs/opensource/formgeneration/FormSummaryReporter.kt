package com.medtroniclabs.opensource.formgeneration


import android.content.Context
import android.content.ContextWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.CardLayoutBinding
import com.medtroniclabs.opensource.databinding.SummaryLayoutBinding
import com.medtroniclabs.opensource.formgeneration.config.DefinedParams
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_AGE
import com.medtroniclabs.opensource.formgeneration.config.ViewType.VIEW_TYPE_FORM_BP
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.mappingkey.Screening
import kotlin.math.roundToInt

class FormSummaryReporter(
    var context: Context,
    private val parentLayout: LinearLayout,
) : ContextWrapper(context) {

    private val rootSuffix = "rootView"
    private val resultRootSuffix = "resultRootView"
    private val bioDataRootSuffix = "bioRootView"
    private var systolicAverageSummary: Int? = null
    private var diastolicAverageSummary: Int? = null

    fun populateSummary(
        serverData: List<FormLayout>,
        resultMap: Map<String, Any>,
        translate: Boolean = false
    ) {
        parentLayout.removeAllViews()
        parentLayout.addView(initialCardLayout(getString(R.string.bio_data), bioDataRootSuffix))
        parentLayout.addView(initialCardLayout(getString(R.string.result), resultRootSuffix))
        serverData.forEach { formLayout ->
            formLayout.apply {
                isSummary?.let {
                    if (it) {
                        when (viewType) {
                            VIEW_TYPE_FORM_AGE -> createAgeSummaryView(
                                serverData,
                                formLayout,
                                resultMap
                            )

                            VIEW_TYPE_FORM_BP -> createBPSummaryView(
                                serverData,
                                formLayout,
                                resultMap
                            )

                            else -> createSummaryView(formLayout, resultMap, translate)
                        }
                    }
                }
            }
        }
    }

    fun populateAssessmentSummary(
        serverData: List<FormLayout>,
        resultMap: Map<String, Any>,
        translate: Boolean = false
    ) {
        parentLayout.removeAllViews()
        parentLayout.addView(initialCardLayout(getString(R.string.result), resultRootSuffix))
    }

    private fun getFamilyView(family: String?): LinearLayout? {
        family ?: return null
        return parentLayout.findViewWithTag(family)
    }

    private fun createBPSummaryView(
        serverData: List<FormLayout>,
        formLayout: FormLayout,
        resultMap: Map<String, Any>
    ) {
        formLayout.apply {
            calculateAverageBloodPressure(serverData, id, resultMap)
            val binding = SummaryLayoutBinding.inflate(LayoutInflater.from(context))
            binding.root.tag = id + rootSuffix
            binding.tvKey.text = getString(R.string.average_blood_pressure)
            binding.tvValue.text =
                getString(
                    R.string.average_mmhg_string,
                    systolicAverageSummary.toString(),
                    diastolicAverageSummary.toString()
                )
            parentLayout.findViewWithTag<LinearLayout>(getFormResultView())
                ?.addView(binding.root)
        }
    }

    private fun calculateAverageBloodPressure(
        serverData: List<FormLayout>,
        id: String,
        resultMap: Map<String, Any>
    ) {
        var systolic = 0.0
        var diastolic = 0.0
        var enteredCount = 0
        systolicAverageSummary = systolic.toInt()
        diastolicAverageSummary = diastolic.toInt()
        FormResultComposer.findGroupIdForNCD(serverData, id)?.let {
            val subMap = resultMap[it] as Map<String, Any>
            if (subMap.containsKey(id)) {
                val actualMapList = subMap[id]
                if (actualMapList is ArrayList<*>) {
                    actualMapList.forEach { map ->
                        val sys = getMapValue(map, Screening.Systolic)
                        val dia = getMapValue(map, Screening.Diastolic)
                        if (sys > 0 && dia > 0) {
                            enteredCount++
                            systolic += sys
                            diastolic += dia
                        }
                    }
                    systolicAverageSummary = (systolic / enteredCount).roundToInt()
                    diastolicAverageSummary = (diastolic / enteredCount).roundToInt()
                }
            }
        }
    }

    private fun getMapValue(map: Any?, value: String): Double {
        var d = 0.0
        map?.let {
            if (it is Map<*, *> && it.containsKey(value))
                d = (it[value] as? String?)?.toDoubleOrNull() ?: it[value] as Double
        }
        return d
    }

    private fun createAgeSummaryView(
        serverData: List<FormLayout>,
        formLayout: FormLayout,
        resultMap: Map<String, Any>
    ) {
        formLayout.apply {
            FormResultComposer.findGroupIdForNCD(serverData, id)?.let {
                val subMap = resultMap[it] as Map<String, Any>
                if (subMap.containsKey(id)) {
                    val actualValue = subMap[id]
                    if (actualValue is Map<*, *> && actualValue.containsKey(Screening.DateOfBirth)) {
                        val ageInYears = actualValue[Screening.DateOfBirth]
                        if (ageInYears is Number) {
                            val binding =
                                SummaryLayoutBinding.inflate(LayoutInflater.from(context))
                            binding.tvKey.text = getString(R.string.age)
                            binding.tvValue.text = ageInYears.toInt().toString()
                            getFamilyView(bioDataRootSuffix)
                                ?.addView(binding.root)
                        }
                    }
                }
            }
        }
    }

    private fun initialCardLayout(title: String, tag: String): View {
        val binding = CardLayoutBinding.inflate(LayoutInflater.from(context))
        binding.llFamilyRoot.tag = tag
        binding.cardTitle.text = title
        return binding.root
    }


    private fun createSummaryView(
        formLayout: FormLayout,
        resultMap: Map<String, Any>,
        translate: Boolean
    ) {
        formLayout.apply {
            if (resultMap.containsKey(family)) {
                val subMap = resultMap[family] as Map<String, Any>
                if (subMap.containsKey(id)) {
                    val binding = SummaryLayoutBinding.inflate(LayoutInflater.from(context))
                    binding.root.tag = id + rootSuffix
                    if (translate) {
                        binding.tvKey.text = titleCulture ?: title
                    } else {
                        binding.tvKey.text = title
                    }
                    binding.tvValue.text = getActualValue(subMap[id], optionsList)
                    getFamilyView(bioDataRootSuffix)?.addView(binding.root)
                }
            }
        }
    }

    private fun getActualValue(value: Any?, optionsList: ArrayList<Map<String, Any>>?): String {
        if (optionsList != null) {
            optionsList.let { list ->
                list.forEach { map ->
                    getActualName(map, value)?.let {
                        return it
                    }
                }
            }
        } else {
            when (value) {
                is String -> return if (Screening.national_id == value || Screening.passport == value || Screening.birthCertificate == value) {
                    CommonUtils.getIdentityDisplayName(value)
                } else value

                is Map<*, *> -> {
                    getActual(value)?.let {
                        return it
                    }
                }

                is ArrayList<*> -> {
                    value.forEach { map ->
                        CommonUtils.getListActual(map)?.let {
                            return it
                        }
                    }
                }

                else -> {
                    //Else block execution
                }
            }
        }
        return ""
    }

    private fun getActual(value: Map<*, *>): String? {
        if (value.containsKey(DefinedParams.NAME)) {
            val actual = value[DefinedParams.NAME]
            if (actual is String) {
                return actual
            }
        }
        return null
    }

    private fun getActualName(map: Map<String, Any>, value: Any?): String? {
        if (map.containsKey(DefinedParams.ID)) {
            val id = map[DefinedParams.ID]
            id?.let {
                if (it == value)
                    return  map[DefinedParams.cultureValue] as? String ?: map[DefinedParams.NAME] as String
            }
        }
        return null
    }

    fun getFormResultView(): String {
        return resultRootSuffix
    }
}