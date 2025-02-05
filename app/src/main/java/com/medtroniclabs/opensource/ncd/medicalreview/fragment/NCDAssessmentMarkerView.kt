package com.medtroniclabs.opensource.ncd.medicalreview.fragment

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil

class NCDAssessmentMarkerView(
    private val context: Context? = null,
    layoutResource: Int,
    private val systolicXYValues: ArrayList<Entry>?,
    private val diastolicXYValues: ArrayList<Entry>?,
    private val hbA1CXYValues: ArrayList<Entry>?,
    private val selectedDropDown: Int,
    private val dateValues: ArrayList<Triple<Int, String, String>>?,
    private val unitValue: ArrayList<Pair<Int, String?>>?,
    private val graphType: String? = null
) : MarkerView(context, layoutResource) {
    private var tvSystolic: TextView = findViewById(R.id.tvSystolic)
    private var tvDiastolic: TextView = findViewById(R.id.tvDiastolic)
    private var tvPulse: TextView = findViewById(R.id.tvPulse)
    private var tvHbA1C: TextView = findViewById(R.id.tvHbA1c)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        e?.let {
            val unitSuffix = getUnitSuffix(unitValue?.filter { e.x == it.first.toFloat() })
            setSystolicValue(it, unitSuffix)
            setDiastolicValue(it, unitSuffix)
            setHba1cValue(it, unitSuffix)
            setDateValues(it)
        }
        super.refreshContent(e, highlight)
    }

    private fun setHba1cValue(entry: Entry, unitSuffix: String?) {
        tvHbA1C.gone()
        val hbA1CValue = hbA1CXYValues?.filter { entry.x == it.x }
        if (!hbA1CValue.isNullOrEmpty()) {
            val title = when (graphType) {
                NCDMRUtil.bg -> NCDMRUtil.HbA1c
                NCDMRUtil.bp -> ""
                else -> {
                    null
                }
            }
            tvHbA1C.text = context?.getString(
                R.string.diastolic_formatted,
                title.takeIf { !it.isNullOrBlank() } ?: "",
                hbA1CValue[0].y.toString(),
                unitSuffix ?: ""
            )?.trim() ?: "-"
            tvHbA1C.visible()
        }
    }

    private fun setSystolicValue(entry: Entry, unitSuffix: String?) {
        val systolicValue = systolicXYValues?.filter { entry.x == it.x }
        val unit = if (graphType == NCDMRUtil.bp) unitSuffix else null
        if (!systolicValue.isNullOrEmpty()) {
            val title = when (graphType) {
                NCDMRUtil.bp -> "Sys"
                NCDMRUtil.bg -> NCDMRUtil.RBS
                else -> {
                    ""
                }
            }
            tvSystolic.text = context?.getString(
                R.string.systolic_formatted,
                title,
                systolicValue[0].y.toString(),
                unit ?: ""
            ) ?: "-"
        }
    }

    private fun setDiastolicValue(entry: Entry, unitSuffix: String?) {
        val diastolicValue = diastolicXYValues?.filter { entry.x == it.x }
        val unit = if (graphType == NCDMRUtil.bp) unitSuffix else null
        if (!diastolicValue.isNullOrEmpty()) {
            val title = when (graphType) {
                NCDMRUtil.bp -> "Dia"
                NCDMRUtil.bg -> NCDMRUtil.FBS
                else -> {
                    ""
                }
            }
            tvDiastolic.text =
                context?.getString(
                    R.string.diastolic_formatted,
                    title,
                    diastolicValue[0].y.toString(),
                    unit ?: ""
                ) ?: "-"
        }
    }

    private fun setDateValues(entry: Entry) {
        dateValues?.filter { entry.x == it.first.toFloat() }?.firstOrNull {
            if (selectedDropDown == 4)
                NCDMRUtil.HbA1c == it.third
            else
                NCDMRUtil.HbA1c != it.third
        }?.let { item ->
            tvPulse.text = item.second

            if (graphType == NCDMRUtil.bg) {
                when (item.third) {
                    NCDMRUtil.FBS -> {
                        tvSystolic.gone()
                        tvDiastolic.visible()
                        tvHbA1C.gone()
                    }
                    NCDMRUtil.RBS -> {
                        tvDiastolic.gone()
                        tvSystolic.visible()
                        tvHbA1C.gone()
                    }
                    else -> {
                        tvDiastolic.gone()
                        tvSystolic.gone()
                        tvHbA1C.visible()
                    }
                }
            }
        }
    }

    private fun getUnitSuffix(filter: List<Pair<Int, String?>>?): String {
        return filter?.firstOrNull()?.second ?: ""
    }
}