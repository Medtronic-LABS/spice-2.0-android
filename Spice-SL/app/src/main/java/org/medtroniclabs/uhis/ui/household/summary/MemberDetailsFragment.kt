package org.medtroniclabs.uhis.ui.household.summary

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.textOrDoubleHyphen
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_DD_MMMM_YYYY
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.calculateGestationalAge
import org.medtroniclabs.uhis.common.DateUtils.formatGestationalAge
import org.medtroniclabs.uhis.common.DateUtils.getLastMenstrualDate
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.FragmentMemberDetailsBinding
import org.medtroniclabs.uhis.databinding.SummaryListItemBinding
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationActivity
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationFragment
import org.medtroniclabs.uhis.ui.household.HouseholdActivity
import org.medtroniclabs.uhis.ui.household.viewmodel.MemberSummaryViewModel

class MemberDetailsFragment : Fragment(), View.OnClickListener {
    private lateinit var binding: FragmentMemberDetailsBinding
    private val memberSummaryViewModel: MemberSummaryViewModel by activityViewModels()
    private var isExternalMember: Boolean = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentMemberDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        attachObservers()
        setListeners()
    }

    private fun attachObservers() {
        memberSummaryViewModel.memberDetails.observe(viewLifecycleOwner) { memberDetails ->
            memberDetails ?: return@observe
            binding.llDetails.removeAllViews()
            isExternalMember = memberDetails.member.householdId == null
            val recentHistoryDate = memberDetails.history.firstOrNull()?.let {
                DateUtils.getLastMenstrualDate(it.visitDate ?: "").timeInMillis
            } ?: 0
            val updatedAt = memberDetails.member.updatedAt
            val lastActivity = when {
                recentHistoryDate == 0L && updatedAt == 0L -> {
                    null
                }

                recentHistoryDate < updatedAt -> {
                    DateUtils.formatDateToDisplayFormat(updatedAt)
                }

                else -> {
                    DateUtils.formatDateToDisplayFormat(recentHistoryDate)
                }
            } ?: getString(R.string.separator_double_hyphen)

            val registeredAt = DateUtils.formatDateToDisplayFormat(memberDetails.member.createdAt)
            val servicesProvided = if (memberDetails.history.isNotEmpty()) {
                memberDetails.history.distinctBy { history -> history.serviceProvided }.joinToString { history ->
                    AssessmentUtil.mapServiceToServiceName(history.serviceProvided.orEmpty(), requireContext())
                }
            } else {
                getString(R.string.separator_double_hyphen)
            }
            addSummaryView(getString(R.string.registration_date), registeredAt ?: getString(R.string.separator_double_hyphen))
            addSummaryView(getString(R.string.patient_id), memberDetails.member.patientId ?: getString(R.string.separator_double_hyphen))
            addSummaryView(getString(R.string.mobile_number), memberDetails.member.phoneNumber ?: getString(R.string.separator_double_hyphen))
            addSummaryView(getString(R.string.last_visit_date), lastActivity)
            addSummaryView(getString(R.string.services_provided), servicesProvided)
            val recentService = memberDetails.history
                .firstOrNull()
                ?.serviceProvided
                ?.lowercase()
                .orEmpty()
            when (recentService) {
                MenuConstants.PREGNANT_WOMEN_PROFILE.lowercase(),
                MenuConstants.ANC.lowercase(),
                -> {
                    addPregnancySummaryViews(memberDetails.recentPregnancy)
                }

                else -> {
                    addSummaryView(
                        getString(R.string.recent_status),
                        memberDetails.history.firstOrNull()?.let { history ->
                            AssessmentUtil.mapServiceToServiceName(history.serviceProvided.orEmpty(), requireContext())
                        } ?: getString(R.string.separator_double_hyphen),
                    )
                }
            }
            if (memberDetails.member.isActive) {
                binding.tvEdit.visible()
            } else {
                binding.tvEdit.gone()
            }
        }
    }

    private fun setListeners() {
        binding.tvEdit.setOnClickListener(this)
    }

    /**
     * Binds Gestational age and EDD in case of PW Registration or ANC
     */
    private fun addPregnancySummaryViews(pregnancyDetail: PregnancyDetail?) {
        val gestationalAge = pregnancyDetail?.lastMenstrualPeriod?.let { lmp ->
            try {
                formatGestationalAge(
                    calculateGestationalAge(getLastMenstrualDate(lmp)),
                    requireContext(),
                )
            } catch (_: Exception) {
                null
            }
        }
        addSummaryView(
            getString(R.string.gestational_age),
            gestationalAge.textOrDoubleHyphen(),
        )

        val formattedEdd = pregnancyDetail?.estimatedDeliveryDate?.let { edd ->
            DateUtils.convertDateFormat(
                edd,
                DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                DATE_FORMAT_DD_MMMM_YYYY,
            )
        }
        addSummaryView(
            getString(R.string.estimated_delivery_date),
            formattedEdd.textOrDoubleHyphen(),
        )
    }

    private fun addSummaryView(
        name: String,
        value: String,
    ) {
        val view = SummaryListItemBinding.inflate(LayoutInflater.from(context))
        view.tvLabel.text = name
        view.tvValue.text = value
        binding.llDetails.addView(view.root)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.tvEdit.id -> {
                val intent = if (isExternalMember) {
                    Intent(requireContext(), ExternalMemberRegistrationActivity::class.java).apply {
                        putExtra(ExternalMemberRegistrationFragment.ARG_IS_EDIT_MODE, true)
                    }
                } else {
                    Intent(requireContext(), HouseholdActivity::class.java)
                }
                intent.putExtra(DefinedParams.MEMBER_ID, memberSummaryViewModel.memberId)
                if (!isExternalMember) {
                    intent.putExtra(DefinedParams.householdId, memberSummaryViewModel.householdId)
                }
                startActivity(intent)
            }
        }
    }
}
