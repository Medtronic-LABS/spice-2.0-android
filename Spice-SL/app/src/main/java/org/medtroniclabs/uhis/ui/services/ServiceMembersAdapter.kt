package org.medtroniclabs.uhis.ui.services

import android.annotation.SuppressLint
import android.content.Context
import android.text.SpannableStringBuilder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.getAgeFromDOB
import org.medtroniclabs.uhis.common.CommonUtils.getGenderText
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.databinding.MembersSummaryListItemBinding
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener

class ServiceMembersAdapter(
    private val listener: MemberSelectionListener,
) : RecyclerView.Adapter<ServiceMembersAdapter.HouseholdListViewHolder>() {
    private val membersList = mutableListOf<HouseholdMemberWithTb>()

    fun setMembersList(list: List<HouseholdMemberWithTb>) {
        val oldCount = membersList.size
        membersList.clear()
        membersList.addAll(list)
        notifyItemRangeRemoved(0, oldCount)
        notifyItemRangeInserted(0, list.size)
    }

    class HouseholdListViewHolder(val binding: MembersSummaryListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: HouseholdListViewHolder,
        position: Int,
    ) {
        val context = holder.context
        val item = membersList[position]

        holder.binding.clReasonOfDeath.gone()
        holder.binding.forwardIcon.visible()

        holder.binding.tvRecentService.visible()
        holder.binding.tvRecentServiceSeparator.visible()
        holder.binding.tvRecentServiceValue.visible()
        holder.binding.tvRecentServiceDate.visible()
        holder.binding.tvRecentServiceDateSeparator.visible()
        holder.binding.tvRecentServiceDateValue.visible()

        holder.binding.tvRecentServiceValue.text = item.assessmentHistory
            .firstOrNull { !it.serviceProvided.isNullOrBlank() }
            ?.serviceProvided
            ?.let { recentService -> AssessmentUtil.mapServiceToServiceName(recentService, context) }
            ?: context.resources.getString(R.string.separator_double_hyphen)
        holder.binding.tvRecentServiceDateValue.text = item.recentServiceDate?.let {
            DateUtils.formatDateToDisplayFormat(it)
        } ?: context.resources.getString(R.string.separator_double_hyphen)

        holder.binding.tvDiagnosis.setText(R.string.ss_name)
        holder.binding.tvDiagnosisStatus.setTextColor(ContextCompat.getColor(holder.binding.root.context, R.color.grey_black))
        val shasthyaShebika = when {
            !item.shasthyaShebikaNameSsId.isNullOrBlank() && !item.shasthyaShebikaName.isNullOrBlank() -> {
                "${item.shasthyaShebikaNameSsId} - ${item.shasthyaShebikaName}"
            }

            !item.shasthyaShebikaName.isNullOrBlank() -> {
                item.shasthyaShebikaName
            }

            else -> {
                context.getString(R.string.separator_double_hyphen)
            }
        }
        holder.binding.tvDiagnosisStatus.text = shasthyaShebika

        val memberName: String

        if (item.isActive) {
            memberName = getMemberInfoText(context, item).toString()
            holder.binding.clPatientRoot.setBackgroundResource(R.drawable.default_color_bg)
            disableAllChildren(holder.binding.root, 1f, true)
        } else {
            memberName = "${getMemberInfoText(context, item)} (${context.getString(R.string.deceased)})"
            holder.binding.clPatientRoot.setBackgroundResource(R.drawable.drak_grey_bg)
            holder.binding.clReasonOfDeath.visible()
            holder.binding.forwardIcon.invisible()
            holder.binding.tvReasonForDeath.text =
                item.deceasedReason ?: context.getString(R.string.separator_double_hyphen)
            disableAllChildren(holder.binding.root, 1f, false)
        }

        holder.binding.flexTitle.removeAllViews()
        holder.binding.flexTitle.addView(
            TextView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextAppearance(R.style.TextStyle_Bold_16_NoBG)
                setTextColor(ContextCompat.getColor(context, R.color.grey_black))
                text = memberName
            },
        )

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

        holder.binding.tvPatientId.text = item.patientId ?: context.getString(R.string.separator_double_hyphen)

        holder.binding.cardPatient.safeClickListener {
            if (item.isActive) {
                listener.onMemberSelected(
                    item.id,
                    false,
                    item.dateOfBirth,
                    houseHoldId = item.householdId,
                )
            }
        }
    }

    override fun onViewRecycled(holder: HouseholdListViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.flexTitle.removeAllViews()
    }

    private fun disableAllChildren(
        root: MaterialCardView,
        alpha: Float,
        enabled: Boolean,
    ) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            child.alpha = alpha // Set 50% opacity to indicate a disabled state
            child.isEnabled = enabled // Optionally disable interaction
        }
    }

    private fun getMemberInfoText(
        context: Context,
        item: HouseholdMemberWithTb,
    ): CharSequence =
        SpannableStringBuilder(
            context.getString(
                R.string.household_summary_member_info,
                item.name,
                getAgeFromDOB(item.dateOfBirth, context),
                getGenderText(item.gender, context),
            ),
        )

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): HouseholdListViewHolder =
        HouseholdListViewHolder(
            MembersSummaryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun getItemCount(): Int = membersList.size
}
