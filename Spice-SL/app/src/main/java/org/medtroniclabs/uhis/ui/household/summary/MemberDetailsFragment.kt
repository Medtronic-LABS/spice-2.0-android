package org.medtroniclabs.uhis.ui.household.summary

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
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
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_DD_MMMM_YYYY
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.calculateGestationalAge
import org.medtroniclabs.uhis.common.DateUtils.convertToTimestamp
import org.medtroniclabs.uhis.common.DateUtils.formatGestationalAge
import org.medtroniclabs.uhis.common.DateUtils.getLastMenstrualDate
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.FragmentMemberDetailsBinding
import org.medtroniclabs.uhis.databinding.SummaryListItemBinding
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.rmnch.PregnancyCohortRules
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationActivity
import org.medtroniclabs.uhis.ui.externalmember.ExternalMemberRegistrationFragment
import org.medtroniclabs.uhis.ui.household.HouseholdActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION
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
            val householdId = memberDetails.member.householdId
            isExternalMember = householdId == null
            val recentHistoryDate = memberDetails.history.firstOrNull()?.let {
                getLastMenstrualDate(it.visitDate ?: "").timeInMillis
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
            val householdHeadName = memberDetails.householdHeadName
            if (householdHeadName.isNullOrBlank()) {
                addSummaryView(getString(R.string.patient_id), memberDetails.member.patientId ?: getString(R.string.separator_double_hyphen))
            } else {
                val familyBinding = addSummaryView(getString(R.string.family), getString(R.string.household_family, householdHeadName))
                val tvValue = familyBinding.tvValue
                tvValue.setTextColor(Color.BLUE)
                tvValue.paintFlags = tvValue.paintFlags or Paint.UNDERLINE_TEXT_FLAG
                tvValue.setOnClickListener {
                    activity?.let { safeActivity ->
                        val intent = Intent(safeActivity, HouseholdSummaryActivity::class.java)
                        intent.putExtra(DefinedParams.householdId, householdId)
                        intent.putExtra(IS_FROM_HOUSEHOLD_REGISTRATION, false)
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                        startActivity(intent)
                        safeActivity.finish()
                    }
                }
            }
            addSummaryView(getString(R.string.mobile_number), memberDetails.member.phoneNumber ?: getString(R.string.separator_double_hyphen))
            addSummaryView(getString(R.string.last_visit_date), lastActivity)
            addSummaryView(getString(R.string.services_provided), servicesProvided)
            if (memberDetails.member.isActive) {
                binding.tvEdit.visible()
            } else {
                binding.tvEdit.gone()
            }
            val recentPregnancy = memberDetails.memberPregnancyDetails?.firstOrNull()
            val latestFpVisit = memberDetails.history.firstOrNull {
                MenuConstants.FP_MENU_ID.equals(it.serviceProvided.orEmpty(), true)
            }
            var showRecentStatus = true
            when {
                recentPregnancy != null && PregnancyCohortRules.isActivePregnancy(recentPregnancy) -> {
                    showRecentStatus = !addPregnancySummaryViews(recentPregnancy)
                }

                recentPregnancy != null && PregnancyCohortRules.isPostnatal(recentPregnancy) -> {
                    showRecentStatus = !addDeliveryOutcomeSummaryViews(memberDetails.history, recentPregnancy)
                }

                recentPregnancy != null &&
                    !recentPregnancy.typeOfAbortion.isNullOrBlank() &&
                    !hasVisitAfterAbortion(memberDetails.history, recentPregnancy) -> {
                    showRecentStatus = !addDeliveryOutcomeSummaryViews(memberDetails.history, recentPregnancy)
                }

                latestFpVisit != null -> {
                    showRecentStatus = !addFPSummaryView(latestFpVisit)
                }
            }
            if (showRecentStatus) {
                addSummaryView(
                    getString(R.string.recent_status),
                    memberDetails.history.firstOrNull()?.let { history ->
                        AssessmentUtil.mapServiceToServiceName(history.serviceProvided.orEmpty(), requireContext())
                    } ?: getString(R.string.separator_double_hyphen),
                )
            }
        }
    }

    private fun hasVisitAfterAbortion(
        history: List<MemberAssessmentHistoryEntity>,
        pregnancyDetail: PregnancyDetail,
    ): Boolean {
        val outcomeVisit = history.firstOrNull {
            it.observations?.pregnancyEpisodeId == pregnancyDetail.pregnancyEpisodeId &&
                MenuConstants.PREGNANCY_OUTCOME.equals(it.serviceProvided.orEmpty(), true)
        }
        val abortionAnchorDate = outcomeVisit?.visitDate ?: return false
        val anchorMillis = convertToTimestamp(abortionAnchorDate, 0)
        return history.any { visit ->
            val visitMillis = convertToTimestamp(visit.visitDate.orEmpty(), 0)
            visitMillis > anchorMillis
        }
    }

    private fun setListeners() {
        binding.tvEdit.setOnClickListener(this)
    }

    /**
     * Binds Gestational age and EDD in case of PW Registration or ANC
     * Returns true if it is able to add pregnancy summary view
     */
    private fun addPregnancySummaryViews(pregnancyDetail: PregnancyDetail?): Boolean {
        pregnancyDetail ?: return false
        val gestationalAge = pregnancyDetail.lastMenstrualPeriod?.let { lmp ->
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

        val formattedEdd = pregnancyDetail.estimatedDeliveryDate?.let { edd ->
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
        return true
    }

    /**
     * Binds pregnancy outcome and delivery date in case of pregnancy outcome or PNC
     * Returns true if it is able to add delivery date
     */
    private fun addDeliveryOutcomeSummaryViews(
        recentHistory: List<MemberAssessmentHistoryEntity>?,
        pregnancyDetail: PregnancyDetail?,
    ): Boolean {
        // Pregnacy Outcome - Live Births (Number) /Still Births (Number)/Maternal Death/Abortion/Newborn Death (Number)
        pregnancyDetail ?: return false
        var returnResult = false
        val observations = recentHistory
            ?.find {
                it.observations?.pregnancyEpisodeId == pregnancyDetail.pregnancyEpisodeId &&
                    MenuConstants.PREGNANCY_OUTCOME.equals(it.serviceProvided.orEmpty(), true)
            }?.observations
        val pregnancyOutcomeValues = mutableListOf<String>()
        if (DefinedParams.YES.equals(observations?.abortion.orEmpty(), true)) {
            pregnancyOutcomeValues.add(getString(R.string.abortion))
        } else {
            if (!observations?.modeOfDelivery.isNullOrBlank()) {
                pregnancyOutcomeValues.add(
                    getString(
                        R.string.live_birth_s,
                        CommonUtils.formatCountForCurrentLocale(CommonUtils.getInteger(observations.liveBirthNumbers)),
                    ),
                )
                pregnancyOutcomeValues.add(
                    getString(
                        R.string.still_birth_s,
                        CommonUtils.formatCountForCurrentLocale(CommonUtils.getInteger(observations.stillbirthNumbers)),
                    ),
                )
                if (DefinedParams.YES.equals(observations.maternalDeath.orEmpty(), true)) {
                    pregnancyOutcomeValues.add(getString(R.string.maternal_death))
                }
                pregnancyOutcomeValues.add(
                    getString(
                        R.string.newborn_death_s,
                        CommonUtils.formatCountForCurrentLocale(CommonUtils.getInteger(observations.newbornDeathNumbers)),
                    ),
                )
            } else if (DefinedParams.YES.equals(observations?.maternalDeath.orEmpty(), true)) {
                pregnancyOutcomeValues.add(getString(R.string.maternal_death))
            }
        }
        if (pregnancyOutcomeValues.isNotEmpty()) {
            addSummaryView(
                getString(R.string.pregnancy_outcome),
                pregnancyOutcomeValues.joinToString("/"),
            )
            returnResult = true
        }
        if (pregnancyDetail.typeOfAbortion.isNullOrBlank() && !pregnancyDetail.dateOfDelivery.isNullOrBlank()) {
            val formattedDeliveryDate = pregnancyDetail.dateOfDelivery?.let { dateOfDelivery ->
                DateUtils.convertDateFormat(
                    dateOfDelivery,
                    DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DATE_FORMAT_DD_MMMM_YYYY,
                )
            }
            addSummaryView(
                getString(R.string.date_of_delivery),
                formattedDeliveryDate.textOrDoubleHyphen(),
            )
            returnResult = true
        }
        return returnResult
    }

    private fun addFPSummaryView(recentHistory: MemberAssessmentHistoryEntity): Boolean {
        if (!recentHistory.observations?.numberOfLivingChildren.isNullOrBlank()) {
            addSummaryView(
                getString(R.string.no_of_living_children),
                CommonUtils.formatCountForCurrentLocale(CommonUtils.getInteger(recentHistory.observations.numberOfLivingChildren)),
            )
            return true
        }
        return false
    }

    private fun addSummaryView(
        name: String,
        value: String,
    ): SummaryListItemBinding {
        val itemBinding = SummaryListItemBinding.inflate(LayoutInflater.from(context))
        itemBinding.tvLabel.text = name
        itemBinding.tvValue.text = value
        binding.llDetails.addView(itemBinding.root)
        return itemBinding
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
