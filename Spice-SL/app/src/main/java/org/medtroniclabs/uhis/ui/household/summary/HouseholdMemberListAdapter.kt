package org.medtroniclabs.uhis.ui.household.summary

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
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
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.household.MemberSelectionListener

class HouseholdMemberListAdapter(
    private val houseHoldMembersList: List<HouseholdMemberWithTb>,
    private val listener: MemberSelectionListener,
    private val phuWalkInsFlow: Boolean,
    private val isTranslationEnabled: Boolean,
) : RecyclerView.Adapter<HouseholdMemberListAdapter.HouseholdListViewHolder>(),
    View.OnClickListener {
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
        val item = houseHoldMembersList[position]

        holder.binding.clReasonOfDeath.gone()
        holder.binding.forwardIcon.visible()

        if (phuWalkInsFlow) {
            if (item.diagnoses != null && item.diagnoses.trim().isNotEmpty()) {
                holder.binding.tvDiagnosisStatus.text = item.diagnoses
                holder.binding.tvDiagnosisStatus.setTextColor(
                    ColorStateList.valueOf(context.getColor(R.color.a_red_error)),
                )
            } else {
                holder.binding.tvDiagnosisStatus.text =
                    context.resources.getString(R.string.separator_double_hyphen)
                holder.binding.tvDiagnosisStatus.setTextColor(
                    ColorStateList.valueOf(context.getColor(R.color.grey_black)),
                )
            }
        } else {
            holder.binding.tvDiagnosis.gone()
            holder.binding.tvDiagnosisSeparator.gone()
            holder.binding.tvDiagnosisSeparator.gone()
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
        }

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
                item.deceasedReason
                    ?.let(::formatDeceasedReasonForDisplay)
                    ?.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.separator_double_hyphen)
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

        if (!phuWalkInsFlow) {
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
        } else {
            holder.binding.forwardIcon.gone()
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

    /**
     * Formats deceased reason for UI, mapping encoded neonatal/maternal cause ids to labels.
     *
     * Supported encoded formats:
     * - `__neonatal__:<id1,id2>`
     * - `__mother__:<id1,id2>`
     */
    private fun formatDeceasedReasonForDisplay(reason: String): String {
        val trimmedReason = reason.trim()
        val (type, causeIds) = parseEncodedDeceasedReason(trimmedReason) ?: return trimmedReason
        val mappedReasons = mapCauseIdsToDisplayValues(type, causeIds)
        if (mappedReasons.isBlank()) return trimmedReason
        val typeLabel = getTypeLabel(type).ifBlank { type }
        return "$typeLabel($mappedReasons)"
    }

    /**
     * Parses type and cause ids from encoded deceased reason value.
     */
    private fun parseEncodedDeceasedReason(reason: String): Pair<String, List<String>>? {
        val parts = reason.split(":", limit = 2)
        if (parts.size != 2) return null
        val typePrefix = parts[0].trim()
        val normalizedType =
            when (typePrefix.lowercase()) {
                RMNCH.DECEASED_REASON_PREFIX_NEONATAL -> RMNCH.DEATH_TYPE_NEONATAL
                RMNCH.DECEASED_REASON_PREFIX_MOTHER -> RMNCH.DEATH_TYPE_MOTHER
                else -> return null
            }
        val causeIds =
            parts[1]
                .split(",")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
        return normalizedType to causeIds
    }

    /**
     * Maps cause ids to translated or default labels based on app language setting.
     */
    private fun mapCauseIdsToDisplayValues(
        type: String,
        causeIds: List<String>,
    ): String {
        if (causeIds.isEmpty()) return ""
        val options =
            if (type == RMNCH.DEATH_TYPE_NEONATAL) {
                RMNCH.neonatalDeathCauseOptions
            } else {
                RMNCH.maternalDeathCauseOptions
            }
        val optionMap =
            options.associateBy(
                keySelector = { it["id"]?.toString().orEmpty().lowercase() },
                valueTransform = {
                    if (isTranslationEnabled) {
                        it["cultureValue"]?.toString().orEmpty().ifBlank { it["name"]?.toString().orEmpty() }
                    } else {
                        it["name"]?.toString().orEmpty()
                    }
                },
            )
        return causeIds.joinToString(", ") { causeId ->
            optionMap[causeId.lowercase()] ?: causeId
        }
    }

    /**
     * Resolves translated/non-translated type label from shared RMNCH options.
     */
    private fun getTypeLabel(type: String): String {
        val option =
            when (type) {
                RMNCH.DEATH_TYPE_NEONATAL -> RMNCH.neonatalDeathTypeOptions.firstOrNull { it["id"] == RMNCH.DEATH_TYPE_NEONATAL }
                RMNCH.DEATH_TYPE_MOTHER -> RMNCH.maternalDeathTypeOptions.firstOrNull { it["id"] == RMNCH.DEATH_TYPE_MOTHER }
                else -> null
            } ?: return ""
        return if (isTranslationEnabled) {
            option["cultureValue"]?.toString().orEmpty().ifBlank { option["name"]?.toString().orEmpty() }
        } else {
            option["name"]?.toString().orEmpty()
        }
    }

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

    override fun getItemCount(): Int = houseHoldMembersList.size

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.tvContactTracingStatus -> {
                val pos = view.tag as Int
                listener.onMemberSelected(
                    houseHoldMembersList[pos].id,
                    true,
                    houseHoldMembersList[pos].dateOfBirth,
                    true,
                )
            }
        }
    }
}
