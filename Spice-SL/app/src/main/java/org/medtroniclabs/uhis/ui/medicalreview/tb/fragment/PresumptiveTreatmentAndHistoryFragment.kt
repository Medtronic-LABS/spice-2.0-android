package org.medtroniclabs.uhis.ui.medicalreview.tb.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.setVisible
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.model.MotherNeonateAncRequest
import org.medtroniclabs.uhis.data.model.TbHistory
import org.medtroniclabs.uhis.databinding.FragmentPresumptiveTreatmentAndHistoryBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.medicalreview.tb.activity.TBMedicalReviewActivity
import org.medtroniclabs.uhis.ui.medicalreview.tb.viewmodel.TbPatientHistoryAndPresumptiveViewModel
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel

@AndroidEntryPoint
class PresumptiveTreatmentAndHistoryFragment : BaseFragment(), View.OnClickListener {
    private val patientDetailsViewModel: PatientDetailViewModel by activityViewModels()
    private lateinit var binding: FragmentPresumptiveTreatmentAndHistoryBinding
    private val viewModel: TbPatientHistoryAndPresumptiveViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentPresumptiveTreatmentAndHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    companion object {
        const val TAG = "PresumptiveTreatmentAndHistoryFragment"

        fun newInstance() = PresumptiveTreatmentAndHistoryFragment()
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        initListeners()
        attachObserver()
    }

    private fun initListeners() {
        binding.tvUpdateInv.safeClickListener(this)
    }

    private fun attachObserver() {
        viewModel.getHistory.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                }

                ResourceState.SUCCESS -> {
                    resourceState.data?.let { history ->
                        if (patientDetailsViewModel.getTbMedicalReviewStatus()) {
                            updateUiWithMedicalReview(history)
                        } else {
                            updateUiWithTbSummary(history)
                        }
                    }
                }

                ResourceState.ERROR -> {
                }
            }
        }
    }

    private fun updateUiWithMedicalReview(history: TbHistory) {
        with(binding) {
            tvPresentingText.text = CommonUtils.combineText(
                history.presentingComplaints.orEmpty(),
                history.presentingComplaintsNotes.orEmpty(),
                getString(R.string.hyphen_symbol),
            )
            tvComorbiditiesText.text = CommonUtils.combineText(
                history.comorbidities.orEmpty(),
                history.comorbiditiesNotes.orEmpty(),
                getString(R.string.hyphen_symbol),
            )
            tvSystemicText.text = CommonUtils.combineText(
                history.systemicExaminations
                    ?.map {
                        if (it.name.equals(
                                MedicalReviewDefinedParams.respiratory,
                                ignoreCase = true,
                            ) &&
                            !it.value.isNullOrBlank()
                        ) {
                            "${it.name} : ${it.value}"
                        } else {
                            it.name
                        }
                    }.orEmpty(),
                history.systemicExaminationNotes.orEmpty(),
                getString(R.string.hyphen_symbol),
            )
            tvClinicalNotesText.text = history.clinicalNotes?.takeIf { it.isNotBlank() }
                ?: getString(R.string.hyphen_symbol)
            tvPrescriptionsText.text =
                history.prescriptions
                    ?.let { CommonUtils.createPrescription(it, requireContext()) }
                    ?.takeIf { it.isNotEmpty() }
                    ?: requireContext().getString(R.string.hyphen_symbol)
            tvInvestigationsText.text = history.investigations
                ?.let {
                    CommonUtils.createInvestigation(
                        it,
                        requireContext(),
                    )
                }?.takeIf { it.isNotEmpty() }
                ?: requireContext().getString(R.string.hyphen_symbol)
            if (history.tbInvestigationStatus.equals(AssessmentDefinedParams.NA, true)) {
                binding.tvUpdateInv.visible()
                binding.tvUpdateInv.text = getString(R.string.add_inv_result)
            } else if (history.tbInvestigationStatus.equals(DefinedParams.NO, true)) {
                binding.tvUpdateInv.visible()
                binding.tvUpdateInv.text = getString(R.string.update_inv_result)
            } else {
                binding.tvUpdateInv.gone()
            }
        }
    }

    private fun updateUiWithTbSummary(history: TbHistory) {
        val builder = StringBuilder()

        history.tbSummary?.tbScreening?.let { summary ->
            if (summary.hasCough == true) {
                if (summary.hasCoughLastedLonger == true) {
                    builder.append(getString(R.string.cough_2weeks))
                }
                if (summary.hasNightSweats == true) {
                    appendIfNotEmpty(builder, ", ", getString(R.string.drenching_night_sweats))
                }
                if (summary.hasFever == true) {
                    appendIfNotEmpty(builder, ", ", getString(R.string.fever))
                }
                if (summary.hasWeightLoss == true) {
                    appendIfNotEmpty(builder, ", ", getString(R.string.weight_loss))
                }
            }
        }

        val presentingText = if (builder.isEmpty()) getString(R.string.hyphen_symbol) else builder.toString()
        binding.tvPresentingText.text = presentingText
    }

    private fun appendIfNotEmpty(
        builder: StringBuilder,
        delimiter: String,
        text: String,
    ) {
        if (builder.isNotEmpty()) builder.append(delimiter)
        builder.append(text)
    }

    fun initView() {
        binding.apply {
            val isTbReview = patientDetailsViewModel.getTbMedicalReviewStatus()
            tvPresentingLabel.text =
                getString(if (isTbReview) R.string.presenting_complaints else R.string.tb_sign_symptoms)
            tvTitle.text =
                getString(if (isTbReview) R.string.presumptive_treatment_summary else R.string.patient_history)
            groupCMRTb.setVisible(isTbReview)
            if (isTbReview) {
                viewModel.fetchPatientHistory(
                    MotherNeonateAncRequest(
                        patientReference = patientDetailsViewModel.getPatientFHIRId(),
                        tbIMRCompleted = true,
                    ),
                )
            } else {
                viewModel.fetchPatientHistory(
                    MotherNeonateAncRequest(
                        patientReference = patientDetailsViewModel.getPatientFHIRId(),
                        tbIMRCompleted = false,
                    ),
                )
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.tvUpdateInv -> {
                (activity as TBMedicalReviewActivity).openInvestigationActivity()
            }
        }
    }
}
