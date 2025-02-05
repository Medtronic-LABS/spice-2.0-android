package com.medtroniclabs.opensource.ui.mypatients.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.combineText
import com.medtroniclabs.opensource.common.CommonUtils.getContactNumber
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_ddMMMyyyy
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import com.medtroniclabs.opensource.common.DateUtils.DATE_ddMMyyyy
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.IsReferredScreen
import com.medtroniclabs.opensource.common.DefinedParams.OtherNotes
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentPatientInfoBinding
import com.medtroniclabs.opensource.formgeneration.extension.capitalizeFirstChar
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.data.NCDPregnancyRiskUpdate
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMedicalReviewActivity
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDMentalHealthQuestionDialog
import com.medtroniclabs.opensource.ncd.medicalreview.dialog.NCDPatientHistoryDialog
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.BaseFragment
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.ANC
import com.medtroniclabs.opensource.ui.assessment.rmnch.RMNCH.PNC
import com.medtroniclabs.opensource.ui.common.GeneralInfoDialog
import com.medtroniclabs.opensource.ui.dialog.GeneralSuccessDialog
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.AncVisitCallBack
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class PatientInfoFragment : BaseFragment() {

    private lateinit var binding: FragmentPatientInfoBinding
    val viewModel: PatientDetailViewModel by activityViewModels()
    private var dataCallback: AncVisitCallBack? = null
    fun setDataCallback(callback: AncVisitCallBack) {
        dataCallback = callback
    }


    companion object {
        const val TAG = "PatientInfoFragment"

        fun newInstance(): PatientInfoFragment {
            return PatientInfoFragment()
        }

        fun newInstance(
            patientId: String?,
            isAnc: Boolean = false,
            isPnc:Boolean =false,
            isReferredScreen: Boolean = false
        ): PatientInfoFragment {
            val fragment = PatientInfoFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putBoolean(ANC, isAnc)
            bundle.putBoolean(PNC,isPnc)
            bundle.putBoolean(IsReferredScreen, isReferredScreen)
            fragment.arguments = bundle
            return fragment
        }

        fun newInstanceForNCD(
            patientId: String?,
            origin: String
        ): PatientInfoFragment {
            val fragment = PatientInfoFragment()
            val bundle = Bundle()
            bundle.putString(DefinedParams.PatientId, patientId)
            bundle.putString(DefinedParams.ORIGIN, origin)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentPatientInfoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getMenuForClinicalWorkflows()
        attachObservers()
    }

    fun  initView() {
        val patientId = arguments?.getString(DefinedParams.PatientId, "")
        if (patientId?.isNotBlank() == true) {
            if (CommonUtils.isCommunity()) {
                viewModel.getPatients(patientId, if (isAnc() == true) ANC.uppercase() else null)
            } else {
                val origin = arguments?.getString(DefinedParams.ORIGIN, "")
                viewModel.getPatients(
                    patientId,
                    origin = origin
                )
            }
        }
    }

    private fun attachObservers() {
        viewModel.clinicalWorkflowsMenusLiveData.observe(viewLifecycleOwner) { data ->
            initView()
        }
        viewModel.patientDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    resource.data?.let {
                        if (CommonUtils.isCommunity()) {
                            setDataInInfo(it)
                        } else {
                            setNCDData(it)
                        }
                        dataCallback?.onDataLoaded(it)
                    }
                }

                ResourceState.ERROR -> {
//                    hideProgress()
                }
            }
        }

        viewModel.ncdInstructionModelResponse.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    resourceState.data?.instructions?.let { instructions ->
                        val title = instructions.removeAt(0)
                        GeneralInfoDialog.newInstance(
                            title,
                            null,
                            instructions
                        ).show(childFragmentManager, GeneralInfoDialog.TAG)
                    }
                }

                else -> {
                    //Nothing to invoke
                }
            }
        }

        viewModel.updatePregnancyRisk.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    initView()
                }
            }
        }
    }

    private fun isAnc(): Boolean? {
        return arguments?.getBoolean(ANC, false)
    }
    private fun isPnc(): Boolean? {
        return arguments?.getBoolean(PNC, false)
    }

    private fun isReferredScreen(): Boolean? {
        return arguments?.getBoolean(IsReferredScreen, false)
    }
    private fun setDataInInfo(patientListRespModel: PatientListRespModel) {
        showProgress()
        viewModel.patientDetailsId = patientListRespModel.id
        viewModel.childPatientDetails=patientListRespModel.pregnancyDetails?.neonatePatientId
        viewModel.dateOfDelivery=patientListRespModel.pregnancyDetails?.dateOfDelivery
        viewModel.neonateOutCome=patientListRespModel.pregnancyDetails?.neonatalOutcomes
        val isAnc = isAnc()
        val isPnc= isPnc()
        val name =
            patientListRespModel.name ?: requireContext().getString(R.string.separator_hyphen)
        val gender =
            patientListRespModel.gender ?: requireContext().getString(R.string.separator_hyphen)
        val age = patientListRespModel.birthDate?.let {
            DateUtils.getAgeDescription(patientListRespModel.birthDate, requireContext())
        } ?: (patientListRespModel.age ?: requireContext().getString(R.string.separator_hyphen))
        setTitle(requireContext().getString(R.string.household_summary_member_info, name.trim(), age, CommonUtils.translatedGender(requireContext(), gender)))
        with(binding) {
            val lastMenstrualDate =
                patientListRespModel.pregnancyDetails?.lastMenstrualPeriod.takeIf { it?.isNotBlank() == true }?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)
                }
            val dateOfDelivery =
                patientListRespModel.pregnancyDetails?.dateOfDelivery.takeIf { it?.isNotBlank() == true }?.let {
                    DateUtils.convertDateFormat(it, DATE_FORMAT_yyyyMMddHHmmssZZZZZ, DATE_ddMMyyyy)?:requireContext().getString(R.string.hyphen_symbol)
                }

            val dataList = mutableListOf(
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.patient_id),
                    DefinedParams.Value to (patientListRespModel.patientId
                        ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim()
                ),
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.contact_number),
                    DefinedParams.Value to (getContactNumber(patientListRespModel.phoneNumber.takeIf { it?.isNotBlank() == true }
                        ?.trim())
                        ?: requireContext().getString(R.string.hyphen_symbol))
                ),
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.hh_id),
                    DefinedParams.Value to (patientListRespModel.houseHoldNumber
                        ?: requireContext().getString(R.string.hyphen_symbol)).toString()
                ),
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.landmark),
                    DefinedParams.Value to (patientListRespModel.landmark.takeIf { it?.isNotBlank() == true }?.trim()
                        ?: requireContext().getString(R.string.hyphen_symbol))),
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.village),
                    DefinedParams.Value to (patientListRespModel.village.takeIf { it?.isNotBlank() == true }?.trim()
                        ?: requireContext().getString(R.string.hyphen_symbol)))
            )
            if (isAnc == true && lastMenstrualDate != null) {
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.last_menstrual_period),
                        DefinedParams.Value to lastMenstrualDate
                    )
                )
            }
            if (isPnc==true&&dateOfDelivery != null){
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.date_of_delivery),
                        DefinedParams.Value to dateOfDelivery
                    )
                )
            }
            if (isAnc == true && !(viewModel.isSummary)) {
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.anc_visit),
                        DefinedParams.Value to (patientListRespModel.pregnancyDetails?.ancVisitMedicalReview?.takeIf { true }
                            ?.plus(1)
                            ?.toString()
                            ?: "1")
                    )
                )
            }
            if (isReferredScreen() == true) {
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.diagnosis),
                        DefinedParams.Value to combineText(
                            patientListRespModel.diagnosis?.filter { it.diseaseCategory?.lowercase() != OtherNotes.lowercase() }
                                ?.map { it.diseaseCategory }?.distinct(),
                            "",
                            getString(R.string.hyphen_symbol)
                        )
                    )
                )
            }
            commonAdapter(dataList as MutableList<Map<String, Any>>)
        }
    }

    private fun commonAdapter(dataList: MutableList<Map<String, Any>>) {
        val adapter =
            PatientInfoAdapter(
                dataList,
                R.color.fragment_bg,
                (requireActivity() as BaseActivity),
                mentalHealthAssessment = { mhPair ->
                    mhPair.first?.let { type ->
                        if (type.isNotBlank()) {
                            viewModel.patientDetailsLiveData.value?.data?.let {
                                withNetworkAvailability(online = {
                                    showMentalHealthDialog(
                                        CommonUtils.getAssessmentType(
                                            requireContext(),
                                            type
                                        ), mhPair.second
                                    )
                                })
                            }
                        }
                    }
                },
                onItemPregnantDialog = {
                    withNetworkAvailability(online = {
                        viewModel.ncdGetInstructions()
                    })
                }, onItemToggle = {
                    withNetworkAvailability(online = {
                        val request = NCDPregnancyRiskUpdate(
                            memberReference = viewModel.getPatientFHIRId(),
                            patientReference = viewModel.getPatientId(),
                            isPregnancyRisk = it,
                            provenance = ProvanceDto()
                        )
                        viewModel.ncdUpdatePregnancyRisk(request)
                    })
                })
        val isLandscape =
            resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

        val spanCount = if (isLandscape) 2 else 1
        if (CommonUtils.checkIsTablet(requireContext())) {
            binding.rvPatientInfo.layoutManager = GridLayoutManager(requireContext(), spanCount)
        } else {
            binding.rvPatientInfo.layoutManager = GridLayoutManager(requireContext(), 1)
        }
        binding.rvPatientInfo.adapter = adapter
        hideProgress()
    }

    private fun showMentalHealthDialog(type: String, isEditAssessment: Boolean) {
        val dialog = childFragmentManager.findFragmentByTag(NCDMentalHealthQuestionDialog.TAG)
        if (dialog == null) {
            NCDMentalHealthQuestionDialog.newInstance(
                type,
                viewModel.getPatientId(),
                viewModel.getPatientFHIRId(),
                isEditAssessment
            ) { response, errorResponse ->
                if (errorResponse.isNullOrBlank() && response != null) {
                    val fragment = childFragmentManager.findFragmentByTag(GeneralSuccessDialog.TAG)
                    if (fragment == null) {
                        GeneralSuccessDialog.newInstance(
                            title = response.first,
                            message = response.second,
                            okayButton = getString(R.string.done)
                        ) {
                            (requireActivity() as? NCDMedicalReviewActivity)?.swipeRefresh()
                        }.show(childFragmentManager, GeneralSuccessDialog.TAG)
                    }
                } else {
                    (activity as BaseActivity?)?.showErrorDialogue(
                        title = getString(R.string.error),
                        message = errorResponse
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok)
                    ) {}
                }
            }
                .show(childFragmentManager, NCDPatientHistoryDialog.TAG)
        }
    }

    private fun setNCDData(data: PatientListRespModel) {
        showProgress()
        data.name?.let { name ->
            setTitle(
                setTitleBasedOnRole(data, name)
            )
        }
        val cvdRiskLevel = data.cvdRiskScore?.toLong()?.takeIf { it > 0 }?.let {
            Pair(
                StringConverter.appendTexts(
                    "${it}%",
                    data.cvdRiskLevel, separator = "-"
                ),
                CommonUtils.cvdRiskColorCode(it, requireContext())
            )
        }

        val bmiPair = CommonUtils.getBMIFormattedText(
            requireContext(),
            data.bmi?.takeIf { it > 0.0 }
        )
        val dataList = mutableListOf(
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.registration_date),
                DefinedParams.Value to (data.enrollmentAt?.let {
                    DateUtils.convertDateFormat(
                        it,
                        DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
                        DATE_FORMAT_ddMMMyyyy
                    ).takeIf { it.isNotBlank() } ?: requireContext().getString(R.string.hyphen_symbol)
                } ?: requireContext().getString(R.string.pending_registration)).toString().trim()
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.cvd_risk),
                DefinedParams.Value to (cvdRiskLevel?.first
                    ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                DefinedParams.color to cvdRiskLevel?.second
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.patient_id),
                DefinedParams.Value to (data.programId
                    ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim()
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.bmi),
                DefinedParams.Value to (bmiPair.first
                    ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim(),
                DefinedParams.color to bmiPair.second
            ),
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.national_id),
                DefinedParams.Value to (data.identityValue
                    ?: requireContext().getString(R.string.hyphen_symbol)).toString().trim()
            )
        )
        dataList.add(
            mapOf(
                DefinedParams.label to requireContext().getString(R.string.contact_number),
                DefinedParams.Value to (getContactNumber(data.phoneNumber.takeIf { it?.isNotBlank() == true }
                    ?.trim())
                    ?: requireContext().getString(R.string.hyphen_symbol))
            )
        )
        val isMentalHealth = viewModel.mrMenuId.equals("mentalHealth", ignoreCase = true)
        if (isMentalHealth) {
            val phq4Score = data.phq4score ?: requireContext().getString(R.string.hyphen_symbol)
            val phq4AssessmentType = if (data.phq4score != null) requireContext().getString(R.string.edit_assessment) else requireContext().getString(
                R.string.start_assessment
            )
            dataList.add(
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.phq4_score),
                    DefinedParams.Value to phq4Score,
                    Screening.type to phq4AssessmentType,
                    DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color)
                )
            )
            val showSuicidcalIdeation =
                viewModel.clinicalWorkflowsMenusLiveData.value?.firstOrNull {
                    it.workflowName.equals(
                        Screening.suicideScreener,
                        true
                    )
                } != null
            if (showSuicidcalIdeation) {
                val suicidcalIdeation = data.suicidalIdeation ?: requireContext().getString(
                    R.string.hyphen_symbol
                )
                val type =
                    if (data.suicidalIdeation != null) requireContext().getString(R.string.edit_assessment) else requireContext().getString(
                        R.string.start_assessment
                    )
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.suicidal_ideation),
                        DefinedParams.Value to suicidcalIdeation.capitalizeFirstChar(),
                        Screening.type to type,
                        DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color)
                    )
                )
            }
            val showCageAid =
                viewModel.clinicalWorkflowsMenusLiveData.value?.firstOrNull {
                    it.workflowName.equals(
                        Screening.substanceAbuse,
                        true
                    )
                } != null
            if (showCageAid) {
                val cageAid = data.cageAid?.toDoubleOrNull()?.toInt()?.toString()
                    ?: requireContext().getString(R.string.hyphen_symbol)
                val assessmentType =
                    if (data.cageAid != null) requireContext().getString(R.string.edit_assessment) else requireContext().getString(
                        R.string.start_assessment
                    )
                dataList.add(
                    mapOf(
                        DefinedParams.label to requireContext().getString(R.string.cage_aid),
                        DefinedParams.Value to cageAid,
                        Screening.type to assessmentType,
                        DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color)
                    )
                )
            }
        } else {
            data.cageAid?.let { aid ->
                if ((aid.toDoubleOrNull()?.toInt() ?: 0) > 0) {
                    dataList.add(
                        mapOf(
                            DefinedParams.label to requireContext().getString(R.string.cage_aid),
                            DefinedParams.Value to aid.toDoubleOrNull()?.toInt()?.toString(),
                            DefinedParams.color to requireContext().getColor(R.color.medium_high_risk_color)
                        )
                    )
                }
            }
        }

        val isPregnancyANC =
            viewModel.mrMenuId.equals(DefinedParams.PregnancyANC, ignoreCase = true)
        if (isPregnancyANC && CommonUtils.canShowToggle(viewModel.getGender(), data.pregnancyDetails?.isPregnancyRisk)) {
            dataList.add(
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.high_risk),
                    DefinedParams.Value to (data.pregnancyDetails?.isPregnancyRisk ?: false),
                    DefinedParams.Gender to (data.gender)
                )
            )
        }
        if (viewModel.isCmr) {
            dataList.add(
                mapOf(
                    DefinedParams.label to requireContext().getString(R.string.diagnosis),
                    DefinedParams.Value to combineText(
                        data.confirmDiagnosis?.diagnosis?.mapNotNull { it.name },
                        data.confirmDiagnosis?.diagnosisNotes.takeIf { it?.isNotBlank() == true },
                        getString(R.string.hyphen_symbol)
                    )
                )
            )
        }
        (activity as BaseActivity).setRedRiskPatient(data.isRedRiskPatient)
        commonAdapter(dataList as MutableList<Map<String, Any>>)
    }

    private fun setTitleBasedOnRole(response: PatientListRespModel, text: String): String {
        return StringConverter.appendTexts(
            firstText = text,
            response.age.toString(),
            CommonUtils.translatedGender(requireContext(), response.gender),
            separator = getString(R.string.hyphen_symbol)
        ).capitalizeFirstChar()
    }
}