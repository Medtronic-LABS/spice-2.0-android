package com.medtroniclabs.opensource.ui.followup.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.getPatientStatus
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.setDialogWidthAndHeightAsWrapPercent
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.convertDateTimeToDate
import com.medtroniclabs.opensource.databinding.FragmentFollowUpDialogBinding
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_HH_VISIT
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_MEDICAL_REVIEW
import com.medtroniclabs.opensource.ui.followup.FollowUpDefinedParams.FU_TYPE_REFERRED
import com.medtroniclabs.opensource.ui.followup.viewmodel.FollowUpViewModel

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
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentFollowUpDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        applyOrientationChange()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        binding.btnCall.setOnClickListener {
            dismiss()
            listener?.onCallClicked()
        }
        binding.ivClose.setOnClickListener {
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
                tvCallsMadeText.text =
                    "${details.successfulAttempts}/${viewModel.maxSuccessfulCallLimit}"
                tvStartDateText.text = convertDateTimeToDate(
                    details.encounterDate,
                    DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                    DATE_ddMMyyyy,
                    inUTC = true
                )

                when (details.type) {
                    FU_TYPE_HH_VISIT -> {
                        btnAssessment.visible()
                        btnCall.gone()
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
        gender: String?
    ): String {
        return getString(
            R.string.household_summary_member_info,
            name,
            CommonUtils.getAgeFromDOB(
                dob,
                requireContext()
            ),
            CommonUtils.getGenderText(gender, requireContext())
        )
    }

    private fun applyOrientationChange() {
        val tabletSize =
            resources.getBoolean(R.bool.isLargeTablet) || resources.getBoolean(R.bool.isTablet)
        if (tabletSize) {
            forPhoneAndTab(65, 75)
        } else {
            forPhoneAndTab(65, 90)
        }

    }

    private fun forPhoneAndTab(landscape: Int, portrait: Int) {
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