package com.medtroniclabs.opensource.ui.dashboard.ncd.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isTablet
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.databinding.ListItemUserDashboardBinding

class UserDashboardAdapter(private val  customize: Boolean, private val userDashboardLists: ArrayList<Triple<String, Int, Int>>) :
    RecyclerView.Adapter<UserDashboardAdapter.UserDashboardViewHolder>() {

    inner class UserDashboardViewHolder(val binding: ListItemUserDashboardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserDashboardViewHolder {
        return UserDashboardViewHolder(
            ListItemUserDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: UserDashboardViewHolder,
        position: Int
    ) {
        userDashboardLists[position].let { dashboard ->
            val context = holder.binding.root.context
            holder.binding.apply {
                tvKey.text = dashboard.first.uppercase()
                tvValue.text = if (customize) "0" else dashboard.second.toString()

                if (context.isTablet()) {
                    ivActivity.apply {
                        visible()
                        setImageDrawable(
                            ContextCompat.getDrawable(
                                root.context,
                                dashboard.third
                            )
                        )
                    }
                } else
                    ivActivity.gone()
            }
        }
    }

    override fun getItemCount(): Int {
        return userDashboardLists.size
    }
}

