package org.medtroniclabs.uhis.ui.membersearch.adapter

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
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.invisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.CommonUtils.getAgeFromDOB
import org.medtroniclabs.uhis.common.CommonUtils.getGenderText
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.databinding.MembersSummaryListItemBinding
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.formgeneration.extension.capitalizeFirstChar
import org.medtroniclabs.uhis.formgeneration.extension.px
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener
import org.medtroniclabs.uhis.ui.membersearch.model.MemberSearchListItem

/**
 * Paging adapter for hybrid member search results.
 *
 * Local rows navigate to member summary; remote rows fetch details then navigate.
 */
class MemberSearchAdapter(
    private val listener: MemberSelectionListener,
    private val onRemotePatientClick: (PatientListResModel) -> Unit = {},
) : PagingDataAdapter<MemberSearchListItem, MemberSearchAdapter.MemberViewHolder>(ItemComparator) {
    inner class MemberViewHolder(val binding: MembersSummaryListItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        val context: Context = binding.root.context
    }

    override fun getItemViewType(position: Int): Int =
        when (getItem(position)) {
            is MemberSearchListItem.Local -> VIEW_TYPE_LOCAL
            is MemberSearchListItem.Remote -> VIEW_TYPE_REMOTE
            null -> VIEW_TYPE_LOCAL
        }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): MemberViewHolder =
        MemberViewHolder(
            MembersSummaryListItemBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            ),
        )

    override fun onBindViewHolder(
        holder: MemberViewHolder,
        position: Int,
    ) {
        when (val item = getItem(position)) {
            is MemberSearchListItem.Local -> bindLocal(holder, item.member)
            is MemberSearchListItem.Remote -> bindRemote(holder, item.patient)
            null -> Unit
        }
    }

    override fun onViewRecycled(holder: MemberViewHolder) {
        super.onViewRecycled(holder)
        holder.binding.flexTitle.removeAllViews()
    }

    @SuppressLint("SetTextI18n")
    private fun bindLocal(
        holder: MemberViewHolder,
        item: HouseholdMemberWithTb,
    ) {
        val context = holder.context

        holder.binding.clReasonOfDeath.gone()
        holder.binding.forwardIcon.visible()
        holder.binding.cardPatient.isClickable = true
        holder.binding.cardPatient.isFocusable = true
        holder.binding.clPatientRoot.alpha = 1f

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
        holder.binding.tvDiagnosisStatus.setTextColor(ContextCompat.getColor(context, R.color.grey_black))
        val shasthyaShebika = when {
            !item.shasthyaShebikaNameSsId.isNullOrBlank() && !item.shasthyaShebikaName.isNullOrBlank() -> {
                "${item.shasthyaShebikaNameSsId} - ${item.shasthyaShebikaName}"
            }

            !item.shasthyaShebikaName.isNullOrBlank() -> item.shasthyaShebikaName

            else -> context.getString(R.string.separator_double_hyphen)
        }
        holder.binding.tvDiagnosisStatus.text = shasthyaShebika

        val memberName = if (item.isActive) {
            holder.binding.clPatientRoot.setBackgroundResource(R.drawable.default_color_bg)
            disableAllChildren(holder.binding.root, 1f, true)
            getMemberInfoText(context, item).toString()
        } else {
            holder.binding.clPatientRoot.setBackgroundResource(R.drawable.drak_grey_bg)
            holder.binding.clReasonOfDeath.visible()
            holder.binding.forwardIcon.invisible()
            holder.binding.tvReasonForDeath.text =
                item.deceasedReason ?: context.getString(R.string.separator_double_hyphen)
            disableAllChildren(holder.binding.root, 1f, false)
            "${getMemberInfoText(context, item)} (${context.getString(R.string.deceased)})"
        }

        bindTitle(holder, memberName, item.assessmentHistory)

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

    private fun bindRemote(
        holder: MemberViewHolder,
        item: PatientListResModel,
    ) {
        val context = holder.context

        holder.binding.clReasonOfDeath.gone()
        holder.binding.forwardIcon.visible()
        holder.binding.cardPatient.isClickable = true
        holder.binding.cardPatient.isFocusable = true
        holder.binding.clPatientRoot.alpha = 1f
        holder.binding.clPatientRoot.setBackgroundResource(R.drawable.default_color_bg)

        holder.binding.tvRecentService.gone()
        holder.binding.tvRecentServiceSeparator.gone()
        holder.binding.tvRecentServiceValue.gone()
        holder.binding.tvRecentServiceDate.gone()
        holder.binding.tvRecentServiceDateSeparator.gone()
        holder.binding.tvRecentServiceDateValue.gone()

        holder.binding.tvDiagnosis.setText(R.string.national_id)
        holder.binding.tvDiagnosisStatus.text = item.nationalId ?: context.getString(R.string.separator_double_hyphen)
        holder.binding.tvDiagnosisStatus.setTextColor(ContextCompat.getColor(context, R.color.grey_black))

        val name = item.name ?: context.getString(R.string.separator_hyphen)
        val gender = item.gender?.lowercase()?.capitalizeFirstChar()
            ?: context.getString(R.string.separator_hyphen)
        val formattedAge = item.birthDate?.let { DateUtils.getAgeDescription(it, context) }
            ?: context.getString(R.string.separator_hyphen)
        val age = item.age?.toString() ?: formattedAge

        val memberInfo = context.getString(
            R.string.household_summary_member_info,
            name,
            age,
            CommonUtils.translatedGender(context, gender),
        )

        bindTitle(holder, memberInfo, emptyList())
        holder.binding.tvPatientId.text = item.patientId ?: context.getString(R.string.separator_double_hyphen)

        holder.binding.cardPatient.safeClickListener {
            onRemotePatientClick(item)
        }
    }

    private fun bindTitle(
        holder: MemberViewHolder,
        title: String,
        assessmentHistory: List<MemberAssessmentHistoryEntity>,
    ) {
        val context = holder.context
        holder.binding.flexTitle.removeAllViews()
        holder.binding.flexTitle.addView(
            TextView(context).apply {
                layoutParams = ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                )
                setTextAppearance(R.style.TextStyle_Bold_16_NoBG)
                setTextColor(ContextCompat.getColor(context, R.color.grey_black))
                text = title
            },
        )

        val services = assessmentHistory.filterNot { it.serviceProvided.isNullOrBlank() }
        if (services.isNotEmpty()) {
            val iconsToShow = services
                .map(AssessmentUtil::mapServiceToServiceIcon)
                .filterNot { it == View.NO_ID }
                .take(3)

            iconsToShow.forEach { iconRes ->
                holder.binding.flexTitle.addView(
                    ImageView(context).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(28.px, 28.px)
                        setImageResource(iconRes)
                    },
                )
            }

            val remainingCount = services.size - iconsToShow.size
            if (remainingCount > 0) {
                holder.binding.flexTitle.addView(
                    TextView(context).apply {
                        layoutParams = ViewGroup.MarginLayoutParams(28.px, 28.px)
                        text = "+$remainingCount"
                        textSize = 16f
                        gravity = Gravity.CENTER
                        setTextColor(ContextCompat.getColor(context, R.color.base_muted_foreground))
                        setBackgroundResource(R.drawable.bg_more_services)
                    },
                )
            }
        }
    }

    private fun disableAllChildren(
        root: MaterialCardView,
        alpha: Float,
        enabled: Boolean,
    ) {
        for (i in 0 until root.childCount) {
            val child = root.getChildAt(i)
            child.alpha = alpha
            child.isEnabled = enabled
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

    private companion object {
        const val VIEW_TYPE_LOCAL = 0
        const val VIEW_TYPE_REMOTE = 1

        private val ItemComparator = object : DiffUtil.ItemCallback<MemberSearchListItem>() {
            override fun areItemsTheSame(
                oldItem: MemberSearchListItem,
                newItem: MemberSearchListItem,
            ): Boolean =
                when {
                    oldItem is MemberSearchListItem.Local && newItem is MemberSearchListItem.Local ->
                        oldItem.member.id == newItem.member.id

                    oldItem is MemberSearchListItem.Remote && newItem is MemberSearchListItem.Remote ->
                        oldItem.patient.id == newItem.patient.id &&
                            oldItem.patient.patientId == newItem.patient.patientId &&
                            oldItem.patient.programId == newItem.patient.programId

                    else -> false
                }

            override fun areContentsTheSame(
                oldItem: MemberSearchListItem,
                newItem: MemberSearchListItem,
            ): Boolean = oldItem == newItem
        }
    }
}
