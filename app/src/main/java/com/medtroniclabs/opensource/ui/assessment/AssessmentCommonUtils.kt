package com.medtroniclabs.opensource.ui.assessment

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams.No
import com.medtroniclabs.opensource.common.DefinedParams.Yes
import com.medtroniclabs.opensource.databinding.AssessmentSummaryLayoutBinding
import com.medtroniclabs.opensource.model.AssessmentSummaryModel

object AssessmentCommonUtils {
    fun getListItemValue(
        givenId: String,
        listSummaryData: MutableList<AssessmentSummaryModel>
    ): AssessmentSummaryModel? {
        return listSummaryData.find { it.id == givenId }
    }

    fun getValueOfKeyFromMap(stringToMap: Map<String, Any>, keys: String, menuType: String): String? {
        val mapDataByType = stringToMap[menuType.lowercase()] as Map<*, *>
        for (entry in mapDataByType.entries) {
            if (entry.value is Map<*, *>) {
                val nestedMap = entry.value as? Map<*, *>
                if (nestedMap?.containsKey(keys) == true) {
                    if (nestedMap[keys] is String) {
                        return nestedMap[keys] as String
                    }
                    if (nestedMap[keys] is Double) {
                        return nestedMap[keys].toString()
                    }
                    if (nestedMap[keys] is Boolean) {
                        return if (nestedMap[keys] == true) Yes else No
                    }
                    if (nestedMap[keys] is ArrayList<*>) {
                        val arrayListValue = nestedMap[keys] as ArrayList<*>
                        return getDialogValue(arrayListValue)
                    }
                }
            }
        }
        return null
    }

    private fun getDialogValue(value: Any?, otherSymptoms: String? = null): String {
        val result = StringBuilder()
        if (value is ArrayList<*>) {
            value.forEach { map ->
                CommonUtils.getListActual(map)?.let {
                    result.append(it)
                    result.append(", ")
                }
            }
        }
        if (result.isNotEmpty()) {
            otherSymptoms?.let {
                return result.delete(result.length - 2, result.length).append(" - ").append(it)
                    .toString()
            }
            return result.delete(result.length - 2, result.length).toString()
        }
        return ""
    }

    fun addViewSummaryLayout(title: String?, value: String?, valueTextColor: Int? = null, context:Context): ConstraintLayout {
        val summaryBinding = AssessmentSummaryLayoutBinding.inflate(LayoutInflater.from(context))
        summaryBinding.tvKey.text = title ?: context.getString(R.string.separator_hyphen)
        summaryBinding.tvValue.text = value ?: context.getString(R.string.separator_hyphen)
        valueTextColor?.let {
            summaryBinding.tvValue.setTextColor(it)
            summaryBinding.tvValue.typeface = Typeface.defaultFromStyle(Typeface.BOLD)
        }
        return summaryBinding.root
    }

    fun getNutritionStatus(selectedId: String?, context: Context): String {
        return when (selectedId) {
            AssessmentDefinedParams.Green -> context.getString(R.string.normal)
            AssessmentDefinedParams.Yellow -> context.getString(R.string.moderate_malnutrition)
            AssessmentDefinedParams.Red -> context.getString(R.string.severe_nutrition)
            else -> context.getString(R.string.hyphen_symbol)
        }
    }
    fun getMuacColorCode(selectedId: String?, context: Context): Int {
        return when (selectedId) {
            AssessmentDefinedParams.Green -> context.getColor( R.color.bmi_normal_weight)
            AssessmentDefinedParams.Yellow -> context.getColor( R.color.bmi_over_weight)
            AssessmentDefinedParams.Red ->context.getColor( R.color.medium_high_risk_color)
            else ->context.getColor( R.color.edittext_stroke)
        }
    }
}