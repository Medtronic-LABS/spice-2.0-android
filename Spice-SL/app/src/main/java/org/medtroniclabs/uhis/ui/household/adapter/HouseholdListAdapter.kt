package org.medtroniclabs.uhis.ui.household.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.databinding.ListItemHouseholdBinding
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil

/**
 * Showing list of households based from local DB in [org.medtroniclabs.uhis.ui.household.HouseholdSearchActivity]
 */
class HouseholdListAdapter(
    private val callback: (Long) -> Unit,
) : RecyclerView.Adapter<HouseholdListAdapter.HouseholdListViewHolder>() {
    private val houseHoldList = mutableListOf<HouseHoldEntityWithLastActivity>()

    fun setHouseHoldList(list: List<HouseHoldEntityWithLastActivity>) {
        val oldCount = houseHoldList.size
        houseHoldList.clear()
        houseHoldList.addAll(list)
        notifyItemRangeRemoved(0, oldCount)
        notifyItemRangeInserted(0, list.size)
    }

    class HouseholdListViewHolder(val binding: ListItemHouseholdBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        init {
            binding.tvCardHouseholdName.gone()
            binding.flexTitle.visible()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: HouseholdListViewHolder,
        position: Int,
    ) {
        val context = holder.context
        val item = houseHoldList[position]
        holder.binding.flexTitle.removeAllViews()
        holder.binding.flexTitle.addView(
            TextView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextAppearance(R.style.TextStyle_Bold_16_NoBG)
                setTextColor(ContextCompat.getColor(context, R.color.grey_black))
                text = item.name
            },
        )
        holder.binding.flexTitle.addView(
            TextView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextAppearance(R.style.TextStyle_Bold_16_NoBG)
                setTextColor(ContextCompat.getColor(context, R.color.base_muted_foreground))
                text = formatBracServiceMembers(
                    context,
                    item.membersSeekingBracServices,
                    item.totalRegisteredMembers,
                )
            },
        )
        holder.binding.tvHouseholdNo.text = item.householdNo ?: holder.context.getString(R.string.separator_double_hyphen)
        holder.binding.tvLabelVillage.setText(R.string.village)
        holder.binding.tvVillageName.text = item.subVillageName
        val shasthyaShebika = when {
            item.shasthyaShebikaNameSsId.isNotBlank() && item.shasthyaShebikaName.isNotBlank() -> {
                "${item.shasthyaShebikaNameSsId} - ${item.shasthyaShebikaName}"
            }

            item.shasthyaShebikaName.isNotBlank() -> {
                item.shasthyaShebikaName
            }

            else -> {
                context.getString(R.string.separator_double_hyphen)
            }
        }
        holder.binding.tvSSName.text = shasthyaShebika
        holder.binding.tvLastVisitDate.text = DateUtils.formatDateToDisplayFormat(item.lastActivityAt)

        val services = item.assessmentHistory.filterNot { it.serviceProvided.isNullOrBlank() }

        // If user have received some services, then add their icons beside name
        if (services.isNotEmpty()) {
            val iconsToShow = services
                .map(AssessmentUtil::mapServiceToServiceIcon)
                .filterNot { it == View.NO_ID }
                .take(3)

            iconsToShow.forEach { iconRes ->
                val imageView = ImageView(holder.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        28.px,
                        28.px,
                    )
                }
                imageView.setImageResource(iconRes)
                holder.binding.flexTitle.addView(imageView)
            }

            val remainingCount = services.size - iconsToShow.size
            if (remainingCount > 0) {
                val textView = TextView(holder.context).apply {
                    layoutParams = ViewGroup.MarginLayoutParams(
                        28.px,
                        28.px,
                    )
                }
                textView.text = "+$remainingCount"
                textView.textSize = 16f
                textView.gravity = Gravity.CENTER
                textView.setTextColor(ContextCompat.getColor(holder.context, R.color.base_muted_foreground))
                textView.setBackgroundResource(R.drawable.bg_more_services)
                holder.binding.flexTitle.addView(textView)
            }
        }
        holder.binding.cardPatient.safeClickListener {
            callback(item.id)
        }
    }

    override fun onViewRecycled(holder: HouseholdListViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.flexTitle.removeAllViews()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): HouseholdListViewHolder =
        HouseholdListViewHolder(
            ListItemHouseholdBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun getItemCount(): Int = houseHoldList.size

    private fun formatBracServiceMembers(
        context: Context,
        seeking: Int,
        total: Int,
    ): String =
        context.getString(
            R.string.household_brac_service_members_value,
            CommonUtils.formatCountForCurrentLocale(seeking),
            CommonUtils.formatCountForCurrentLocale(total),
        )
}
