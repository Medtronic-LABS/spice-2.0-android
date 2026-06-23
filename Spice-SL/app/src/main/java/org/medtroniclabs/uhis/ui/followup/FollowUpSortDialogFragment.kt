package org.medtroniclabs.uhis.ui.followup

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.databinding.FragmentSortingDialogBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.model.followup.FollowUpSortOrder
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel

/**
 * Sorting dialog for followup referral search
 */
@AndroidEntryPoint
class FollowUpSortDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentSortingDialogBinding

    private val followUpViewModel: FollowUpViewModel by activityViewModels()

    companion object {
        const val TAG = "SortingDialogFragment"

        fun newInstance(): FollowUpSortDialogFragment = FollowUpSortDialogFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentSortingDialogBinding.inflate(inflater, container, false)
        isCancelable = false
        val window: Window? = dialog?.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        return binding.root
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initViews()
        prefillValues()
        setListeners()
    }

    fun initViews() {
        binding.rbRedRisk.setText(R.string.latest_screening_date)
        binding.rbLatestAssessment.setText(R.string.oldest_screening_date)
        binding.rbBP.gone()
        binding.rbBG.gone()
        binding.rbMedicalReview.gone()
        binding.rbAssessmentDueDate.gone()
    }

    private fun prefillValues() {
        binding.apply {
            val sortOrder = followUpViewModel.getFilterData()?.sortOrder
            when (sortOrder) {
                FollowUpSortOrder.LATEST_SCREENING_DATE -> {
                    binding.rbRedRisk.isChecked = true
                }

                FollowUpSortOrder.OLDEST_SCREENING_DATE -> {
                    binding.rbLatestAssessment.isChecked = true
                }

                else -> binding.rgSortCondition.clearCheck()
            }
        }
        handleResetButtons()
    }

    private fun setListeners() {
        binding.rgSortCondition.setOnCheckedChangeListener { _, checkedId ->
            val sortOrder = when (checkedId) {
                R.id.rbRedRisk -> {
                    FollowUpSortOrder.LATEST_SCREENING_DATE
                }

                R.id.rbLatestAssessment -> {
                    FollowUpSortOrder.OLDEST_SCREENING_DATE
                }

                else -> {
                    null
                }
            }
            // When we call clearCheck() on radio group,
            // Then it triggers CheckedChangeListener 2 times
            // Once with the id of first radio button in the group
            // Once with -1
            // Hence added a check if any of the radio button actually checked to apply filter
            val isAnyChecked = binding.rbRedRisk.isChecked ||
                binding.rbLatestAssessment.isChecked ||
                binding.rbBP.isChecked
            if (isAnyChecked && sortOrder != null) {
                followUpViewModel.setUserJourney(AnalyticsDefinedParams.HOUSE_HOLD_SORT_APPLY_TRIGGERED)
                followUpViewModel.updateFollowUpFilter(
                    sortOrder = sortOrder,
                )
                handleResetButtons()
                dismiss()
            }
        }
        binding.btnReset.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                followUpViewModel.setUserJourney(AnalyticsDefinedParams.HOUSE_HOLD_SORT_RESET_TRIGGERED)
                dismiss()
                followUpViewModel.updateFollowUpFilter(
                    sortOrder = FollowUpSortOrder.DEFAULT,
                )
            }

            binding.labelHeader.ivClose.id -> dismiss()
            binding.btnReset.id -> doReset()
        }
    }

    private fun doReset() {
        binding.apply {
            rgSortCondition.clearCheck()
            btnDone.isEnabled = true
        }
    }

    private fun handleResetButtons() {
        binding.btnReset.isEnabled = binding.rgSortCondition.checkedRadioButtonId != -1
    }
}
