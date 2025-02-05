package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import android.widget.RadioButton
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.databinding.FragmentSortingDialogBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientListViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SortDialogFragment : DialogFragment(), View.OnClickListener {
    private lateinit var binding: FragmentSortingDialogBinding
    private val patientListViewModel: PatientListViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    companion object {
        const val TAG = "SortingDialogFragment"
        fun newInstance(): SortDialogFragment {
            return SortDialogFragment()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
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
            WindowManager.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        prefillValues()
        setListeners()
    }

    private fun prefillValues() {
        patientListViewModel.let {
            binding.apply {
                rbRedRisk.isChecked = it.isRedRisk != null
                rbLatestAssessment.isChecked = it.isLatestAssessment != null
                rbMedicalReview.isChecked = it.isMedicalReviewDueDate != null
                rbBP.isChecked = it.isHighLowBp != null
                rbBG.isChecked = it.isHighLowBg != null
                rbAssessmentDueDate.isChecked = it.isAssessmentDueDate != null
            }
            handleResetButtons()
        }
    }

    private fun setListeners() {
        binding.rgSortCondition.setOnCheckedChangeListener { _, checkedId ->
            if (connectivityManager.isNetworkAvailable()) {
                val radioButton =
                    if (checkedId > 0) binding.rgSortCondition.findViewById<RadioButton>(
                        checkedId
                    ) else null
                if (radioButton != null && radioButton.isChecked) {
                    patientListViewModel.apply {
                        isRedRisk = value(checkedId, binding.rbRedRisk.id)
                        isLatestAssessment = value(
                            checkedId,
                            binding.rbLatestAssessment.id
                        )
                        isMedicalReviewDueDate = value(
                            checkedId,
                            binding.rbMedicalReview.id
                        )
                        isHighLowBp = value(checkedId, binding.rbBP.id)
                        isHighLowBg = value(checkedId, binding.rbBG.id)
                        isAssessmentDueDate = value(
                            checkedId,
                            binding.rbAssessmentDueDate.id
                        )
                    }
                    handleResetButtons()
                    patientListViewModel.setSort(true)
                    dismiss()
                }
            } else {
                dismiss()
                (activity as BaseActivity).showErrorDialogue(
                    getString(R.string.error),
                    getString(R.string.no_internet_error), false
                ) {}
            }
        }
        binding.btnReset.safeClickListener(this)
        binding.btnDone.safeClickListener(this)
        binding.labelHeader.ivClose.safeClickListener(this)
    }

    private fun value(checkedId: Int, id: Int): Boolean? {
        return if (checkedId == id) true else null
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnDone.id -> {
                patientListViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDPatientSort,
                    isCompleted = true
                )
                patientListViewModel.setSort(true)
                dismiss()
            }

            binding.labelHeader.ivClose.id -> dismiss()
            binding.btnReset.id -> doReset()
        }
    }

    private fun doReset() {
        binding.apply {
            patientListViewModel.apply {
                isRedRisk = null
                isLatestAssessment = null
                isMedicalReviewDueDate = null
                isHighLowBp = null
                isHighLowBg = null
                isAssessmentDueDate = null
            }

            rgSortCondition.clearCheck()
            btnDone.isEnabled = true
        }
    }

    private fun handleResetButtons() {
        binding.btnReset.isEnabled = binding.rgSortCondition.checkedRadioButtonId != -1
    }
}