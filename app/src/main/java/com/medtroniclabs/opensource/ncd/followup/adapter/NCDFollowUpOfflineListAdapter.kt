package com.medtroniclabs.opensource.ncd.followup.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils.getDaysDifference
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FollowUpListItemPatientsBinding
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUpOffline

class NCDFollowUpOfflineListAdapter(val listener: PatientSelectionListenerForFollowUpOffline)
    : RecyclerView.Adapter<NCDFollowUpOfflineListAdapter.NCDFollowUpOfflineViewHolder>() {
    var list = mutableListOf<NCDFollowUp>()

    inner class NCDFollowUpOfflineViewHolder(val binding: FollowUpListItemPatientsBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
        fun bind(item: NCDFollowUp) {
            val context = binding.root.context
            val name = item.name ?: context.getString(R.string.separator_hyphen)
            val gender = item.gender?.lowercase()?.capitalizeFirstChar()
                ?: context.getString(R.string.separator_hyphen)

            val age = item.dateOfBirth?.let {
                CommonUtils.getAgeFromDOB(
                    item.dateOfBirth,
                    context
                )
            } ?: context.getString(R.string.separator_hyphen)

            val patientInfo = context.getString(
                R.string.household_summary_member_info,
                name,
                age,
                CommonUtils.translatedGender(context, gender)
            )
            binding.tvPatientName.text = patientInfo
            binding.tvDueInformation.setTextColor(Color.parseColor("#994242"))
            binding.tvLabelReason.gone()
            if (item.type.equals(NCDFollowUpUtils.SCREENED, true)) {
                val referredReasonsText =
                    item.referredReasons?.filterNot { it.isNullOrBlank() }?.joinToString(", ")
                updateReasonSection(
                    label = context.getString(R.string.reason),
                    text = referredReasonsText,
                    context = context,
                    binding = binding
                )
            }
            if (item.type.equals(NCDFollowUpUtils.LTFU_Type, true)) {
                val overDueCategoriesText = item.overDueCategories
                    ?.asSequence()
                    ?.filter { !it.isNullOrBlank() }
                    ?.joinToString(", ") { it?.capitalizeFirstChar() ?: "" }
                updateReasonSection(
                    label = context.getString(R.string.overdue),
                    text = overDueCategoriesText,
                    context = context,
                    binding = binding
                )
            }
            item.dueDate?.let { dueDate ->
                val value = getDaysDifference(dueDate)?.toLong()

                if (value == null) {
                    binding.tvDueInformation.gone()
                    return@let
                }

                val remainingDaysKey = when (item.type?.lowercase()) {
                    NCDFollowUpUtils.LTFU_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LOST_REMAINING_DAYS
                    NCDFollowUpUtils.SCREENED.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_SCREENING_REMAINING_DAYS
                    NCDFollowUpUtils.Assessment_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_ASSESSMENT_REMAINING_DAYS
                    NCDFollowUpUtils.Defaulters_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_MEDICAL_REVIEW_REMAINING_DAYS
                    else -> null
                }

                remainingDaysKey?.let { key ->
                    val remainingDays = SecuredPreference.getInt(key.name)
                    if (value > remainingDays) {
                        binding.tvDueInformation.visible()
                        val due = if (item.type.equals(NCDFollowUpUtils.LTFU_Type, true)) {
                            value
                        } else {
                            value - remainingDays
                        }
                        binding.tvDueInformation.text = context.getString(
                            NCDFollowUpUtils.getDaysString(due),
                            due
                        )
                    } else {
                        binding.tvDueInformation.gone()
                    }
                } ?: binding.tvDueInformation.gone()
            } ?: binding.tvDueInformation.gone()

            item.retryAttempts?.let {
                binding.ivRecentAttemptCount.apply {
                    visibility = View.VISIBLE
                    text = it.toString()
                }
            } ?: kotlin.run { binding.ivRecentAttemptCount.gone() }
            binding.callButton.safeClickListener {
                listener.onSelectedPatientForCall(item)
            }
            if (item.type.equals(NCDFollowUpUtils.Defaulters_Type, true)) {
                binding.assessmentButton.invisible()
            } else {
                binding.assessmentButton.visible()
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

    fun submitData(data: List<NCDFollowUp>) {
        this.list.clear()
        this.list.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): NCDFollowUpOfflineViewHolder {
        return NCDFollowUpOfflineViewHolder(
            FollowUpListItemPatientsBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: NCDFollowUpOfflineViewHolder, position: Int) {
        holder.bind(list[position])
    }

    override fun getItemCount(): Int {
        return list.size
    }
}