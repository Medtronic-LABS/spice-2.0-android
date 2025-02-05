package com.medtroniclabs.opensource.ncd.followup.fragment

import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setDialogWidthAndHeightAsWrapPercent
import com.medtroniclabs.opensource.appextensions.setVisible
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.databinding.FragmentNcdFollowUpDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.LTFU_Type
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.SCREENED
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils.getDaysString
import com.medtroniclabs.opensource.ncd.followup.viewmodel.NCDFollowUpViewModel
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUp
import com.medtroniclabs.opensource.ui.mypatients.PatientSelectionListenerForFollowUpOffline

class NCDFollowUpDialogFragment : DialogFragment() {

    private lateinit var binding: FragmentNcdFollowUpDialogBinding
    private val viewModel: NCDFollowUpViewModel by activityViewModels()
    private var listener: PatientSelectionListenerForFollowUp? = null
    private var listenerForFollowUp: PatientSelectionListenerForFollowUpOffline? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentNcdFollowUpDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    companion object {
        const val TAG = "NCDFollowUpDialogFragment"
        fun newInstance(listener: PatientSelectionListenerForFollowUp): NCDFollowUpDialogFragment {
            val frag = NCDFollowUpDialogFragment()
            frag.listener = listener
            return frag
        }
        fun newInstance(listener: PatientSelectionListenerForFollowUpOffline): NCDFollowUpDialogFragment {
            val frag = NCDFollowUpDialogFragment()
            frag.listenerForFollowUp = listener
            return frag
        }
    }

    override fun onStart() {
        super.onStart()
        applyOrientationChange()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
    }

