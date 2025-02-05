package com.medtroniclabs.opensource.ncd.followup.adapter

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.databinding.FollowUpListItemPatientsBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.LTFU_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.SCREENED
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.getDaysString
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUp

class NCDPatientFollowUPListAdapter(
    val listener: PatientSelectionListenerForFollowUp
) : PagingDataAdapter<PatientFollowUpEntity, NCDPatientFollowUPListAdapter.NCDPatientFollowUPListViewHolder>(
    PatientListComparator
) {

    inner class NCDPatientFollowUPListViewHolder(val binding: FollowUpListItemPatientsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context

        fun bind(item: PatientFollowUpEntity) {
            val context = binding.root.context
            val name = item.name ?: context.getString(R.string.separator_hyphen)
            val gender = item.gender?.lowercase()?.capitalizeFirstChar()
                ?: context.getString(R.string.separator_hyphen)

            val age = item.age?.toString() ?: ""
            val patientInfo = context.getString(
                R.string.household_summary_member_info,
                name,
                age,
                CommonUtils.translatedGender(context, gender)
            )
            binding.tvPatientName.text = patientInfo
            binding.tvDueInformation.setTextColor(Color.parseColor("#994242"))
            binding.tvLabelReason.gone()
            if (item.type.equals(SCREENED, true)) {
                val referredReasonsText =
                    item.referredReasons?.filterNot { it.isBlank() }
                        ?.joinToString(", ") { it.trim() }
                updateReasonSection(
                    label = context.getString(R.string.reason),
                    text = referredReasonsText,
                    context = context,
                    binding = binding
                )
            }
            if (item.type.equals(LTFU_Type, true)) {
                val overDueCategoriesText = item.overDueCategories
                    ?.asSequence()
                    ?.filter { it.isNotBlank() }
                    ?.joinToString(", ") { it.capitalizeFirstChar() }
                updateReasonSection(
                    label = context.getString(R.string.overdue),
                    text = overDueCategoriesText,
                    context = context,
                    binding = binding
                )
            }
            item.referredDateSince?.let {
                if (it > 0) {
                    binding.tvDueInformation.visible()
                    binding.tvDueInformation.text =
                        context.getString(
                            getDaysString(item.referredDateSince),
                            item.referredDateSince
                        )
                } else {
                    binding.tvDueInformation.gone()
                }
            } ?: binding.tvDueInformation.gone()

            item.retryAttempts?.let {
                binding.ivRecentAttemptCount.apply {
                    visibility = View.VISIBLE
                    text = it.toString()
                }
            } ?: kotlin.run { binding.ivRecentAttemptCount.gone() }
            if (item.type.equals(NCDFollowUpUtils.Defaulters_Type, true)) {
                binding.assessmentButton.invisible()
            } else {
                binding.assessmentButton.visible()
            }
            binding.callButton.safeClickListener {
                listener.onSelectedPatientForCall(item)
            }
            binding.assessmentButton.safeClickListener {
                listener.onSelectedPatientForAssessment(item)
            }
            binding.root.safeClickListener {
                listener.onSelectedPatientCard(item)
            }
        }
    }

    private fun updateReasonSection(
        label: String,
        text: String?,
        context: Context,
        binding: FollowUpListItemPatientsBinding // Replace with the actual type of your binding
    ) {
        if (!text.isNullOrEmpty()) {
            binding.tvLabelReason.text = context.getString(R.string.label_with_text, label, text)
            binding.tvLabelReason.visible()
        } else {
            binding.tvLabelReason.gone()
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NCDPatientFollowUPListViewHolder {
        return NCDPatientFollowUPListViewHolder(
            FollowUpListItemPatientsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }


    override fun onBindViewHolder(holder: NCDPatientFollowUPListViewHolder, position: Int) {
        getItem(position)?.let { item ->
            holder.bind(item)
        }
    }

    object PatientListComparator : DiffUtil.ItemCallback<PatientFollowUpEntity>() {
        override fun areItemsTheSame(
            oldItem: PatientFollowUpEntity,
            newItem: PatientFollowUpEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: PatientFollowUpEntity,
            newItem: PatientFollowUpEntity
        ): Boolean {
            return oldItem.id == newItem.id
        }
    }

    fun getDrawable(view: ConstraintLayout, colorCode: String) {
        if (view.background != null) {
            val drawable = view.background as GradientDrawable
            drawable.mutate()
            drawable.setStroke(3, Color.parseColor(colorCode))
        }
    }
}