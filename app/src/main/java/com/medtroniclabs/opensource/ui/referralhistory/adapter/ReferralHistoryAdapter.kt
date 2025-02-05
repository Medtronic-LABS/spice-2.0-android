package com.medtroniclabs.opensource.ui.referralhistory.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.ReferralticketItemLayoutBinding

class ReferralHistoryAdapter :RecyclerView.Adapter<ReferralHistoryAdapter.ViewHolder>() {
    private val items: MutableList<Map<String, Any?>> = mutableListOf()

    fun updateList(newList: List<Map<String, Any?>>) {
        items.clear()
        items.addAll(newList)
        notifyItemChanged(0, items.size)
    }

    inner class ViewHolder(private val binding: ReferralticketItemLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(data: Map<String, Any?>) {
            val context = binding.root.context
            with(binding) {
                tvLabel.text = data[DefinedParams.label] as? String
                    ?: binding.root.context.getString(R.string.hyphen_symbol)
                val value = data[DefinedParams.Value]

                tvValue.text = processMapValue(value,data[DefinedParams.label] as? String)
                val color = data[DefinedParams.valueColor] as? Int
                color?.let {
                    tvValue.setTextColor(ContextCompat.getColor(context, color))
                }
            }
        }

        private fun processMapValue(value: Any?, keys: String?): String {
            return when (value) {
                is String -> value.ifBlank {
                    binding.root.context.getString(R.string.separator_double_hyphen)
                }

                is List<*> -> if (value.isEmpty()) {
                    binding.root.context.getString(R.string.separator_double_hyphen)
                } else {
                    if (keys.equals(binding.root.context.getString(R.string.medication_prescribed),true) || keys.equals(binding.root.context.getString(R.string.investigations_referred),true)) {
                        value.mapIndexed { index, item -> "${index + 1}. $item" }
                            .joinToString(separator = "\n")
                    } else {
                        value.joinToString(", ")
                    }
                }

                else -> binding.root.context.getString(R.string.separator_double_hyphen)
            }.trim()
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(
            ReferralticketItemLayoutBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        items[position].let { holder.bind(it) }
    }

}