package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CVDRiskCalculator
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ANY_NEW_OR_WORSENING_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.EYE_CARE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_DIAGNOSED_BP
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_DIAGNOSED_GLUCOSE
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ID_NCD_SYMPTOMS_MEDICATION
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NAME
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NEW_WORSENING_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ncd
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rootSuffix
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import org.medtroniclabs.uhis.ui.common.GeneralInfoDialog

@AndroidEntryPoint
class BDNCDAssessmentFragment : BaseFragment(), FormEventListener {
    private lateinit var binding: FragmentAssessmentBinding

    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG: String = "BDNCDAssessmentFragment"

        fun newInstance(): BDNCDAssessmentFragment = BDNCDAssessmentFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        getFormDataForWorkflow()
        setListeners()
        attachObservers()
    }

    private fun getFormDataForWorkflow() {
        viewModel.getFormData(MenuConstants.NCD_MENU_ID)
        viewModel.getNearestHealthFacility()
    }

    private fun initView() {
        // viewModel.setUserJourney(AnalyticsDefinedParams.NCDASSESSMENT)
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = isTranslationEnabled,
        ) { map, id ->
            when (id) {
                Screening.Weight, Screening.Height -> {
                    viewModel.renderBMIValue(requireContext(), formGenerator, map)
                }
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    private fun setListeners() {
        binding.btnSubmit.setOnClickListener {
            formGenerator.formSubmitAction(binding.btnSubmit)
        }
    }

    private fun attachObservers() {
        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showProgress()
                }

                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        formGenerator.populateViews(data.formLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }
            }
        }
    }

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
    }

    override fun onPopulate(targetId: String) {
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        val inputData = EntityMapper.mapToSignsAndSymptomsEntity(formLayout.optionsList)
        CheckBoxDialog
            .newInstance(id, resultMap, inputData = inputData) { resultMap ->
                formGenerator.validateCheckboxDialogue(id, formLayout, resultMap)
                hideOrShowAnyNewWorseningSymptomView(resultMap)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
    }

    private fun hideOrShowAnyNewWorseningSymptomView(resultMap: ArrayList<HashMap<String, Any>>) {
        val shouldShowAnyNew = resultMap.any { map ->
            map[NAME] == ANY_NEW_OR_WORSENING_SYMPTOMS
        }

        formGenerator.getViewByTag(NEW_WORSENING_SYMPTOMS + rootSuffix)?.visibility =
            if (shouldShowAnyNew) View.VISIBLE else View.GONE
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        informationList?.let {
            GeneralInfoDialog
                .newInstance(
                    title,
                    description,
                    it,
                ).show(childFragmentManager, GeneralInfoDialog.TAG)
        }
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.let { details ->
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    ncd,
                )
            }

            viewModel.memberDetailsLiveData.value?.data?.let { memberDetail ->
                result?.second?.let { assessmentMap ->
                    lifecycleScope.launch {
                        val ncdMap = assessmentMap[ncd] as HashMap<String, Any>
                        val bpResult = AssessmentUtil.calculateAverageBloodPressure(ncdMap)
                        val bgResult = AssessmentUtil.addDateAndTimeForGlucose(ncdMap)
                        val symptomList = AssessmentUtil.getSymptomsList(ncdMap)
                        viewModel.isFollowupVisit = viewModel.getLastServiceHistory(MenuConstants.NCD_MENU_ID) != null

                        val referralResult =
                            ReferralResultGenerator().computeReferralResultForBDNCD(
                                ncdMap,
                                bpResult,
                                bgResult,
                                symptomList,
                                viewModel.isFollowupVisit,
                                useNcdRiskAlgorithm = viewModel.isFollowupVisit,
                            )

                        val riskModels = viewModel.loadRiskClassificationModels()
                        CVDRiskCalculator.calculateCVDRiskFactor(
                            ncdMap,
                            riskModels,
                            memberDetail.dateOfBirth,
                            memberDetail.gender,
                        )

                        viewModel.saveAssessment(serverData, assessmentMap, referralResult, viewModel.menuId)
                    }
                }
            }
        }
    }

    override fun onRenderingComplete() {
        handleDateOfBirth()
        lifecycleScope.launch {
            if (viewModel.getLastServiceHistory(MenuConstants.NCD_MENU_ID) != null) {
                formGenerator.getViewByTag(ID_NCD_SYMPTOMS_MEDICATION + rootSuffix)?.visible()
                formGenerator.getViewByTag(ID_DIAGNOSED_BP + rootSuffix)?.gone()
                formGenerator.getViewByTag(ID_DIAGNOSED_GLUCOSE + rootSuffix)?.gone()
            }
            prefillHeightAndWeightFromObservations()
        }
    }

    private suspend fun prefillHeightAndWeightFromObservations() {
        val (height, weight) = viewModel.getLatestHeightWeightFromServiceHistory() ?: return
        AssessmentUtil.prefillHeightAndWeight(
            formGenerator,
            height,
            weight,
            isHeightReadOnly = height != null,
        )
        viewModel.renderBMIValue(requireContext(), formGenerator, formGenerator.getResultMap())
    }

    /**
     * Hide eye care section if the member's age is less than 35 years
     */
    private fun handleDateOfBirth() {
        val age = DateUtils.calculateAge(viewModel.selectedMemberDob)
        if (age < 35) {
            formGenerator.getServerData()?.let { serverData ->
                formGenerator.getViewByTag(EYE_CARE + rootSuffix)?.gone()
                serverData.filter { it.family == EYE_CARE }.forEach {
                    formGenerator.getViewByTag(it.id + rootSuffix)?.gone()
                }
            }
        }
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
    }

    override fun onAgeCheckForPregnancy() {
    }

    override fun handleMandatoryCondition(formLayout: FormLayout?) {
    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout>?,
        resultHashMap: HashMap<String, Any>,
    ) {
    }

    override fun onQRScanRequested() {
    }
}
