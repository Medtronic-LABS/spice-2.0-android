package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.CommonUtils.createInvestigation
import com.medtroniclabs.opensource.common.CommonUtils.createPrescription
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams.FhirId
import com.medtroniclabs.opensource.common.DefinedParams.PatientId
import com.medtroniclabs.opensource.data.MotherNeonateAncSummaryModel
import com.medtroniclabs.opensource.databinding.FragmentMotherNeonateAncHistoryBinding
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.calculateBp
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertBeatsPerMinute
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertCMS
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertNullableDoubleToString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertNullableStringToString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel.MedicalReviewAncHistoryViewModel

class MotherNeonateAncHistoryFragment : BaseFragment() {
    private lateinit var binding: FragmentMotherNeonateAncHistoryBinding
    private val viewModel: MedicalReviewAncHistoryViewModel by activityViewModels()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = FragmentMotherNeonateAncHistoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObservers()
    }

    private fun attachObservers() {
        viewModel.motherNeonateAncSummary.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let {
                        autoPopulate(it)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    private fun autoPopulate(motherNeonateSummaryModel: MotherNeonateAncSummaryModel) {
        motherNeonateSummaryModel.let {
            with(binding) {
                tvDateOfReviewValue.text = it.dateOfReview?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DateUtils.DATE_FORMAT_yyyyMMdd,
                        DateUtils.DATE_ddMMyyyy,
                    )
                }.let {
                    convertNullableStringToString(
                        it,
                        requireContext()
                    )
                }
                tvAncVisitLabelValue.text = it.visitNumber ?: getString(R.string.hyphen_symbol)
                tvPrescriptionValue.text = createPrescription(it.prescriptions, requireContext())
                    ?: getString(R.string.hyphen_symbol)
                tvInvestigationValue.text =  createInvestigation(it.investigations,requireContext())
                    ?: requireContext().getString(R.string.hyphen_symbol)
                tvBpValue.text = if (it.systolic == null && it.diastolic == null) {
                    getString(R.string.hyphen_symbol)
                } else {
                    calculateBp(it.systolic, it.diastolic, requireContext())
                }
                tvAncVisitLabelValue.text =
                    convertNullableStringToString(it.visitNumber, requireContext())
                tvBmiValue.text = convertNullableDoubleToString(it.bmi, requireContext())
                // tvWeightValue.text = convertNullableDoubleToString(it.weight, requireContext())
                if (it.weight != null && it.weight != 0.0) {
                    tvWeightValue.text = "${CommonUtils.getDecimalFormatted(
                        it.weight
                    )} ${getString(R.string.kg)}"
                } else {
                    tvWeightValue.text = getString(R.string.hyphen_symbol)
                }
                tvFetalHeartRateValue.text =
                    convertBeatsPerMinute(it.fetalHeartRate, requireContext())
                tvFundalHeightValue.text =
                    convertCMS(it.fundalHeight, requireContext())
                tvClinicalNotesValue.setExpandableText(
                    convertNullableStringToString(
                        it.clinicalNotes,
                        requireContext()
                    ),
                    title = tvClinicalNotesLabel.text.toString(),
                    maxLength = 70,
                    activity = (requireActivity() as BaseActivity)
                )
                tvPresentingComplaintsValue.setExpandableText(
                    combineText(
                        it.presentingComplaints, it.presentingComplaintsNotes, getString(
                            R.string.hyphen_symbol
                        )
                    ),
                    title = tvPresentingComplaintsLabel.text.toString(),
                    maxLength = 35,
                    activity = (requireActivity() as BaseActivity)
                )
                tvObstetricsExaminationValue.setExpandableText(
                    combineText(
                        it.obstetricExaminations, it.obstetricExaminationNotes, getString(
                            R.string.hyphen_symbol
                        )
                    ),
                    title = tvObstetricsExaminationLabel.text.toString(),
                    maxLength = 35,
                    activity = (requireActivity() as BaseActivity)
                )
            }
        }
    }

    private fun initView() {
        viewModel.getMedicalReviewAncHistory(
            arguments?.getString(PatientId),
            arguments?.getString(FhirId)
        )
    }

    companion object {
        const val TAG = "MedicalReviewAncHistoryFragment"
        fun newInstance(): MotherNeonateAncHistoryFragment {
            return MotherNeonateAncHistoryFragment()
        }

        fun newInstance(patientId: String?, fhirId: String?): MotherNeonateAncHistoryFragment {
            val fragment = MotherNeonateAncHistoryFragment()
            fragment.arguments = Bundle().apply {
                putString(PatientId, patientId)
                putString(FhirId, fhirId)
            }
            return fragment
        }
    }
}
