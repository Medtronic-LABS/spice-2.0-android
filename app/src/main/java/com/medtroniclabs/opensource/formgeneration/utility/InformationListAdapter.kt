package com.medtroniclabs.opensource.formgeneration.utility

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.databinding.InformationItemListBinding
import com.medtroniclabs.opensource.formgeneration.model.InformationModel

class InformationListAdapter(
    private val infoList: ArrayList<InformationModel>
) : RecyclerView.Adapter<InformationListAdapter.InformationListViewHolder>() {

    inner class InformationListViewHolder(val binding: InformationItemListBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): InformationListAdapter.InformationListViewHolder {
        return InformationListViewHolder(
            InformationItemListBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(holder: InformationListViewHolder, position: Int) {
            val infoModel = infoList[position]
            if (infoModel.imageId != null){
                holder.binding.ivItem.setImageResource(infoModel.imageId)
            } else{
                holder.binding.ivItem.visibility = View.GONE
            }
            holder.binding.tvInfo.text = infoModel.inputText
    }

    override fun getItemCount(): Int {
        return infoList.size
    }
}

