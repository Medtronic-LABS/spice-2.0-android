package com.medtroniclabs.opensource.formgeneration.utility

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.databinding.DosageInstructionItemListBinding

class DosageInstructionAdapter(
    private val infoList: ArrayList<String>
) : RecyclerView.Adapter<DosageInstructionAdapter.DosageInstructionListViewHolder>(){

    inner class DosageInstructionListViewHolder(val binding: DosageInstructionItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): DosageInstructionAdapter.DosageInstructionListViewHolder {
        return DosageInstructionListViewHolder(
            DosageInstructionItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: DosageInstructionAdapter.DosageInstructionListViewHolder, position: Int) {
        val infoModel = infoList[position]
        holder.binding.tvInfo.text = infoModel
    }

    override fun getItemCount(): Int {
        return infoList.size
    }
}