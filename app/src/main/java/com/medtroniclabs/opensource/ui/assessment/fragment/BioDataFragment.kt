package com.medtroniclabs.opensource.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.calculateEstimatedDeliveryDate
import com.medtroniclabs.opensource.common.DateUtils.calculateGestationalAge
import com.medtroniclabs.opensource.databinding.FragmentBioDataBinding
import com.medtroniclabs.opensource.db.entity.MemberClinicalEntity
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.assessment.AssessmentCommonUtils
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import java.text.DecimalFormat


class BioDataFragment : BaseFragment() {

    private lateinit var binding: FragmentBioDataBinding

    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG = "BioDataFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentBioDataBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        attachObserver()
    }

    private fun attachObserver() {

        viewModel.memberDetailsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    viewModel.getPregnancyDetailInformation()
                    showPatientBioData(resourceState.data)
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }

        viewModel.memberClinicalLiveData.observe(viewLifecycleOwner) { entity ->
            showPatientOtherInformation(entity)
        }

    }

    private fun showPatientOtherInformation(entity: MemberClinicalEntity?) {
        val title: String
        val visitCount = getVisitCount(entity?.visitCount)
        when (viewModel.workflowName) {
            RMNCH.ANC -> {
                title = getString(R.string.anc_visit)
                showAncRelatedInformation(entity)
            }

            RMNCH.PNC -> {
                title = getString(R.string.pnc_visit)
                showPncRelatedInformation(entity, visitCount)
            }

            RMNCH.ChildHoodVisit -> {
                title = getString(R.string.child_hood_visit)
            }

            else -> {
                title = getString(R.string.hyphen_symbol)
            }
        }

        binding.llPatientInfo.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title = title,
                value = visitCount.toString(),
                null,
                binding.root.context
            )
        )
    }

    private fun showPncRelatedInformation(entity: MemberClinicalEntity?, visitCount: Long) {
        if (entity != null) {
            entity.apply {
                if (visitCount == 1L) {
                    binding.llPatientInfo.addView(
                        AssessmentCommonUtils.addViewSummaryLayout(
                            title = getString(R.string.delivery_at),
                            value = getString(R.string.home_title),
                            context = binding.llPatientInfo.context
                        )
                    )
                } else {
                    isDeliveryAtHome?.let {
                        val value =
                            if (it) getString(R.string.home_title) else getString(R.string.phu)
                        binding.llPatientInfo.addView(
                            AssessmentCommonUtils.addViewSummaryLayout(
                                title = getString(R.string.delivery_at),
                                value = value,
                                context = binding.llPatientInfo.context
                            )
                        )
                    }
                }

                clinicalDate?.let {
                    binding.llPatientInfo.addView(
                        AssessmentCommonUtils.addViewSummaryLayout(
                            title = getString(R.string.date_of_delivery),
                            value = DateUtils.convertDateFormat(
                                clinicalDate,
                                DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                                DATE_ddMMyyyy
                            ),
                            context = binding.llPatientInfo.context
                        )
                    )
                }
                if (numberOfNeonate != null) {
                    binding.llPatientInfo.addView(
                        AssessmentCommonUtils.addViewSummaryLayout(
                            title = getString(R.string.no_of_neonates),
                            value = DecimalFormat("##.#").format(numberOfNeonate),
                            context = binding.llPatientInfo.context
                        )
                    )
                }
            }
        } else {
            if (visitCount == 1L) {
                binding.llPatientInfo.addView(
                    AssessmentCommonUtils.addViewSummaryLayout(
                        title = getString(R.string.delivery_at),
                        value = getString(R.string.home_title),
                        context = binding.llPatientInfo.context
                    )
                )
            }
        }
    }

    private fun showAncRelatedInformation(entity: MemberClinicalEntity?) {
        entity?.apply {
            if (!clinicalDate.isNullOrEmpty()) {
                binding.llPatientInfo.addView(
                    AssessmentCommonUtils.addViewSummaryLayout(
                        title = getString(R.string.last_menstrual_period),
                        value = DateUtils.convertDateFormat(
                            clinicalDate,
                            DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                            DATE_ddMMyyyy
                        ),
                        context = binding.llPatientInfo.context
                    )
                )
                val lastMenstrualDate = DateUtils.getLastMenstrualDate(clinicalDate)
                val gestationWeek = calculateGestationalAge(lastMenstrualDate).first
                createSummary(
                    getString(R.string.gestational_age),
                    "$gestationWeek ${getWeekPeriod(gestationWeek)}"
                )
                val estimatedDeliveryDate = calculateEstimatedDeliveryDate(lastMenstrualDate)
                val formattedEstimatedDeliveryDate =
                    DateUtils.getDateFormat().format(estimatedDeliveryDate.time)
                createSummary(
                    getString(R.string.estimated_delivery_date),
                    formattedEstimatedDeliveryDate
                )

            }
        }
    }

    private fun getWeekPeriod(gestationWeek: Long): String {
        return if (gestationWeek == 1L){
            requireContext().getString(R.string.week)
        } else{
            requireContext().getString(R.string.weeks)
        }
    }

    private fun createSummary(title: String, value: String) {
        binding.llPatientInfo.addView(
            AssessmentCommonUtils.addViewSummaryLayout(
                title = title,
                value = value,
                context = binding.llPatientInfo.context
            )
        )
    }

    private fun getVisitCount(visitCount: Long?): Long {
        return if (visitCount == null) {
            1L
        } else {
            (visitCount + 1)
        }
    }

    private fun initView() {
        viewModel.getMemberDetailsById()
    }

    private fun showPatientBioData(data: AssessmentMemberDetails?) {
        data?.apply {
            binding.patientName.tvKey.text = getString(R.string.name)
            binding.patientName.tvValue.text = name.capitalizeFirstChar()
            binding.patientId.tvKey.text = getString(R.string.patient_id)
            binding.patientId.tvValue.text = patientId ?: getString(R.string.separator_double_hyphen)
            binding.gender.tvKey.text = getString(R.string.gender)
            binding.gender.tvValue.text = gender.capitalizeFirstChar()
            binding.dobAge.tvKey.text = getString(R.string.age)
            binding.dobAge.tvValue.text = getAgeValue(
                CommonUtils.getAgeFromDOB(
                    dateOfBirth,
                    requireContext()
                )
            )
            viewModel.workflowName?.let { workFlowName ->
                viewModel.getPatientVisitCountByType(workFlowName, id)
            }
        }
    }

    private fun getAgeValue(ageFromDob: String): String {
        return if (!ageFromDob.contains(" ") && ageFromDob.isNotEmpty())
            requireContext().getString(
                R.string.firstname_lastname,
                ageFromDob,
                getString(R.string.years)
            )
        else if (ageFromDob.isEmpty()) {
            requireContext().getString(R.string.seperator_hyphen)
        } else
            ageFromDob
    }

}
