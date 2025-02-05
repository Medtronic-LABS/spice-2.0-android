package com.medtroniclabs.opensource.ui.landing.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.databinding.RowOfflineEntityDetailsBinding
import com.medtroniclabs.opensource.model.landing.OfflineSyncEntityDetail

class OfflineSyncEntitiesAdapter :
    RecyclerView.Adapter<OfflineSyncEntitiesAdapter.OfflineSyncEntitiesViewHolder>() {

    private var items = mutableListOf<OfflineSyncEntityDetail>()

    fun updateList(list: List<OfflineSyncEntityDetail>) {
        val oldCount = items.size
        items.clear()
        items.addAll(list)

        notifyItemRangeRemoved(0, oldCount)
        notifyItemRangeInserted(0, list.size)
    }

    inner class OfflineSyncEntitiesViewHolder(val binding: RowOfflineEntityDetailsBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(
        parent: ViewGroup, viewType: Int
    ): OfflineSyncEntitiesViewHolder {
        return OfflineSyncEntitiesViewHolder(
            RowOfflineEntityDetailsBinding.inflate(
                LayoutInflater.from(parent.context), parent, false
            )
        )
    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: OfflineSyncEntitiesViewHolder, position: Int) {
        val item = items[position]
        holder.binding.tvLabel.text = item.tableName
        holder.binding.tvValue.text = ": ${item.unSyncedCount}"
    }
}