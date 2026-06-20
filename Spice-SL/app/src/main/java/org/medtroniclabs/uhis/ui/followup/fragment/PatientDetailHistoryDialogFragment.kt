package org.medtroniclabs.uhis.ui.followup.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.common.CommonUtils.checkIsTablet
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.databinding.DialogFragmentPatientHistoryBinding
import org.medtroniclabs.uhis.ui.followup.customview.PatientHistoryCardView

@AndroidEntryPoint
class PatientDetailHistoryDialogFragment(private val history: List<PatientHistoryData>) : DialogFragment() {
    private lateinit var binding: DialogFragmentPatientHistoryBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogFragmentPatientHistoryBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(R.color.transparent)
        // updatePatientHistoryCardView()
        return binding.root
    }

    private fun updatePatientHistoryCardView() {
        binding.llCardHolder.removeAllViews()
        history.forEach { item ->
            val view = PatientHistoryCardView(requireContext())
            view.setContent(item)
            binding.llCardHolder.addView(view)
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
}
