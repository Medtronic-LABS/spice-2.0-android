package org.medtroniclabs.uhis.ui.followup.customview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryDataItem
import org.medtroniclabs.uhis.databinding.RowViewPatientHistoryBinding

class PatientHistoryAdapter :
    RecyclerView.Adapter<PatientHistoryAdapter.PatientHistoryViewHolder>() {
    private val items = mutableListOf<PatientHistoryDataItem>()

    inner class PatientHistoryViewHolder(private val binding: RowViewPatientHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(
            holder: PatientHistoryViewHolder,
            item: PatientHistoryDataItem,
        ) {
            if (item.labelId == 0) {
                binding.tvNameAge.visible()
                binding.clLabelTextViews.gone()

                binding.tvNameAge.text = item.valueText
            } else {
                binding.clLabelTextViews.visible()
                binding.tvNameAge.gone()

                val context = holder.itemView.context
                val defaultText = context.getString(R.string.hyphen_symbol)

                binding.tvLabelText.text = context.getString(item.labelId)

                // Default values
                var valueText = item.valueText ?: defaultText
                var valueColor = context.getColor(R.color.gray_black)

                if (item.labelId == R.string.bmi) {
                    val bmiValue = item.valueText?.toDoubleOrNull()
                    val bmiInfo = CommonUtils.getBMIInformation(context, bmiValue)

                    valueText = bmiInfo?.first ?: valueText
                    valueColor = bmiInfo?.second ?: valueColor
                }

                if (valueText != defaultText) {
                    item.unitText?.let {
                        valueText = valueText + " " + context.getString(it)
                    }
                }

                binding.tvValueText.text = valueText
                binding.tvValueText.setTextColor(valueColor)
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PatientHistoryViewHolder {
        val binding = RowViewPatientHistoryBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return PatientHistoryViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PatientHistoryViewHolder,
        position: Int,
    ) {
        holder.bind(holder, items[position])
    }

    override fun getItemCount() = items.size

    fun submitData(data: List<PatientHistoryDataItem>) {
        val oldSize = items.size
        items.clear()
        notifyItemRangeRemoved(0, oldSize)
        items.addAll(data)
        notifyItemRangeInserted(0, data.size)
    }
}