    private fun initViews() {
        // Setup button click listeners
        binding.apply {
            btnCall.setOnClickListener {
                dismiss()
                viewModel.selectedPatient?.let { listener?.onSelectedPatientForCall(it) }
                viewModel.selectedFollowUpPatient?.let { listenerForFollowUp?.onSelectedPatientForCall(it) }
            }

            ivClose.setOnClickListener {
                viewModel.selectedPatient = null
                viewModel.selectedFollowUpPatient = null
                dismiss()
            }

            btnAssessment.setOnClickListener {
                dismiss()
                viewModel.selectedPatient?.let { listener?.onSelectedPatientForAssessment(it) }
                viewModel.selectedFollowUpPatient?.let { listenerForFollowUp?.onSelectedPatientForAssessment(it) }
            }
        }
        binding.tvReasonText.setTextColor(Color.parseColor("#994242"))
        // Populate UI elements with patient data
        viewModel.selectedPatient?.let { patient ->
            with(binding) {
                val hyphen = getString(R.string.hyphen_symbol)
                val name = patient.name ?: hyphen
                val gender = patient.gender?.lowercase()?.capitalizeFirstChar() ?: hyphen
                val age = patient.age?.toString() ?: ""

                // Patient info
                tvTitle.text = getString(R.string.household_summary_member_info, name, age, CommonUtils.translatedGender(requireContext(), gender))

                // Diagnosis and referred reasons
                val isVisible = viewModel.type == SCREENED || viewModel.type == LTFU_Type
                tvReasonLabel.setVisible(isVisible)
                tvReasonText.setVisible(isVisible)
                tvReasonSeparator.setVisible(isVisible)

                tvReasonLabel.text = getString(R.string.reason)
                tvReasonText.text = patient.referredReasons
                    ?.filterNot { it.isBlank() }
                    ?.joinToString(", ")
                    ?: hyphen

                setField(tvStartDateLabel,
                    tvStartDateText,
                    getString(R.string.day_since_last_review),
                    patient.referredDateSince?.let {
                        if (it > 0) {
                            getString(getDaysString(it), it)
                        } else {
                            null
                        }
                    })

                setField(
                    tvCountyLabel,
                    tvCountyText,
                    getString(R.string.county),
                    patient.countyName
                )
                setField(
                    tvSubCountyLabel,
                    tvSubCountyText,
                    getString(R.string.sub_county),
                    patient.subCountyName
                )
                setField(
                    tvChuLabel, tvChuText,
                    getString(R.string.community_health_unit),
                    patient.communityHealthUnitName
                )
                setField(
                    tvVillageLabel,
                    tvVillageText,
                    getString(R.string.village),
                    patient.villageName
                )
                setField(
                    tvLandmarkLabel,
                    tvLandmarkText,
                    getString(R.string.landmark),
                    patient.landmark
                )
                setField(
                    tvNationalIDLabel, tvNationalIDText,
                    getString(R.string.national_id),
                    patient.identityValue
                )
                // Show buttons
                btnAssessment.visible()
                btnCall.visible()
            }
        }
        viewModel.selectedFollowUpPatient?.let { patient ->
            with(binding) {
                val hyphen = getString(R.string.hyphen_symbol)
                val name = patient.name ?: hyphen
                val gender = patient.gender?.lowercase()?.capitalizeFirstChar() ?: hyphen
                val age = patient.dateOfBirth?.let {
                    CommonUtils.getAgeFromDOB(
                        patient.dateOfBirth,
                        requireContext()
                    )
                } ?: hyphen

                // Patient info
                tvTitle.text = getString(R.string.household_summary_member_info, name, age, CommonUtils.translatedGender(requireContext(), gender))
                val isVisible = viewModel.typeOffline == SCREENED || viewModel.typeOffline == LTFU_Type
                tvReasonLabel.setVisible(isVisible)
                tvReasonText.setVisible(isVisible)
                tvReasonSeparator.setVisible(isVisible)
                // Diagnosis and referred reasons
                tvReasonLabel.text = getString(R.string.diagnosis)
                tvReasonText.text = patient.referredReasons
                    ?.filterNot { it.isNullOrBlank() }
                    ?.joinToString(", ")
                    ?: hyphen

                patient.dueDate?.let { dueDate ->
                    val daysDifference = DateUtils.getDaysDifference(dueDate)?.toLong()
                    val label = getString(R.string.day_since_last_review)

                    val remainingDaysKey = when (patient.type?.lowercase()) {
                        LTFU_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_LOST_REMAINING_DAYS
                        SCREENED.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_SCREENING_REMAINING_DAYS
                        NCDFollowUpUtils.Assessment_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_ASSESSMENT_REMAINING_DAYS
                        NCDFollowUpUtils.Defaulters_Type.lowercase() -> SecuredPreference.EnvironmentKey.NCD_FOLLOW_UP_MEDICAL_REVIEW_REMAINING_DAYS
                        else -> null
                    }

                    val text = remainingDaysKey?.let { key ->
                        val remainingDays = SecuredPreference.getInt(key.name).toLong()
                        if (daysDifference == null || daysDifference <= remainingDays) {
                            getString(R.string.hyphen_symbol)
                        } else {
                            val adjustedValue = if (viewModel.typeOffline == LTFU_Type) {
                                daysDifference
                            } else {
                                daysDifference - remainingDays
                            }
                            if (adjustedValue > 0) {
                                getString(getDaysString(adjustedValue), adjustedValue)
                            } else {
                                getString(R.string.hyphen_symbol)
                            }
                        }
                    } ?: getString(R.string.hyphen_symbol)

                    setField(tvStartDateLabel, tvStartDateText, label, text)
                } ?: setField(
                    tvStartDateLabel,
                    tvStartDateText,
                    getString(R.string.day_since_last_review),
                    getString(R.string.hyphen_symbol)
                )

                setField(
                    tvCountyLabel,
                    tvCountyText,
                    getString(R.string.county),
                    patient.countyName
                )
                setField(
                    tvSubCountyLabel,
                    tvSubCountyText,
                    getString(R.string.sub_county),
                    patient.subCountyName
                )
                setField(
                    tvChuLabel, tvChuText,
                    getString(R.string.community_health_unit),
                    patient.communityHealthUnitName
                )
                setField(
                    tvVillageLabel,
                    tvVillageText,
                    getString(R.string.village),
                    patient.villageName
                )
                setField(
                    tvLandmarkLabel,
                    tvLandmarkText,
                    getString(R.string.landmark),
                    patient.landmark
                )
                setField(
                    tvNationalIDLabel, tvNationalIDText,
                    getString(R.string.national_id),
                    patient.identityValue
                )
                // Show buttons
                btnAssessment.visible()
                btnCall.visible()
            }
        }
    }

    // General fields with labels and data
    private fun setField(labelView: TextView, dataView: TextView, label: String, data: String?) {
        labelView.text = label
        dataView.text = data?.capitalizeFirstChar() ?: getString(R.string.hyphen_symbol)
    }
}