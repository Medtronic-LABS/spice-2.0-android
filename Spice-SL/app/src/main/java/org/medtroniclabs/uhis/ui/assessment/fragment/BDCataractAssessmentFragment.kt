package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.CVDRiskCalculator
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.CATARACT
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NCD_SERVICE_PROVIDED
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.YES
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

@AndroidEntryPoint
class BDCataractAssessmentFragment() : BaseFragment(), FormEventListener {
    private lateinit var binding: FragmentAssessmentBinding

    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    companion object {
        const val TAG: String = "BDCataractAssessmentFragment"

        fun newInstance(): BDCataractAssessmentFragment = BDCataractAssessmentFragment()
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
        viewModel.getFormData(MenuConstants.CATARACT_MENU_ID)
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
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
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
                    MenuConstants.CATARACT_MENU_ID,
                )
            }

            viewModel.memberDetailsLiveData.value?.data?.let { memberDetail ->
                result?.second?.let { assessmentMap ->
                    lifecycleScope.launch {
                        val ncdMap = assessmentMap[CATARACT] as HashMap<String, Any>
                        val bpResult = AssessmentUtil.calculateAverageBloodPressure(ncdMap)
                        val bgResult = AssessmentUtil.addDateAndTimeForGlucose(ncdMap)

                        viewModel.isFollowupVisit = viewModel.getLastServiceHistory(MenuConstants.CATARACT_MENU_ID) != null
                        val referralResult =
                            ReferralResultGenerator().computeReferralResultForBDNCD(
                                ncdMap,
                                bpResult,
                                bgResult,
                                AssessmentUtil.getSymptomsList(ncdMap),
                                viewModel.isFollowupVisit,
                            )

                        val riskModels = viewModel.loadRiskClassificationModels()
                        CVDRiskCalculator.calculateCVDRiskFactor(
                            ncdMap,
                            riskModels,
                            memberDetail.dateOfBirth,
                            memberDetail.gender,
                        )
                        viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                        viewModel.saveAssessment(serverData, assessmentMap, referralResult, viewModel.menuId)
                    }
                }
            }
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    override fun onRenderingComplete() {
        lifecycleScope.launch {
            prefillHeightAndWeightFromObservations()
        }
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        if (id == NCD_SERVICE_PROVIDED && YES.equals(selectedId?.toString(), true)) {
            lifecycleScope.launch {
                prefillHeightAndWeightFromObservations()
            }
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
