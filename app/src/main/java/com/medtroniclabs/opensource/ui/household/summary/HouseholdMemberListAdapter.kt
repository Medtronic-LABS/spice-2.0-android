package com.medtroniclabs.opensource.ui.household.summary

import android.content.Context
import android.text.SpannableStringBuilder
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.common.CommonUtils.getAgeFromDOB
import com.medtroniclabs.opensource.common.CommonUtils.getGenderText
import com.medtroniclabs.opensource.databinding.MembersSummaryListItemBinding
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ui.household.MemberSelectionListener

class HouseholdMemberListAdapter(
    private val houseHoldMembersList: List<HouseholdMemberEntity>,
    private val listener: MemberSelectionListener,
    private val phuWalkInsFlow: Boolean
) : RecyclerView.Adapter<HouseholdMemberListAdapter.HouseholdListViewHolder>() {

    inner class HouseholdListViewHolder(val binding: MembersSummaryListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun onBindViewHolder(
        holder: HouseholdListViewHolder,
        position: Int
    ) {
        val context = holder.context
        val item = houseHoldMembersList[position]
        if (item.isActive) {
            disableAllChildren(holder.binding.root, 1f, true)
        } else {
            disableAllChildren(holder.binding.root, 0.5f, false)
        }
        holder.binding.tvMemberName.text = getMemberInfoText(context, item)
        holder.binding.tvPatientId.text = item.patientId ?: context.getString(R.string.separator_double_hyphen)
        if(!phuWalkInsFlow) {
        holder.binding.cardPatient.safeClickListener {
            if (item.isActive) {
                listener.onMemberSelected(item.id, false, item.dateOfBirth)
            }
        }
        } else {
            holder.binding.forwardIcon.gone()

        }
    }

    private fun disableAllChildren(root: MaterialCardView, alpha: Float, enabled: Boolean) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            child.alpha = alpha // Set 50% opacity to indicate a disabled state
            child.isEnabled = enabled // Optionally disable interaction
        }
    }

    private fun getMemberInfoText(context: Context, item: HouseholdMemberEntity): CharSequence {
        return SpannableStringBuilder(
            context.getString(
                R.string.household_summary_member_info,
                item.name,
                getAgeFromDOB(item.dateOfBirth, context),
                getGenderText(item.gender, context)
            )
        )
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): HouseholdListViewHolder {
        return HouseholdListViewHolder(
            MembersSummaryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return houseHoldMembersList.size
    }

}