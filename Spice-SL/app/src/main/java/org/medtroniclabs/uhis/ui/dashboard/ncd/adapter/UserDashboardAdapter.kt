package org.medtroniclabs.uhis.ui.dashboard.ncd.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.databinding.ListItemUserDashboardBinding

data class DashboardCardItem(
    val key: String,
    val title: String,
    val count: Int,
    val iconRes: Int,
)

class UserDashboardAdapter(
    private val customize: Boolean,
    private val userDashboardLists: ArrayList<DashboardCardItem>,
    private val onCardClick: (DashboardCardItem) -> Unit,
) : RecyclerView.Adapter<UserDashboardAdapter.UserDashboardViewHolder>() {
    class UserDashboardViewHolder(val binding: ListItemUserDashboardBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): UserDashboardViewHolder =
        UserDashboardViewHolder(
            ListItemUserDashboardBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(
        holder: UserDashboardViewHolder,
        position: Int,
    ) {
        userDashboardLists[position].let { dashboard ->
            holder.binding.apply {
                tvKey.text = dashboard.title.uppercase()
                tvValue.text = if (customize) "0" else dashboard.count.toString()
                root.setOnClickListener {
                    onCardClick(dashboard)
                }
            }
        }
    }

    override fun getItemCount(): Int = userDashboardLists.size
}
