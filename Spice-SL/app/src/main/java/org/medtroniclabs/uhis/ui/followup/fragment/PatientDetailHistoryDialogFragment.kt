package org.medtroniclabs.uhis.ui.followup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.BundleCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils.checkIsTablet
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.databinding.DialogFragmentPatientHistoryBinding
import org.medtroniclabs.uhis.ui.followup.customview.PatientHistoryCardView
import org.medtroniclabs.uhis.ui.followup.viewmodel.FollowUpViewModel
import kotlin.getValue

@AndroidEntryPoint
class PatientDetailHistoryDialogFragment() : DialogFragment() {
    private lateinit var binding: DialogFragmentPatientHistoryBinding
    private val viewModel: FollowUpViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogFragmentPatientHistoryBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        return binding.root
    }

    private fun updatePatientHistoryCardView() {
        binding.llCardHolder.removeAllViews()
        arguments?.let { arguments ->
            val history = BundleCompat.getParcelableArrayList(arguments, HISTORY_DATA, PatientHistoryData::class.java)
            val showCallButton = arguments.getBoolean(SHOW_CALL_BUTTON)
            history?.forEach { item ->
                val view = PatientHistoryCardView(requireContext())
                view.setContent(item)
                binding.llCardHolder.addView(view)
            }
            if (showCallButton) {
                binding.btnCall.visible()
                binding.btnCall.setOnClickListener {
                    dismiss()
                    viewModel.triggerCall()
                }
            }
        }

        binding.btnClose.setOnClickListener {
            dismiss()
        }
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updatePatientHistoryCardView()
    }

    override fun onStart() {
        super.onStart()

        val width = if (checkIsTablet(requireContext())) {
            0.6
        } else {
            0.96
        }

        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * width).toInt(),
            (resources.displayMetrics.heightPixels * 0.85).toInt(),
        )
    }

    companion object {
        private const val HISTORY_DATA = "history_data"
        private const val SHOW_CALL_BUTTON = "call_button"

        fun newInstance(
            history: List<PatientHistoryData>,
            showCallButton: Boolean = false,
        ): PatientDetailHistoryDialogFragment =
            PatientDetailHistoryDialogFragment().apply {
                arguments = bundleOf(
                    HISTORY_DATA to history,
                    SHOW_CALL_BUTTON to showCallButton,
                )
            }
    }
}
