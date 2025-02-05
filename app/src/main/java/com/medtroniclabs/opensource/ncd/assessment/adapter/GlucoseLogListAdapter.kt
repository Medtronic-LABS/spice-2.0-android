package com.medtroniclabs.opensource.ncd.assessment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import com.medtroniclabs.opensource.databinding.ListItemGlucoseLogBinding
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.GlucoseLogList

class GlucoseLogListAdapter(private val glucoseLogs: ArrayList<GlucoseLogList>) :
    RecyclerView.Adapter<GlucoseLogListAdapter.GlucoseLogListViewHolder>() {

    inner class GlucoseLogListViewHolder(val binding: ListItemGlucoseLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): GlucoseLogListViewHolder {
        return GlucoseLogListViewHolder(
            ListItemGlucoseLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: GlucoseLogListViewHolder, position: Int
    ) {
        val context = holder.context
        val glucoseLog = glucoseLogs[position]

        glucoseLog.glucoseDateTime?.let {
            val date = DateUtils.convertDateFormat(
                it, DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_FORMAT_ddMMMyyyy
            )
            holder.binding.tvAssessmentDate.text = date
        }


        val glucoseValue =
            if ((glucoseLog.glucoseValue ?: 0.0f) > 0f) glucoseLog.glucoseValue.toString() else ""
        val glucoseUnit = if (glucoseLog.glucoseUnit.isNullOrBlank()) "" else glucoseLog.glucoseUnit
        val glucose =
            if (glucoseValue.isNotEmpty() && glucoseUnit.isNotEmpty()) "$glucoseValue ($glucoseUnit)" else ""
        when (glucoseLog.glucoseType) {
            Screening.rbs -> {
                holder.binding.tvFbs.text = context.getString(R.string.hyphen_symbol)
                holder.binding.tvRbs.text =
                    glucose.ifEmpty { context.getString(R.string.hyphen_symbol) }
            }

            Screening.fbs -> {
                holder.binding.tvFbs.text =
                    glucose.ifEmpty { context.getString(R.string.hyphen_symbol) }
                holder.binding.tvRbs.text = context.getString(R.string.hyphen_symbol)
            }
        }

        val hbA1cValue =
            if ((glucoseLog.hba1c ?: 0.0f) > 0f) glucoseLog.hba1c.toString() else ""
        val hbA1cUnit = if (glucoseLog.hba1cUnit.isNullOrBlank()) "" else glucoseLog.hba1cUnit
        val hbA1c =
            if (hbA1cValue.isNotEmpty() && hbA1cUnit.isNotEmpty()) "$hbA1cValue ($hbA1cUnit)" else ""
        holder.binding.tvHbA1c.text = hbA1c.ifEmpty { context.getString(R.string.hyphen_symbol) }
    }

    override fun getItemCount(): Int {
        return glucoseLogs.size
    }
}