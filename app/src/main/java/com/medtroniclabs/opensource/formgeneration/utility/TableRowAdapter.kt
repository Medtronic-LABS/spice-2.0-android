package com.medtroniclabs.opensource.formgeneration.utility

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.data.model.DosageTableModel
import com.medtroniclabs.opensource.databinding.TableRowLayoutBinding

class TableRowAdapter(
    val context: Context,
    private val infoList: ArrayList<DosageTableModel>
) : RecyclerView.Adapter<TableRowAdapter.InformationListViewHolder>() {

    inner class InformationListViewHolder(val binding: TableRowLayoutBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): TableRowAdapter.InformationListViewHolder {
        return InformationListViewHolder(
            TableRowLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: InformationListViewHolder, position: Int) {
        val infoModel = infoList[position]
        if (position % 2 != 0) {
            holder.binding.apply {
                tvDay.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.context,
                        R.color.table_row_color
                    )
                )
                tvMorning.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.context,
                        R.color.table_row_color
                    )
                )
                tvNight.setBackgroundColor(
                    ContextCompat.getColor(
                        holder.context,
                        R.color.table_row_color
                    )
                )
            }
        }
        holder.binding.tvDay.text = infoModel.day
        holder.binding.tvMorning.text = infoModel.morning
        infoModel.night?.let {
            holder.binding.tvNight.text = infoModel.night
        } ?: kotlin.run {
            if (holder.binding.tvNight.isVisible())
                holder.binding.tvNight.gone()
        }
    }

    override fun getItemCount(): Int {
        return infoList.size
    }
}

