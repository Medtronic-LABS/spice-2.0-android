package org.medtroniclabs.uhis.ui.assessment.fragment

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentOtherSymptomsBinding
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.formgeneration.utility.RecommendedDosageFragment
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.MenuConstants.OTHER_SYMPTOMS
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.referrallogic.model.ReferralDefinedParams
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel

@AndroidEntryPoint
class AssessmentOtherSymptomsFragment : BaseFragment(), FormEventListener, View.OnClickListener {
    private lateinit var binding: FragmentAssessmentOtherSymptomsBinding
    private lateinit var formGenerator: FormGenerator
    private val viewModel: AssessmentViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentOtherSymptomsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.getMemberDetailsById()
        initViews()
        getFormDataForWorkflow()
        initializeFormGenerator()
        setListeners()
        attachObservers()
        viewModel.setUserJourney(AnalyticsDefinedParams.OtherSymptoms)
    }

    private fun initViews() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )
    }

    private fun getFormDataForWorkflow() {
        viewModel.getFormData(OTHER_SYMPTOMS)
        viewModel.getNearestHealthFacility()
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

    private fun setListeners() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun initializeFormGenerator() {
        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
        )
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
        viewModel.instructionId = id
        viewModel.dosageListModel = dosageListModel
        showInstructionDialog(id)
    }

    private fun showInstructionDialog(id: String) {
        val titleById = getTitleById(id)
        when (id) {
            AssessmentDefinedParams.ACT.lowercase() -> {
                RecommendedDosageFragment
                    .newInstance(id, titleById)
                    .show(childFragmentManager, RecommendedDosageFragment.TAG)

                viewModel.setUserJourney(AnalyticsDefinedParams.ACTINFORMATIONDIALOUGE)
            }

            AssessmentDefinedParams.rdtTest -> {
                InformationLayoutFragment
                    .newInstance(id, titleById)
                    .show(childFragmentManager, InformationLayoutFragment.TAG)

                viewModel.setUserJourney(AnalyticsDefinedParams.RDTTESTINFORMATIONDIALOUGE)
            }
        }
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.let { details ->
            val referralResult =
                ReferralResultGenerator().calculateOtherSymptomsReferralResult(details)
            val result = serverData?.let {
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    OTHER_SYMPTOMS,
                )
            }

            result?.second?.let {
                viewModel.saveAssessment(serverData, it, referralResult, viewModel.menuId)
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.OtherSymptoms,
            eventName = AnalyticsDefinedParams.AssessmentCreation,
        )
    }

    override fun onRenderingComplete() {
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        when (id) {
            AssessmentDefinedParams.hasFever -> {
                renderDosageDetails()
            }

            AssessmentDefinedParams.rdtTest -> {
                if (selectedId == ReferralDefinedParams.RdtPositive) {
                    renderDosageDetails()
                }
            }
        }
    }

    private fun renderDosageDetails() {
        /**
         * ACT Status Condition Rendering
         */
        formGenerator.getViewByTag(AssessmentDefinedParams.ACTStatus)?.let {
            if (it is TextView) {
                it.text = requireContext().getString(R.string.act_6)
            }
        }
        formGenerator
            .getViewByTag(AssessmentDefinedParams.ACTStatus + AssessmentDefinedParams.infoSuffixText)
            ?.let {
                if (it is TextView) {
                    it.text =
                        getACTSuffixText(
                            viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dob ->
                                CommonUtils.convertStringDobToMonths(
                                    dob,
                                )
                            },
                        )
                    it.visibility = View.VISIBLE
                }
            }
    }

    private fun getTitleById(id: String): String =
        when (id) {
            AssessmentDefinedParams.muacCode -> getString(R.string.measuring_muac)
            AssessmentDefinedParams.hasOedemaOfBothFeet -> getString(R.string.checking_for_oedema)
            AssessmentDefinedParams.chestInDrawing -> getString(R.string.chest_in_drawing)
            AssessmentDefinedParams.rdtTest -> getString(R.string.job_aid_reading_result)
            else -> {
                id
            }
        }

    private fun getACTSuffixText(age: Int?): String =
        when (age) {
            in 6..36 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 2, 3)
            }

            in 37..96 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 4, 3)
            }

            in 97..168 -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 6, 3)
            }

            else -> {
                requireContext().getString(R.string.no_of_tablets_no_of_days, 8, 3)
            }
        }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        if (enteredDays != null && enteredDays > noOfDays) {
            updateColorCode(
                id,
                ContextCompat.getColor(requireContext(), R.color.medium_high_risk_color),
            )
            displayDaysInformation(id, View.VISIBLE)
        } else {
            updateColorCode(id, ContextCompat.getColor(requireContext(), R.color.secondary_black))
            displayDaysInformation(id, View.INVISIBLE)
        }
    }

    private fun displayDaysInformation(
        id: String,
        viewVisibility: Int,
    ) {
        formGenerator
            .getViewByTag(id + DefinedParams.INFORMATION)
            ?.apply { visibility = viewVisibility }
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
        /*
       Never used
         */
    }

    override fun onQRScanRequested() {
    }

    private fun updateColorCode(
        id: String,
        colorCode: Int,
    ) {
        formGenerator
            .getViewByTag(id + DefinedParams.INFORMATION)
            ?.let { view ->
                if (view is TextView) {
                    view.setTextColor(colorCode)
                }
            }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                withLocationCheck({
                    viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(view)
                })
            }
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val dosageDialog =
            childFragmentManager.findFragmentByTag("RecommendedDosageFragment") as? DialogFragment
        if (dosageDialog != null && dosageDialog.showsDialog) {
            dosageDialog.dismiss()
            showDialogBasedOnId()
        }
    }

    private fun showDialogBasedOnId() {
        viewModel.instructionId?.let {
            showInstructionDialog(it)
        }
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()
}
