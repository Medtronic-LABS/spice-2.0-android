package org.medtroniclabs.uhis.ui.household.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.core.view.setPadding
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.databinding.SummaryListItemBinding
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.formgeneration.extension.px

/**
 * Adapter for showing member assessment history
 */
class MemberAssessmentHistoryAdapter(
    val historyList: List<MemberAssessmentHistoryEntity>,
) : RecyclerView.Adapter<MemberAssessmentHistoryAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        viewGroup: ViewGroup,
        itemType: Int,
    ): ViewHolder =
        ViewHolder(
            LinearLayout(viewGroup.context).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setPadding(10.px)
            },
        )

    override fun onBindViewHolder(
        viewHolder: ViewHolder,
        position: Int,
    ) {
        viewHolder.bindData(historyList[position])
    }

    override fun getItemCount() = historyList.size

    class ViewHolder(private val view: LinearLayout) : RecyclerView.ViewHolder(view) {
        fun bindData(history: MemberAssessmentHistoryEntity) {
            val context = view.context
            view.removeAllViews()
            val summaryItems = MemberAssessmentHistoryAdapterUtil.constructHistoryAdapterBindItems(context, history)
            summaryItems.forEach { summaryItem ->
                addSummaryView(summaryItem.name, summaryItem.value, summaryItem.valueColor)
            }
        }

        private fun addSummaryView(
            name: String,
            value: String,
            valueColor: Int? = null,
        ) {
            val binding = SummaryListItemBinding.inflate(LayoutInflater.from(view.context))
            binding.tvLabel.text = name
            binding.tvValue.text = value
            valueColor?.let { binding.tvValue.setTextColor(it) }
            view.addView(binding.root)
        }
    }
}
