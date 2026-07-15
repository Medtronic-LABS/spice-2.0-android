package org.medtroniclabs.uhis.ui.patient.adapter

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.graphics.toColorInt
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.databinding.BdListItemPatientBinding
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ui.patient.PatientSelectionListener
import org.medtroniclabs.uhis.ui.patient.UIConstants

class PatientsListAdapter(
    val listener: PatientSelectionListener,
    val origin: String,
    private val selectedTab: String,
) :
    PagingDataAdapter<PatientListResModel, PatientsListAdapter.PatientsListViewHolder>(
            PatientListComparator,
        ) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): PatientsListViewHolder =
        PatientsListViewHolder(
            BdListItemPatientBinding.inflate(LayoutInflater.from(parent.context), parent, false),
        )

    override fun onBindViewHolder(
        holder: PatientsListViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { searchItem ->
            holder.bind(searchItem)
            holder.binding.cardPatient.safeClickListener {
                listener.onSelectedPatient(searchItem)
            }
            holder.binding.callContainer.safeClickListener {
                listener.onRegisterPatientCall(searchItem)
            }
        }
    }

    inner class PatientsListViewHolder(val binding: BdListItemPatientBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: PatientListResModel) =
            with(binding) {
                val context = binding.root.context
                if (origin == UIConstants.FOLLOW_UP) {
                    if (selectedTab.equals(DefinedParams.RED_RISK, true)) {
                        patientFollowupGroup.visibility = View.VISIBLE
                    } else {
                        patientFollowupGroup.visibility = View.VISIBLE
                    }
                    conditionalVisibility(item, context, binding)

                    item.retryAttempts?.let {
                        binding.ivRecentAttemptCount.apply {
                            visibility = View.VISIBLE
                            text = it.toString()
                        }
                    } ?: kotlin.run { binding.ivRecentAttemptCount.visibility = View.GONE }
                    binding.callContainer.isEnabled = !item.callInitiated
                    binding.tvCallBtn.isEnabled = !item.callInitiated
                } else {
                    patientFollowupGroup.visibility = View.GONE
                }

                val nameTxt = item.name ?: item.firstName?.let { firstName -> StringConverter.appendTexts(firstName, item.lastName) } ?: ""
                val age = item.birthDate?.let { birthDate -> DateUtils.getAge(birthDate) } ?: item.age?.toString()
                tvCardPatientName.text = CommonUtils.capitalize(
                    StringConverter.appendTexts(
                        firstText = nameTxt,
                        age,
                        CommonUtils.getGenderConstant(item.gender),
                        separator = "-",
                    ),
                )

                val identityLabel = getIdentityLabel(context, item.identityType)
                if (identityLabel != null) {
                    tvLabelNationalID.visible()
                    tvCardNationalID.visible()
                    tvLabelNationalID.text = identityLabel
                    tvCardNationalID.text = item.identityValue ?: context.getString(R.string.hyphen_symbol)
                } else {
                    tvLabelNationalID.gone()
                    tvCardNationalID.gone()
                }

                tvCardPatientID.text = item.patientId ?: context.getString(R.string.hyphen_symbol)
                getDrawable(clPatientRoot, item.riskColorCode ?: "#FFFFFFFF")
            }
    }

    private fun getIdentityLabel(
        context: Context,
        identityType: String?,
    ): String? =
        when {
            identityType.isNullOrEmpty() || identityType == DefinedParams.NA -> null
            identityType == DefinedParams.IDENTITY_TYPE_BRN -> context.getString(R.string.brn)
            else -> context.getString(R.string.national_id)
        }

    private fun conditionalVisibility(
        item: PatientListResModel,
        context: Context,
        binding: BdListItemPatientBinding,
    ) {
        with(binding) {
            if (!item.referredReasons.isNullOrEmpty() && selectedTab.isNotEmpty()) {
                tvBPStatus.visibility = View.VISIBLE
                setBpStatus(item.referredReasons, context, binding)
            } else {
                tvBPStatus.visibility = View.INVISIBLE
                binding.tvBPStatus.text = ""
            }
            when {
                item.referredSite != null && item.referredDateSince != null -> {
                    binding.dueDateContainer.visibility = View.VISIBLE
                    if (selectedTab.equals(DefinedParams.RED_RISK, true)) {
                        binding.tvDueDate.visibility = View.INVISIBLE
                    } else {
                        binding.tvDueDate.visibility = View.VISIBLE
                    }
                    if (selectedTab.equals(DefinedParams.SCREENED, true)) {
                        tvDueDate.text =
                            context.getString(
                                getDaysString(item.referredDateSince),
                                item.referredDateSince.toString(),
                                item.referredSite,
                            )
                    } else {
                        tvDueDate.text =
                            context.getString(
                                getDaysStringMR(item.referredDateSince),
                                item.referredDateSince.toString(),
                            )
                    }
                }

                item.referredDateSince != null -> {
                    binding.dueDateContainer.visibility = View.VISIBLE
                    if (selectedTab.equals(DefinedParams.RED_RISK, true)) {
                        binding.tvDueDate.visibility = View.INVISIBLE
                    } else {
                        binding.tvDueDate.visibility = View.VISIBLE
                        tvDueDate.text = context.getString(R.string.overdue_days, item.referredDateSince.toString())
                    }
                }

                item.referredSite != null -> {
                    binding.dueDateContainer.visibility = View.VISIBLE
                    binding.tvDueDate.visibility = View.VISIBLE
                    if (selectedTab.equals(DefinedParams.RED_RISK, true)) {
                        binding.tvDueDate.visibility = View.INVISIBLE
                    } else {
                        binding.tvDueDate.visibility = View.VISIBLE
                        tvDueDate.text = context.getString(R.string.referred_to, item.referredSite)
                    }
                }

                else -> {
                    binding.dueDateContainer.visibility = View.INVISIBLE
                    binding.tvDueDate.visibility = View.INVISIBLE
                    tvDueDate.text = ""
                }
            }
        }
    }

    private fun setBpStatus(
        referredReasons: List<String>,
        context: Context,
        binding: BdListItemPatientBinding,
    ) {
        val bpStatus = StringBuilder()
        referredReasons.forEach { string ->
            bpStatus.append(" ")
            if (string.contains(DefinedParams.BG) &&
                string.contains(DefinedParams.BP)
            ) {
                bpStatus.append(context.getString(R.string.high_both))
            } else if (string.contains(DefinedParams.BP)) {
                bpStatus.append(context.getString(R.string.high_bp))
            } else if (string.contains(DefinedParams.BG)) {
                bpStatus.append(context.getString(R.string.high_bg))
            }
        }
        binding.tvBPStatus.text = bpStatus.trim()
    }

    private fun getDaysStringMR(it: Long): Int = if (it == 1L) R.string.day_due_at_mr else R.string.days_due_at_mr

    private fun getDaysString(it: Long): Int = if (it == 1L) R.string.day_due_at else R.string.days_due_at

    object PatientListComparator : DiffUtil.ItemCallback<PatientListResModel>() {
        override fun areItemsTheSame(
            oldItem: PatientListResModel,
            newItem: PatientListResModel,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: PatientListResModel,
            newItem: PatientListResModel,
        ): Boolean = oldItem == newItem
    }

    fun getDrawable(
        view: ConstraintLayout,
        colorCode: String,
    ) {
        if (view.background != null) {
            val drawable = view.background as GradientDrawable
            drawable.mutate()
            drawable.setStroke(3, colorCode.toColorInt())
        }
    }
}
