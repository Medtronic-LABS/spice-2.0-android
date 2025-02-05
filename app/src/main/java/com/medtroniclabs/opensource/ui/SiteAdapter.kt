package com.medtroniclabs.opensource.ui

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.databinding.RowSiteListBinding
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener

class SiteAdapter(
    private val healthFacilityList: List<HealthFacilityEntity>,
    private var healthFacilitySelectedID: Long? = null,
    private val healthFacilitySelectionCallback: ((siteEntity: HealthFacilityEntity) -> Unit)
) :
    RecyclerView.Adapter<SiteAdapter.SiteViewHolder>() {

    class SiteViewHolder(val binding: RowSiteListBinding) : RecyclerView.ViewHolder(binding.root) {
        val viewContext: Context = binding.root.context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SiteViewHolder {
        return SiteViewHolder(
            RowSiteListBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: SiteViewHolder, position: Int) {
        val siteModel = healthFacilityList[position]
        holder.binding.tvSiteName.text = siteModel.name

        holder.binding.root.safeClickListener {
            healthFacilitySelectedID = siteModel.tenantId
            notifyItemRangeChanged(0, healthFacilityList.size)
        }

        healthFacilitySelectedID?.let {
            if (siteModel.tenantId == it) {
                healthFacilitySelectionCallback.invoke(siteModel)
                holder.binding.ivChecked.visibility = View.VISIBLE
                holder.binding.tvSiteName.isEnabled = true
               /* holder.binding.tvSiteName.typeface =
                    ResourcesCompat.getFont(holder.viewContext, R.font.inter_bold)*/
            } else {
                holder.binding.ivChecked.visibility = View.INVISIBLE
                holder.binding.tvSiteName.isEnabled = false
                /*holder.binding.tvSiteName.typeface =
                    ResourcesCompat.getFont(holder.viewContext, R.font.inter_regular)*/
            }
        } ?: run {
            holder.binding.ivChecked.visibility = View.GONE
        }
    }

    override fun getItemCount(): Int {
        return healthFacilityList.size
    }
}