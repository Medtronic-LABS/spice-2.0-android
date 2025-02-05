package com.medtroniclabs.opensource.ncd.assessment.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import com.medtroniclabs.opensource.databinding.ListItemBpLogBinding
import com.medtroniclabs.opensource.ncd.data.BPLogList

class BPLogListAdapter(private val bpLogs: ArrayList<BPLogList>) :
    RecyclerView.Adapter<BPLogListAdapter.BPLogListViewHolder>() {

    inner class BPLogListViewHolder(val binding: ListItemBpLogBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BPLogListViewHolder {
        return BPLogListViewHolder(
            ListItemBpLogBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: BPLogListViewHolder,
        position: Int
    ) {
        val bpLog = bpLogs[position]

        bpLog.bpTakenOn?.let {
            val date = DateUtils.convertDateFormat(
                it,
                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_FORMAT_ddMMMyyyy
            )
            holder.binding.tvAssessmentDate.text = date
        }

        val value = "${bpLog.avgSystolic.toString()} / ${bpLog.avgDiastolic.toString()}"
        holder.binding.tvSystolicDiastolic.text = value
    }

    override fun getItemCount(): Int {
        return bpLogs.size
    }
}