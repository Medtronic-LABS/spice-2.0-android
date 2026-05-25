package org.medtroniclabs.uhis.ui.assessment.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.mappingkey.Screening.BMI_CATEGORY
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.NCDDetails
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.ncd
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

class AssessmentSLNCDFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    private lateinit var binding: FragmentAssessmentBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
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

    private fun initView() {
        viewModel.setUserJourney(AnalyticsDefinedParams.NCDASSESSMENT)
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        ) { map, id ->
            when (id) {
                Screening.Weight, Screening.Height -> {
                    viewModel.renderBMIValue(requireContext(), formGenerator, map)
                }
            }
        }
    }

    private fun setListeners() {
        binding.btnSubmit.safeClickListener(this)
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
        CheckBoxDialog
            .newInstance(id, resultMap) { resultMap ->
                formGenerator.validateCheckboxDialogue(id, formLayout, resultMap)
            }.show(childFragmentManager, CheckBoxDialog.TAG)
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
            val referralResult = ReferralResultGenerator().calculateNCDStatus(requireContext(), details)
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    AssessmentDefinedParams.ncd,
                )
            }
            result?.second?.let { map ->
                val ncdResultMap = map[ncd] as HashMap<String, Any>
                if (ncdResultMap.containsKey(BMI_CATEGORY) && ncdResultMap[BMI_CATEGORY] is String) {
                    val bmiCategory = ncdResultMap[BMI_CATEGORY]
                    ncdResultMap.remove(BMI_CATEGORY)
                    if (ncdResultMap.containsKey(NCDDetails) && ncdResultMap[NCDDetails] is HashMap<*, *>) {
                        (ncdResultMap[NCDDetails] as HashMap<String, Any>)[BMI_CATEGORY] = bmiCategory as String
                    }
                }
                map.remove(ncd)
                map[ncd] = ncdResultMap
                viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                viewModel.saveAssessment(serverData, map, referralResult, viewModel.menuId)
            }
        }
    }

    override fun onRenderingComplete() {
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

    override fun onClick(view: View?) {
        when (view?.id) {
            R.id.btnSubmit -> {
                withLocationCheck({
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(view)
                })
            }
        }
    }

    private fun getFormDataForWorkflow() {
        viewModel.getFormData(MenuConstants.NCD_MENU_ID)
        viewModel.getNearestHealthFacility()
    }

    companion object {
        const val TAG = "AssessmentSLNCDFragment"
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()
}
