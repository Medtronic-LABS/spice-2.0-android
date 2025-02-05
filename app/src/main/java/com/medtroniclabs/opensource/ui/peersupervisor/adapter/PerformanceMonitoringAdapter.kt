package com.medtroniclabs.opensource.ui.peersupervisor.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.data.performance.CHWPerformanceMonitoring
import com.medtroniclabs.opensource.databinding.RowPerformanceMonitoringBinding

class PerformanceMonitoringAdapter :
    PagingDataAdapter<CHWPerformanceMonitoring, PerformanceMonitoringAdapter.PerformanceMonitoringViewHolder>(
        PerformanceMonitorComparator
    ) {

    inner class PerformanceMonitoringViewHolder(val binding: RowPerformanceMonitoringBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PerformanceMonitoringViewHolder {
        return PerformanceMonitoringViewHolder(
            RowPerformanceMonitoringBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun onBindViewHolder(holder: PerformanceMonitoringViewHolder, position: Int) {
        val performanceDetails = getItem(position)
        val binding = holder.binding

        if (position % 2 == 0) {
            binding.clRowPerformanceMonitoringRoot.setBackgroundColor(
                ContextCompat.getColor(
                    holder.context,
                    R.color.white
                )
            )
        } else {
            binding.clRowPerformanceMonitoringRoot.setBackgroundColor(
                ContextCompat.getColor(
                    holder.context,
                    R.color.table_row_color
                )
            )
        }

        binding.tvChwName.text = performanceDetails?.chwName ?: "--"
        binding.tvVillage.text = performanceDetails?.villageName ?: "--"
        binding.tvNoOfHH.text = performanceDetails?.household?.toString() ?: "--"
        binding.tvNoOfPatient.text = performanceDetails?.householdMember?.toString() ?: "--"
        binding.tvICCM.text = performanceDetails?.iccm?.toString() ?: "--"
        binding.tvRMNCH.text = performanceDetails?.rmnch?.toString() ?: "--"
        binding.tvOS.text = performanceDetails?.otherSymptoms?.toString() ?: "--"
        binding.tvReferrals.text = performanceDetails?.referred?.toString() ?: "--"
        binding.tvTreatmentStarted.text = performanceDetails?.onTreatment?.toString() ?: "--"
        binding.tvRecoveredImproved.text = performanceDetails?.recovered?.toString() ?: "--"
        binding.tvWorsened.text = performanceDetails?.worsened?.toString() ?: "--"

        binding.tvFollowUpDueCalls.text = performanceDetails?.followUpDueCalls?.toString() ?: "--"
        binding.tvFollowUpDueVisit.text = performanceDetails?.followUpDueVisit?.toString() ?: "--"
        binding.tvFollowUpCondCalls.text = performanceDetails?.followUpCondCalls?.toString() ?: "--"
        binding.tvFollowUpCondVisit.text = performanceDetails?.followUpCondVisit?.toString() ?: "--"
    }

    object PerformanceMonitorComparator : DiffUtil.ItemCallback<CHWPerformanceMonitoring>() {
        override fun areItemsTheSame(
            oldItem: CHWPerformanceMonitoring,
            newItem: CHWPerformanceMonitoring
        ): Boolean {
            return false
        }

        override fun areContentsTheSame(
            oldItem: CHWPerformanceMonitoring,
            newItem: CHWPerformanceMonitoring
        ): Boolean {
            return false
        }
    }
}

