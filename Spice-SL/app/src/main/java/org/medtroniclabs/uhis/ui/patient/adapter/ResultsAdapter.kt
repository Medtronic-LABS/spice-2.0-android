package org.medtroniclabs.uhis.ui.patient.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.databinding.LayoutResultsAdapterBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams

class ResultsAdapter(private var dataList: ArrayList<Map<String, Any>>) : RecyclerView.Adapter<ResultsAdapter.ViewHolder>() {
    override fun getItemId(position: Int): Long = position.toLong()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ViewHolder =
        ViewHolder(
            LayoutResultsAdapterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    class ViewHolder(val binding: LayoutResultsAdapterBinding) : RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(
            position: Int,
            item: Map<String, Any>,
        ) {
            if (item.containsKey(DefinedParams.RESULT_NAME)) {
                binding.tvKey.text = item[DefinedParams.RESULT_NAME] as String? ?: "-"
            }
            val strBuilder = StringBuilder()
            if (item.containsKey(DefinedParams.RESULT_VALUE)) {
                strBuilder.append(item[DefinedParams.RESULT_VALUE] as String? ?: "")
            }
            strBuilder.append(" ")
            if (item.containsKey(DefinedParams.UNIT)) {
                strBuilder.append(item[DefinedParams.UNIT] as String? ?: "")
            }
            binding.tvValue.text = strBuilder.toString()

            if (item.containsKey(DefinedParams.IS_ABNORMAL)) {
                val isAbnormal = item[DefinedParams.IS_ABNORMAL]
                binding.tvValue.setTextColor(
                    if (isAbnormal is Boolean && isAbnormal) {
                        context.getColor(R.color.a_red_error)
                    } else {
                        context.getColor(
                            R.color.black,
                        )
                    },
                )
            }

            if (item.containsKey(DefinedParams.DISPLAY_NAME)) {
                val displayName = item[DefinedParams.DISPLAY_NAME]
                if (displayName is String) {
                    binding.tvUnit.text = displayName.ifBlank { "" }
                }
            }
        }
    }

    override fun onBindViewHolder(
        holder: ViewHolder,
        position: Int,
    ) {
        dataList.let {
            holder.bind(position, it[position])
        }
    }

    override fun getItemCount(): Int = dataList.size
}
