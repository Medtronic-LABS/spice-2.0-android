package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.setExpandableText
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.databinding.FragmentPregnancySummaryBinding
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertNullableDoubleToString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertNullableIntToString
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.MotherNeonateUtil.convertNullableStringToString
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel

class PregnancySummaryFragment() : BaseFragment() {

    private lateinit var binding: FragmentPregnancySummaryBinding
    private var pregnancyDetailsModel: PregnancyDetailsModel? = null
    private var pregnancyHistoryChip: ArrayList<ChipViewItemModel>? = null
    private var pregnancyHistoryNotes: String? = null
    private val patientDetailsViewModel: PatientDetailViewModel by activityViewModels()

    fun setData(
        pregnancyHistoryChip: ArrayList<ChipViewItemModel>,
        pregnancyHistoryNotes: String?,
        pregnancyDetailsModel: PregnancyDetailsModel
    ) {
        this.pregnancyHistoryChip = pregnancyHistoryChip
        this.pregnancyHistoryNotes = pregnancyHistoryNotes
        this.pregnancyDetailsModel = pregnancyDetailsModel
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPregnancySummaryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeView()
        setDataInfo()
    }

    private fun initializeView() {
        binding.titlePregnancySummaryFragment.text = getString(R.string.title_pregnancy_summary)
    }

    private fun setDataInfo() {
        with(binding) {
            pregnancyDetailsModel?.let { pregnancyDetailsModel ->
                tvBmiValue.text =
                    convertNullableDoubleToString(pregnancyDetailsModel.bmi, requireContext())
                tvGravidaValue.text =
                    convertNullableIntToString(pregnancyDetailsModel.gravida, requireContext())
                tvLastMenstrualValue.text =
                    if (pregnancyDetailsModel.lastMenstrualPeriod != null || patientDetailsViewModel.getPatientLmb() != null) {
                        val lmpDate = pregnancyDetailsModel.lastMenstrualPeriod
                            ?: patientDetailsViewModel.getPatientLmb()
                        lmpDate?.let {
                            DateUtils.convertDateFormat(
                                it,
                                DateUtils.DATE_FORMAT_yyyyMMdd,
                                DateUtils.DATE_ddMMyyyy,
                            )
                        }
                    } else {
                        convertNullableStringToString(
                            null,
                            requireContext()
                        )
                    }
                tvParityValue.text =
                    convertNullableIntToString(pregnancyDetailsModel.parity, requireContext())
                tvEstimatedDeliveryDateValue.text =
                    pregnancyDetailsModel.estimatedDeliveryDate?.let {
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
                tvGestationalAgeValue.text =
                    pregnancyDetailsModel.gestationalAge?.let {
                        DateUtils.formatGestationalAge(
                            it,
                            requireContext()
                        )
                    } ?: getString(R.string.hyphen_symbol)
                tvPregnancyHistoryValue.setExpandableText(
                    combineText(
                        pregnancyHistoryChip?.map { it.name },
                        pregnancyHistoryNotes,
                        getString(R.string.hyphen_symbol)
                    ),
                    title = tvPregnancyHistoryLabel.text.toString(),
                    maxLength = 60,
                    activity = (requireActivity() as BaseActivity)
                )
                tvBloodGroupValue.text =
                    convertNullableStringToString(
                        pregnancyDetailsModel.patientBloodGroup,
                        requireContext()
                    )
                tvNoofFetusValue.text =
                    convertNullableIntToString(pregnancyDetailsModel.noOfFetus, requireContext())
            }
        }
    }

    companion object {
        const val TAG = "PregnancySummaryFragment"

        fun newInstance(): PregnancySummaryFragment {
            return PregnancySummaryFragment()
        }
    }

}