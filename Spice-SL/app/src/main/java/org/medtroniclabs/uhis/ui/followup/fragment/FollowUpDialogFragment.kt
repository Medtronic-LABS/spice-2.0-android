package org.medtroniclabs.uhis.ui.followup.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.getPatientStatus
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setDialogWidthAndHeightAsWrapPercent
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.DATE_ddMMyyyy
import org.medtroniclabs.uhis.common.DateUtils.convertDateTimeToDate
import org.medtroniclabs.uhis.databinding.FragmentFollowUpDialogBinding
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import org.medtroniclabs.uhis.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel

class FollowUpDialogFragment : DialogFragment() {
    interface FollowUpClickListener {
        fun onCallClicked()

        fun onLaunchAssessment()
    }

    private lateinit var binding: FragmentFollowUpDialogBinding
    private val viewModel: FollowUpViewModel by activityViewModels()
    private var listener: FollowUpClickListener? = null

    companion object {
        const val TAG = "FollowUpDialogFragment"

        fun newInstance(listener: FollowUpClickListener): FollowUpDialogFragment {
            val frag = FollowUpDialogFragment()
            frag.listener = listener
            return frag
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentFollowUpDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        applyOrientationChange()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        viewModel.setUserJourney(AnalyticsDefinedParams.FollowUPDetailDialogue)
        binding.btnCall.setOnClickListener {
            dismiss()
            listener?.onCallClicked()
        }
        binding.ivClose.setOnClickListener {
            viewModel.setUserJourney(AnalyticsDefinedParams.CLOSEICONTRIGGERED)
            dismiss()
        }

        binding.btnAssessment.setOnClickListener {
            dismiss()
            listener?.onLaunchAssessment()
        }

        viewModel.selectedFollowUpDetail?.let { details ->
            with(binding) {
                tvTitle.text = getPatientName(details.name, details.dateOfBirth, details.gender)
                btnCall.isEnabled = !details.isWrongNumber
                tvReasonText.text = details.getReason(getString(R.string.hyphen_symbol))
                tvPatientStatusText.text = requireContext().getPatientStatus(details.patientStatus)
                    ?: getString(R.string.hyphen_symbol)
                tvVillageText.text = details.village ?: getString(R.string.hyphen_symbol)
                tvLandmarkText.text = details.landmark ?: getString(R.string.hyphen_symbol)
                tvHHNameText.text = details.householdName ?: getString(R.string.hyphen_symbol)
                tvMemberIDText.text = details.patientId ?: getString(R.string.hyphen_symbol)
                tvCallsMadeText.text = getString(
                    R.string.string_slash_string,
                    CommonUtils.formatCountForCurrentLocale(details.attempts),
                    CommonUtils.formatCountForCurrentLocale(viewModel.screeningRetryAttempts),
                )
                tvStartDateText.text = convertDateTimeToDate(
                    details.encounterDate,
                    DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DATE_ddMMyyyy,
                    inUTC = true,
                )

                when (details.type) {
                    FU_TYPE_HH_VISIT -> {
                        btnAssessment.visible()
                        btnCall.gone()
                        tvCallsMadeLabel.gone()
                        tvCallsMadeSeparator.gone()
                        tvCallsMadeText.gone()
                        tvStartDateLabel.text = getString(R.string.treatment_start_date)
                        tvReasonLabel.visible()
                        tvReasonText.visible()
                        tvReasonSeparator.visible()
                    }

                    FU_TYPE_REFERRED -> {
                        btnAssessment.gone()
                        btnCall.visible()
                        tvStartDateLabel.text = getString(R.string.referred_date)
                        tvReasonLabel.visible()
                        tvReasonText.visible()
                        tvReasonSeparator.visible()
                    }

                    FU_TYPE_MEDICAL_REVIEW -> {
                        btnAssessment.gone()
                        btnCall.visible()
                        tvStartDateLabel.text = getString(R.string.treatment_start_date)
                        tvReasonLabel.gone()
                        tvReasonText.gone()
                        tvReasonSeparator.gone()
                    }
                }
            }
        }
    }

    private fun getPatientName(
        name: String?,
        dob: String?,
        gender: String?,
    ): String =
        getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                requireContext(),
            ),
            CommonUtils.getGenderText(gender, requireContext()),
        )

    private fun applyOrientationChange() {
        val tabletSize =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        if (tabletSize) {
            forPhoneAndTab(65, 75)
        } else {
            forPhoneAndTab(65, 90)
        }
    }

    private fun forPhoneAndTab(
        landscape: Int,
        portrait: Int,
    ) {
        when (resources.configuration.orientation) {
            Configuration.ORIENTATION_LANDSCAPE -> {
                setDialogWidthAndHeightAsWrapPercent(landscape)
            }

            Configuration.ORIENTATION_PORTRAIT -> {
                setDialogWidthAndHeightAsWrapPercent(portrait)
            }
        }
    }
}
