package com.medtroniclabs.opensource.ui.mypatients.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.databinding.RowAgparFooterBinding
import com.medtroniclabs.opensource.databinding.RowAgparHeaderBinding
import com.medtroniclabs.opensource.databinding.RowAgparScoreBinding
import com.medtroniclabs.opensource.model.assessment.ApgarScore
import com.medtroniclabs.opensource.model.assessment.AgparScoreFooter
import com.medtroniclabs.opensource.model.assessment.AgparScoreHeader
import com.medtroniclabs.opensource.model.assessment.AgparScoreRow
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparColumnIdentifierType
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparItemViewType
import com.medtroniclabs.opensource.ui.mypatients.enumType.AgparRowIdentifierType

class AgparScoreAdapter(val onClick: (rowType: AgparRowIdentifierType, columnType: AgparColumnIdentifierType, score: String?) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var apgarScores = mutableListOf<ApgarScore>()

    inner class AgparHeaderViewHolder(val binding: RowAgparHeaderBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(header: AgparScoreHeader) {
            with(binding.root.context) {
                binding.tvHeaderOne.text = getString(header.headerOne)
                binding.tvHeaderTwo.text = getString(header.headerTwo)
                binding.tvHeaderThree.text = getString(header.headerThree)
                binding.tvHeaderFour.text = getString(header.headerFour)
            }

        }
    }

    inner class AgparFooterViewHolder(val binding: RowAgparFooterBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(footer: AgparScoreFooter) {
            with(binding.root.context) {

                binding.tvTotal.text = getString(footer.indicatorName)
                binding.tvOneMinuteTotal.text =
                    footer.oneMinuteTotal ?: getString(R.string.separator_double_hyphen)
                binding.tvFiveMinuteTotal.text =
                    footer.fiveMinuteTotal ?: getString(R.string.separator_double_hyphen)
                binding.tvTenMinuteTotal.text =
                    footer.tenMinuteTotal ?: getString(R.string.separator_double_hyphen)
            }

        }
    }

    inner class AgparItemViewHolder(val binding: RowAgparScoreBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: AgparScoreRow) {
            with(binding.root.context) {
                binding.tvIndicatorName.text = getString(item.indicatorName)
                binding.tvOneMinute.text = item.oneMinute ?: getString(R.string.separator_double_hyphen)
                binding.tvFiveMinute.text = item.fiveMinute ?: getString(R.string.separator_double_hyphen)
                binding.tvTenMinute.text = item.tenMinute ?: getString(R.string.separator_double_hyphen)

                binding.tvOneMinute.setOnClickListener {
                    onClick.invoke(
                        item.indicatorType,
                        AgparColumnIdentifierType.ONE_MINUTE,
                        item.oneMinute
                    )
                }
                binding.tvFiveMinute.setOnClickListener {
                    onClick.invoke(
                        item.indicatorType,
                        AgparColumnIdentifierType.FIVE_MINUTES,
                        item.fiveMinute
                    )
                }
                binding.tvTenMinute.setOnClickListener {
                    onClick.invoke(
                        item.indicatorType,
                        AgparColumnIdentifierType.TEN_MINUTES,
                        item.tenMinute
                    )
                }
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return when (apgarScores[position].viewType) {
            AgparItemViewType.HEADER -> 0
            AgparItemViewType.ROW -> 1
            AgparItemViewType.FOOTER -> 2
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            0 -> AgparHeaderViewHolder(RowAgparHeaderBinding.inflate(inflater, parent,false))
            1 -> AgparItemViewHolder(RowAgparScoreBinding.inflate(inflater, parent, false))
            2 -> AgparFooterViewHolder(RowAgparFooterBinding.inflate(inflater, parent, false))
            else -> AgparItemViewHolder(RowAgparScoreBinding.inflate(inflater, parent, false))
        }
    }

    override fun getItemCount(): Int {
        return apgarScores.size
    }


    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is AgparHeaderViewHolder -> {
                apgarScores[position].header?.let { holder.bind(it) }
            }

            is AgparFooterViewHolder -> {
                apgarScores[position].footer?.let { holder.bind(it) }
            }

            is AgparItemViewHolder -> {
                apgarScores[position].row?.let { holder.bind(it) }
            }
        }
    }

    fun submitData(apgarScores: List<ApgarScore>) {
        this.apgarScores.clear()
        this.apgarScores = apgarScores.toMutableList()
        notifyDataSetChanged()
    }
}