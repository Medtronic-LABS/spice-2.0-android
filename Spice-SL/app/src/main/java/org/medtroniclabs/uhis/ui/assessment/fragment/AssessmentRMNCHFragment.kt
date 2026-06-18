package org.medtroniclabs.uhis.ui.assessment.fragment

import android.content.res.Configuration
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.RadioButton
import android.widget.TextView
import androidx.core.view.children
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import com.google.gson.JsonParser
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.appextensions.getLongTime
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DateUtils
import org.medtroniclabs.uhis.common.DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ
import org.medtroniclabs.uhis.common.DateUtils.calculateAgeInMonths
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.EntityMapper
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.FragmentAssessmentRmnchBinding
import org.medtroniclabs.uhis.db.entity.PregnancyDetail
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.formgeneration.utility.CheckBoxDialog
import org.medtroniclabs.uhis.formgeneration.utility.InformationLayoutFragment
import org.medtroniclabs.uhis.mappingkey.PregnantWomen
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFragment
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getMuacColorCode
import org.medtroniclabs.uhis.ui.assessment.AssessmentCommonUtils.getNutritionStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.MUAC
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.muacStatus
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.rootSuffix
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams.summaryKey
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ANCAssessmentEvaluator
import org.medtroniclabs.uhis.ui.assessment.referrallogic.AnemiaLevel
import org.medtroniclabs.uhis.ui.assessment.referrallogic.PNCAssessmentEvaluator
import org.medtroniclabs.uhis.ui.assessment.referrallogic.PNCAssessmentEvaluator.isValueEquals
import org.medtroniclabs.uhis.ui.assessment.referrallogic.PostpartumDangerSigns
import org.medtroniclabs.uhis.ui.assessment.referrallogic.ReferralResultGenerator
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH
import org.medtroniclabs.uhis.ui.assessment.rmnch.RMNCH.AncPncReferralType
import org.medtroniclabs.uhis.ui.assessment.utils.AssessmentUtil
import org.medtroniclabs.uhis.ui.assessment.viewmodel.AssessmentViewModel
import java.util.Calendar
import java.util.Locale

class AssessmentRMNCHFragment :
    BaseFragment(),
    View.OnClickListener,
    FormEventListener {
    private lateinit var binding: FragmentAssessmentRmnchBinding
    private val viewModel: AssessmentViewModel by activityViewModels()
    private lateinit var formGenerator: FormGenerator

    // Store original titles (without status) for each field
    private val originalTitles = HashMap<String, String>()

    /**
     * By default, non-mandatory
     */
    private var mandatoryHemoglobin = false

    /**
     * Dialog checkbox id
     */
    private var id: String = ""

    /**
     * Dialog checkbox formLayout
     */
    private var formLayout: FormLayout? = null

    /**
     * Dialog checkbox resultMap
     */
    private var resultMap: Any? = null

    /**
     * Dialog checkbox dialog key
     */
    private var dialogKey: String = ""

    /**
     * Boolean flag storing whether user has already selected treatments for existing illness or not.
     * Based on this we are showing high risk
     */
    private var ancSelectedTreatment = false

    companion object {
        const val TAG = "AssessmentRMNCHFragment"
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = FragmentAssessmentRmnchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        initView()
        viewModel.getNearestHealthFacility()
        attachObservers()
        setListener()
        UserDetail.startDateTime = AnalyticsUtils.getCurrentDateTimeInLocalTime()
    }

    private fun setListener() {
        binding.btnSubmit.safeClickListener(this)
    }

    private fun attachObservers() {
        viewModel.formLayoutsLiveData.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.SUCCESS -> {
                    hideProgress()
                    resourceState.data?.let { data ->
                        // For ANC and PNC workflow, filter out fields marked as isSummary
                        val filteredFormLayout = if (viewModel.workflowName == RMNCH.ANC || viewModel.workflowName == RMNCH.PNC) {
                            data.formLayout.filter { it.isSummary != true }
                        } else {
                            data.formLayout
                        }
                        formGenerator.populateViews(filteredFormLayout)
                    }
                }

                ResourceState.ERROR -> {
                    hideProgress()
                }

                ResourceState.LOADING -> {
                    showProgress()
                }
            }
        }

        viewModel.memberDetailsLiveData.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {}
                ResourceState.SUCCESS -> {
                    showHideOptionsForChildHealth()
                }

                ResourceState.ERROR -> {}
            }
        }

        viewModel.pregnancyDetailLiveData.observe(viewLifecycleOwner) { detail ->
            when (viewModel.workflowName) {
                RMNCH.PNC -> {
                    managePncFormBasedOnPregnancyDetail(detail)
                }

                RMNCH.ANC -> {
                    updateANCConditionalFieldVisibility()
                    updateAllFieldStatuses()
                }

                RMNCH.ChildHoodVisit -> {
                    manageChildFormBasedOnPregnancyDetail(detail)
                }
            }
        }

        viewModel.isAssessmentCancelLiveData.observe(viewLifecycleOwner) {
            if (it) {
                formGenerator.getViewByTag(MUAC)?.apply {
                    val background = background as? GradientDrawable
                    background?.setStroke(
                        resources.getDimensionPixelSize(R.dimen._1sdp),
                        context.getColor(R.color.edittext_stroke),
                    )
                }
            }
        }

        // Child symptoms filter
        viewModel.symptomTypeListResponse.observe(viewLifecycleOwner) { options ->
            val fragment = childFragmentManager.findFragmentByTag(CheckBoxDialog.TAG)
            // Fragment is already displayed, just return
            if (fragment != null) {
                return@observe
            }
            if (id == AssessmentDefinedParams.ID_CHILD_ILLNESS_TYPE) {
                viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let { dateOfBirth ->
                    calculateAgeInMonths(dateOfBirth)?.let { pair ->
                        val mutableOptions = options.toMutableList()
                        val listOfOptionsNotEligible = if (pair.first <= 18) {
                            listOf("cannotStandWalk", "cannotBalance", "cannotSpeak")
                        } else {
                            listOf("convulsion", "rapidBreathing", "fever", "lethargy", "unableToSuckMilk", "redUmbilicus", "skinRash", "eyeProblem")
                        }
                        mutableOptions.removeIf {
                            listOfOptionsNotEligible.contains(it.value)
                        }
                        val title = if (isTranslationEnabled && !formLayout?.hintCulture.isNullOrBlank()) {
                            formLayout?.hintCulture
                        } else {
                            formLayout?.hint
                        }
                        CheckBoxDialog
                            .newInstance(dialogKey, resultMap, title = title, inputData = mutableOptions) { map ->
                                formLayout?.let { formLayout ->
                                    formGenerator.validateCheckboxDialogue(id, formLayout, map)
                                }
                            }.show(childFragmentManager, CheckBoxDialog.TAG)
                    }
                }
            }
        }
    }

    private fun initView() {
        replaceFragmentInId<BioDataFragment>(
            binding.bioDataFragmentContainer.id,
            tag = BioDataFragment.TAG,
        )

        // Add Pregnancy Details Fragment for ANC & PNC workflow
        if (viewModel.workflowName == RMNCH.ANC || viewModel.workflowName == RMNCH.PNC) {
            replaceFragmentInId<PregnancyDetailsRMNCHFragment>(
                binding.pregnancyDetailsFragmentContainer.id,
                tag = PregnancyDetailsRMNCHFragment.TAG,
            )
        }
        childFragmentManager.executePendingTransactions() // Ensures transaction is complete

        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            this,
            binding.scrollView,
            translate = isTranslationEnabled,
            callback = { map, id ->
                when (viewModel.workflowName) {
                    RMNCH.PNC -> {
                        handlePNCDetails(id, map)
                    }

                    RMNCH.ANC -> {
                        handleAncDetails(id, map)
                    }
                }
            },
        )
        fetchWorkFlowData()
        fetchLatestAssessment()
    }

    /**
     * Gets triggered on changing of any value in the ANC form
     */
    private fun handleAncDetails(
        id: String,
        map: HashMap<String, Any>,
    ) {
        // Handle status updates for all fields that need status evaluation
        when (id) {
            AssessmentDefinedParams.BLOOD_SUGAR -> {
                val resultMap = formGenerator.getResultMap()
                if (resultMap[id] == AssessmentDefinedParams.VALUE_FASTING) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_FASTING + rootSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                    formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    // Get EditText directly using tag without rootSuffix
                    val editText = formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM) as? EditText
                    editText?.setText("")
                    // Also remove from result map
                    formGenerator.getResultMap().remove(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM)
                }
                if (resultMap[id] == AssessmentDefinedParams.VALUE_RANDOM) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM + rootSuffix)?.apply {
                        visibility = View.VISIBLE
                    }
                    formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_FASTING + rootSuffix)?.apply {
                        visibility = View.GONE
                    }
                    // Get EditText directly using tag without rootSuffix
                    val editText = formGenerator.getViewByTag(AssessmentDefinedParams.BLOOD_SUGAR_FASTING) as? EditText
                    editText?.setText("")
                    // Also remove from result map
                    formGenerator.getResultMap().remove(AssessmentDefinedParams.BLOOD_SUGAR_FASTING)
                }
            }

            AssessmentDefinedParams.SYSTOLIC -> {
                // Update BP-related fields when systolic or diastolic changes
                handleAncFieldStatusUpdate(AssessmentDefinedParams.SYSTOLIC)
                updateAllFieldStatuses() // BP affects multiple fields (edema, urinaryAlbumin)
            }

            AssessmentDefinedParams.DIASTOLIC -> {
                handleAncFieldStatusUpdate(AssessmentDefinedParams.DIASTOLIC)
                updateAllFieldStatuses() // BP affects multiple fields (edema, urinaryAlbumin)
            }

            AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS -> {
                // When existing illness changes, update status for dependent fields
                updateAllFieldStatuses() // Illness affects multiple fields
                // Specifically update BP fields, blood sugar fields, and on treatment status
                handleAncFieldStatusUpdate(AssessmentDefinedParams.SYSTOLIC)
                handleAncFieldStatusUpdate(AssessmentDefinedParams.DIASTOLIC)
                handleAncFieldStatusUpdate(AssessmentDefinedParams.BLOOD_SUGAR_FASTING)
                handleAncFieldStatusUpdate(AssessmentDefinedParams.BLOOD_SUGAR_RANDOM)
                val selectedData = (map[AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS] as? ArrayList<HashMap<String, Any>>) ?: emptyList()
                val isNoneSelected = selectedData.any { DefinedParams.None.equals(it[DefinedParams.Value] as? String, true) }
                if (isNoneSelected) {
                    // If none selected, then hide on treatment
                    formGenerator.getViewByTag(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT + formGenerator.rootSuffix)?.gone()
                } else {
                    // If none not selected, then show on treatment
                    formGenerator.getViewByTag(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT + formGenerator.rootSuffix)?.visible()
                }
                // Reset anc selected treatment flag
                ancSelectedTreatment = false
                // Reset value when we change existing illness
                formGenerator.getFormLayout(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT)?.let {
                    formGenerator.validateCheckboxDialogue(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT, it, arrayListOf(), true)
                }
                handleAncFieldStatusUpdate(AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT)
            }

            AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT -> {
                updateAllFieldStatuses() // Treatment affects multiple fields
            }

            AssessmentDefinedParams.EDEMA, AssessmentDefinedParams.URINARY_ALBUMIN -> {
                updateAllFieldStatuses() // These affect each other
            }

            AssessmentDefinedParams.FOLIC_ACID_TOTAL_CONSUMED -> {
                handleAncFieldStatusUpdate(AssessmentDefinedParams.FOLIC_ACID_TABLETS)
            }

            AssessmentDefinedParams.IFA_TOTAL_CONSUMED -> {
                handleAncFieldStatusUpdate(AssessmentDefinedParams.IFA_TABLETS)
            }

            AssessmentDefinedParams.CALCIUM_TOTAL_CONSUMED -> {
                handleAncFieldStatusUpdate(AssessmentDefinedParams.CALCIUM_TABLETS)
            }

            AssessmentDefinedParams.BLOOD_SUGAR_FASTING, AssessmentDefinedParams.BLOOD_SUGAR_RANDOM -> {
                // Update blood sugar status when value changes
                handleAncFieldStatusUpdate(id)
            }

            AssessmentDefinedParams.ANC_FROM_MEDICAL_DOCTOR, AssessmentDefinedParams.ULTRASOUND -> {
                // Update status when these fields change (they depend on gestational age)
                handleAncFieldStatusUpdate(id)
            }

            AssessmentDefinedParams.TEMPERATURE,
            AssessmentDefinedParams.PULSE,
            AssessmentDefinedParams.FUNDAL_HEIGHT,
            AssessmentDefinedParams.HEMOGLOBIN,
            AssessmentDefinedParams.URINARY_SUGAR,
            AssessmentDefinedParams.URINARY_BILIRUBIN,
            AssessmentDefinedParams.FACILITY_IDENTIFIED_FOR_DELIVERY,
            -> {
                // Update status for individual fields
                handleAncFieldStatusUpdate(id)
            }

            Screening.Weight -> {
                // Calculate BMI only if visible
                // BMI - show only if ANC visit 1 AND gestational age < 12 weeks
                if (formGenerator.isViewVisible(AssessmentDefinedParams.BMI)) {
                    viewModel.renderBMIValue(requireContext(), formGenerator, map)
                }
                // Update status for individual fields
                handleAncFieldStatusUpdate(id)
            }

            Screening.Height -> {
                // Calculate BMI only if visible
                // BMI - show only if ANC visit 1 AND gestational age < 12 weeks
                if (formGenerator.isViewVisible(AssessmentDefinedParams.BMI)) {
                    viewModel.renderBMIValue(requireContext(), formGenerator, map)
                }
                handleAncFieldStatusUpdate(id)
            }
        }
    }

    private fun showHideOptionsForChildHealth() {
        viewModel.memberDetailsLiveData.value?.data?.dateOfBirth?.let {
            calculateAgeInMonths(it)?.let { pair ->
                if (pair.first > 18) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.WHAT_FED_LAST_24_HRS + rootSuffix)?.visibility = View.GONE
                    formGenerator.getViewByTag(AssessmentDefinedParams.BREAST_FEEDING + rootSuffix)?.visibility = View.VISIBLE
                    formGenerator.getViewByTag(AssessmentDefinedParams.ADDITIONAL_FOOD_GIVEN_LAST_24_HRS + rootSuffix)?.visibility = View.VISIBLE
                    formGenerator.getViewByTag(AssessmentDefinedParams.DEWORMING_MEDICINE + rootSuffix)?.visibility = View.VISIBLE
                    formGenerator.getViewByTag(AssessmentDefinedParams.VACCINE_RECEIVED + rootSuffix)?.visibility = View.VISIBLE
                } else if (pair.first > 11) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.BREAST_FEEDING + rootSuffix)?.visibility = View.VISIBLE
                    formGenerator.getViewByTag(AssessmentDefinedParams.ADDITIONAL_FOOD_GIVEN_LAST_24_HRS + rootSuffix)?.visibility = View.VISIBLE
                    formGenerator.getViewByTag(AssessmentDefinedParams.DEWORMING_MEDICINE + rootSuffix)?.visibility = View.VISIBLE
                } else if (pair.first > 6) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.ADDITIONAL_FOOD_GIVEN_MONTHS + rootSuffix)?.visibility = View.VISIBLE
                } else if (pair.first <= 3) {
                    formGenerator.getViewByTag(AssessmentDefinedParams.HOURS_BREAST_FEED_AFTER_BIRTH + rootSuffix)?.visibility = View.VISIBLE
                }
            }
        }
    }

    /**
     * Gets triggered on changing of any value in the PNC form
     */
    private fun handlePNCDetails(
        id: String,
        resultMap: HashMap<String, Any>,
    ) {
        when (id) {
            RMNCH.ID_POSTPARTUM_DANGER_SIGNS -> {
                val dangerSigns = resultMap[id] as? List<*>
                if (!dangerSigns.isNullOrEmpty()) {
                    val haveHeavyBleeding = dangerSigns.any {
                        it is HashMap<*, *> && it[DefinedParams.Value] == PostpartumDangerSigns.HEAVY_BLEEDING.value
                    }
                    // Hemoglobin (Hb) : Mandatory Woman reporting heavy bleeding during PNC period
                    formGenerator.markMandatory(RMNCH.ID_HEMOGLOBIN, haveHeavyBleeding || mandatoryHemoglobin)
                }
                handlePncFieldStatusUpdate(id)
            }

            RMNCH.ID_IFA_TABLETS_CONSUMED -> {
                handlePncFieldStatusUpdate(RMNCH.ID_IFA_TABLETS)
            }

            RMNCH.ID_CALCIUM_TABLETS_CONSUMED -> {
                handlePncFieldStatusUpdate(RMNCH.ID_CALCIUM_TABLETS)
            }

            RMNCH.ID_VITAMIN_A_CONSUMED,
            RMNCH.ID_FAMILY_PLANNING_METHODS,
            RMNCH.ID_TEMPERATURE,
            RMNCH.ID_URINARY_BILIRUBIN,
            RMNCH.ID_PULSE,
            RMNCH.ID_HEMOGLOBIN,
            -> {
                handlePncFieldStatusUpdate(id)
            }

            RMNCH.ID_ECLAMPSIA -> {
                handlePncFieldStatusUpdate(id)
                handleOnTreatmentHtnEclampsiaVisibility(resultMap)
            }

            RMNCH.ID_EDEMA,
            RMNCH.ID_URINARY_ALBUMIN,
            -> {
                handlePncBpHighRiskHighlight()
            }

            RMNCH.ID_KNOWN_HTN -> {
                handlePncFieldStatusUpdate(id)
                handlePncBpHighRiskHighlight()
                handleOnTreatmentHtnEclampsiaVisibility(resultMap)
            }

            RMNCH.ID_GDM_PATIENT,
            RMNCH.ID_DM_PATIENT,
            -> {
                handlePncFieldStatusUpdate(id)
                handlePncBloodSugarHighRiskHighlight()
                handleOnTreatmentDmGdmVisibility(resultMap)
            }

            RMNCH.ID_FASTING_BLOOD_SUGAR,
            RMNCH.ID_RANDOM_BLOOD_SUGAR,
            -> {
                handlePncBloodSugarHighRiskHighlight()
            }

            AssessmentDefinedParams.SYSTOLIC,
            AssessmentDefinedParams.DIASTOLIC,
            -> {
                handlePncBpHighRiskHighlight()
            }
        }
    }

    /**
     * Updates visibility of HTN/Eclampsia on-treatment radio group.
     *
     * The on-treatment field is shown when either Known HTN or Eclampsia is selected as "yes".
     * Otherwise, child values are reset and the field is hidden.
     */
    private fun handleOnTreatmentHtnEclampsiaVisibility(resultMap: HashMap<String, Any>) {
        handleOnTreatmentVisibility(
            resultMap = resultMap,
            treatmentFieldId = RMNCH.ID_ON_TREATMENT_HTN_ECLAMPSIA,
            conditionFieldIds = arrayOf(RMNCH.ID_KNOWN_HTN, RMNCH.ID_ECLAMPSIA),
        )
    }

    /**
     * Updates visibility of DM/GDM on-treatment radio group.
     *
     * The on-treatment field is shown when either DM or GDM is selected as "yes".
     * Otherwise, child values are reset and the field is hidden.
     */
    private fun handleOnTreatmentDmGdmVisibility(resultMap: HashMap<String, Any>) {
        handleOnTreatmentVisibility(
            resultMap = resultMap,
            treatmentFieldId = RMNCH.ID_ON_TREATMENT_DM_GDM,
            conditionFieldIds = arrayOf(RMNCH.ID_DM_PATIENT, RMNCH.ID_GDM_PATIENT),
        )
    }

    /**
     * Generic helper to control visibility for an on-treatment field based on one or more
     * related condition fields.
     *
     * @param resultMap Current form values keyed by field id.
     * @param treatmentFieldId Field id of the on-treatment radio group to show/hide.
     * @param conditionFieldIds Condition field ids checked for "yes" to decide visibility.
     */
    private fun handleOnTreatmentVisibility(
        resultMap: HashMap<String, Any>,
        treatmentFieldId: String,
        conditionFieldIds: Array<String>,
    ) {
        val onTreatmentView = formGenerator.getViewByTag(treatmentFieldId + rootSuffix)
        val shouldShowOnTreatment = conditionFieldIds.any { fieldId ->
            isValueEquals(resultMap[fieldId], DefinedParams.YES_SMALL)
        }
        onTreatmentView?.let {
            if (shouldShowOnTreatment) {
                onTreatmentView.visible()
            } else {
                formGenerator.resetChildViews(onTreatmentView)
                onTreatmentView.gone()
            }
        }
    }

    private fun fetchWorkFlowData() {
        viewModel.workflowName?.let { name ->
            viewModel.getFormData(name)
        }
    }

    private fun fetchLatestAssessment() {
        viewModel.workflowName?.let { type ->
            viewModel.fetchLatestAssessment(type)
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            binding.btnSubmit.id -> {
                withLocationCheck({
                    viewModel.fetchCurrentLocation(requireContext())
                    formGenerator.formSubmitAction(v)
                })
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
        this.id = id
        this.formLayout = formLayout
        this.resultMap = resultMap
        // Use localDataCache if available, otherwise use id (for database loading)
        dialogKey = formLayout.localDataCache ?: id

        val title = if (isTranslationEnabled && !formLayout.hintCulture.isNullOrBlank()) {
            formLayout.hintCulture
        } else {
            formLayout.hint
        }

        // If type is child illness the prefetch the data as
        // we want to manipulate it based on some conditions
        if (id == AssessmentDefinedParams.ID_CHILD_ILLNESS_TYPE) {
            viewModel.getSymptomListByType(dialogKey)
            return
        }

        // For treatment field, convert illness selections to SignsAndSymptomsEntity and pass as inputData
        // This will filter the dialog to only show the selected illness items as available options
        if (id == AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT) {
            // Get illness selections - these will be available as options in treatment dialog
            val illnessSelections = formGenerator.getResult(AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS)
                as? ArrayList<HashMap<String, Any>> ?: arrayListOf()
            val illnessOptions = formGenerator.getFormLayout(AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS)?.optionsList ?: arrayListOf()
            val illnessSelectionsValues = illnessSelections
                .map {
                    it[DefinedParams.Value]?.toString()?.lowercase(Locale.ENGLISH) ?: ""
                }.filterNot { it.equals(DefinedParams.None, true) }

            // Filter selected illness sections
            val filteredSelections = illnessOptions
                .filter { illnessItem ->
                    val value = illnessItem[DefinedParams.Value]?.toString()?.lowercase(Locale.ENGLISH) ?: ""
                    illnessSelectionsValues.contains(value)
                }.toMutableList()
            // Add not taking any treatment option from JSON
            formLayout.optionsList?.let {
                filteredSelections.addAll(it)
            }

            val inputData = EntityMapper.mapToSignsAndSymptomsEntity(filteredSelections, dialogKey)

            CheckBoxDialog
                .newInstance(
                    dialogKey,
                    resultMap,
                    title = title,
                    inputData = inputData,
                ) { map ->
                    ancSelectedTreatment = true
                    formGenerator.validateCheckboxDialogue(id, formLayout, map)
                }.show(childFragmentManager, CheckBoxDialog.TAG)
        } else {
            if (formLayout.localDataCache != null) {
                CheckBoxDialog
                    .newInstance(dialogKey, resultMap, title = title) { map ->
                        formGenerator.validateCheckboxDialogue(id, formLayout, map)
                    }.show(childFragmentManager, CheckBoxDialog.TAG)
            } else {
                val inputData = EntityMapper.mapToSignsAndSymptomsEntity(formLayout.optionsList)
                CheckBoxDialog
                    .newInstance(dialogKey, resultMap, title = title, inputData = inputData) { map ->
                        formGenerator.validateCheckboxDialogue(id, formLayout, map)
                    }.show(childFragmentManager, CheckBoxDialog.TAG)
            }
        }
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        viewModel.instructionId = id
        showInstructionDialog(id)
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        resultMap?.let { details ->
            viewModel.setUserJourney(AnalyticsDefinedParams.SUBMITBUTTONTRIGGERED)
            val result = serverData?.let {
                val menuType = when (viewModel.workflowName) {
                    RMNCH.PNC -> RMNCH.PNC
                    RMNCH.ANC -> RMNCH.ANC
                    else -> {
                        null
                    }
                }
                FormResultComposer().groupValues(
                    serverData = it,
                    details,
                    menuType = menuType,
                )
            }
            result?.second?.let { second ->
                handleNextPregnancyFlow(serverData, second)
            }
        }
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = viewModel.workflowName.plus(AnalyticsDefinedParams.RMNCHAssessment),
            eventName = AnalyticsDefinedParams.AssessmentCreation,
        )
    }

    private fun handleNextPregnancyFlow(
        serverData: List<FormLayout>,
        second: HashMap<String, Any>,
    ) {
        viewModel.workflowName?.let { name ->
            when (name) {
                RMNCH.PNC -> {
                    savePNC(second)
                }

                else -> {
                    // Evaluate and add highRiskPregnantWoman and gapsInAnc to result map for ANC workflow
                    if (name == RMNCH.ANC) {
                        if (second.containsKey(name) && second[name] is Map<*, *>) {
                            val clinicalMap = second[name] as HashMap<String, Any>
                            clinicalMap[RMNCH.visitNo] = getANCVisitNumber()
                        }
                        evaluateAndAddAncSummaryData(second)
                    } else {
                        if (second.containsKey(name) && second[name] is Map<*, *>) {
                            val clinicalMap = second[name] as HashMap<String, Any>
                            clinicalMap[RMNCH.visitNo] = getChildVisitNumber()
                        }
                        // Save visit detail into the pregnancy table
                        viewModel.memberDetailsLiveData.value?.data?.let { memberDetail ->
                            viewModel.handlePregnancy(
                                second,
                                workflowName = name,
                                memberDetail,
                            )
                        }
                    }

                    val resultGenerator = ReferralResultGenerator()
                    val referralResult = resultGenerator.calculateRMNCHReferralResult(second)
                    viewModel.saveAssessment(
                        serverData,
                        second,
                        referralResult,
                        RMNCH.getMenuName(viewModel.workflowName),
                    )
                }
            }
        }
    }

    private fun savePNC(assessmentMap: HashMap<String, Any>) {
        viewModel.memberDetailsLiveData.value?.data?.let { memberDetail ->
            evaluateAndAddPncSummaryData(assessmentMap)
            viewModel.handlePregnancy(
                assessmentMap,
                workflowName = RMNCH.PNC,
                memberDetail,
            )
            viewModel.savePNCDetails(
                formGenerator.getServerData(),
                assessmentMap = assessmentMap,
                memberDetail = memberDetail,
                followUpId = viewModel.followUpId,
            )
        }
    }

    /**
     * Calculates risks and gaps and stores in the map
     */
    private fun evaluateAndAddPncSummaryData(assessmentMap: HashMap<String, Any>) {
        val pncMap = assessmentMap[RMNCH.PNC] as? HashMap<String, Any> ?: return
        // Update days since delivery
        getDaysSinceDelivery()?.let { days ->
            pncMap[RMNCH.ID_DAYS_SINCE_DELIVERY] = days
        }
        val gapsInPnc = PNCAssessmentEvaluator.getPncGaps(pncMap)
        if (gapsInPnc.isNotEmpty()) {
            pncMap[RMNCH.ID_PNC_GAPS] = gapsInPnc
        }
        val urgentReferrals = PNCAssessmentEvaluator.getUrgentReferral(pncMap)
        val nonUrgentReferrals = PNCAssessmentEvaluator.getNonUrgentReferral(pncMap)
        val motherRisks = hashMapOf<String, Any>()
        if (urgentReferrals.isNotEmpty()) {
            motherRisks[AncPncReferralType.URGENT.name] = urgentReferrals
        }
        if (nonUrgentReferrals.isNotEmpty()) {
            motherRisks[AncPncReferralType.NON_URGENT.name] = nonUrgentReferrals
        }
        if (motherRisks.isNotEmpty()) {
            pncMap[RMNCH.ID_MOTHER_RISKS] = motherRisks
        }
        val pncIllness = hashMapOf<String, Any?>()
        (pncMap[RMNCH.ID_MATERNAL_HEALTH_ASSESSMENT] as? Map<*, *>)?.let { maternalAssessment ->
            val anemiaLevel = PNCAssessmentEvaluator.getAnemiaLevel(pncMap)
            pncIllness[RMNCH.ID_DM_PATIENT] = maternalAssessment[RMNCH.ID_DM_PATIENT] as? String
            pncIllness[RMNCH.ID_GDM_PATIENT] = maternalAssessment[RMNCH.ID_GDM_PATIENT] as? String
            pncIllness[RMNCH.ID_KNOWN_HTN] = maternalAssessment[RMNCH.ID_KNOWN_HTN] as? String
            pncIllness[RMNCH.ID_ECLAMPSIA] = maternalAssessment[RMNCH.ID_ECLAMPSIA] as? String
            pncIllness[RMNCH.ID_ANEMIA] = anemiaLevel.name
            pncIllness[RMNCH.ID_BLOOD_SUGAR] = PNCAssessmentEvaluator.isHighBloodSugar(pncMap)
        }
        pncMap[RMNCH.ID_PNC_ILLNESS] = pncIllness
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        val instructionDialog =
            childFragmentManager.findFragmentByTag(AssessmentDefinedParams.InformationLayoutFragment) as? DialogFragment
        if (instructionDialog != null && instructionDialog.showsDialog) {
            instructionDialog.dismiss()
            showDialogBasedOnId()
        }
    }

    private fun showDialogBasedOnId() {
        viewModel.instructionId?.let {
            showInstructionDialog(it)
        }
    }

    private fun showInstructionDialog(id: String) {
        val titleById = getTitleById(id)
        when (id) {
            MUAC -> {
                InformationLayoutFragment
                    .newInstance(id, titleById)
                    .show(childFragmentManager, InformationLayoutFragment.TAG)
                viewModel.setUserJourney("$id ${AnalyticsDefinedParams.INFORMATIONDIALOUGE}")
            }
        }
    }

    private fun getTitleById(id: String): String =
        when (id) {
            MUAC -> getString(R.string.measuring_muac)
            else -> {
                id
            }
        }

    override fun onRenderingComplete() {
        viewModel.formRenderedLiveData.postValue(true)
        // Show biodata after form rendering completes
        binding.bioDataFragmentContainer.visible()
        // Show pregnancy biodata after form rendering completes
        if (viewModel.workflowName == RMNCH.ANC || viewModel.workflowName == RMNCH.PNC) {
            binding.pregnancyDetailsFragmentContainer.visible()
        }
        if (viewModel.pregnancyDetailLiveData.isInitialized) {
            when (viewModel.workflowName) {
                RMNCH.PNC -> {
                    managePncFormBasedOnPregnancyDetail(viewModel.pregnancyDetailLiveData.value)
                }

                RMNCH.ANC -> {
                    updateANCConditionalFieldVisibility()
                    updateAllFieldStatuses()
                }

                RMNCH.ChildHoodVisit -> {
                    manageChildFormBasedOnPregnancyDetail(viewModel.pregnancyDetailLiveData.value)
                }
            }
        }
    }

    /**
     * Manipulates form based on pregnancy details
     */
    private fun managePncFormBasedOnPregnancyDetail(details: PregnancyDetail?) {
        val pregnancyHistoryCardView = formGenerator.getViewByTag(RMNCH.ID_PREGNANCY_HISTORY + formGenerator.rootSuffix)
        val pregnancyHistoryView = formGenerator.getViewByTag(RMNCH.ID_PREGNANCY_HISTORY) as? ViewGroup
        val pncVisitCount = getPncVisitNumber()
        val ancVisitCountForPnc = getAncVisitNumberForPnc()

        // For first PNC visit only, if parity is not set, then ask for pregnancy history
        // The user is visiting the member for the first time, no PW Profile, no ANC.
        if (pncVisitCount <= 1L && ancVisitCountForPnc < 1) {
            pregnancyHistoryCardView?.visible()
            formGenerator.getViewByTag(PregnantWomen.ID_PARITY + formGenerator.rootSuffix)?.visible()
        } else {
            pregnancyHistoryCardView?.gone()
            pregnancyHistoryView?.children?.forEach { pregnancyField ->
                formGenerator.resetChildViews(pregnancyField)
                pregnancyField.gone()
            }
        }
        // By default, non-mandatory
        var bloodSugarMandatory = false

        if (pncVisitCount <= 1) {
            // For first PNC visit autopopulate the data from ANC details and disable the fields,
            // if ANC details captured

            // Mandatory during 1st PNC visit in case of
            // Woman with excessive bleeding during delivery
            if (details?.complicationsDuringDelivery?.contains("Excessive bleeding", true) == true) {
                mandatoryHemoglobin = true
            }

            if (ancVisitCountForPnc > 0) {
                // HTN
                disablePncIllnessView(RMNCH.ID_KNOWN_HTN)
                setPncIllnessValue(RMNCH.ID_KNOWN_HTN) {
                    details?.pregnantWomanExistingIllness?.contains("HTN", true) == true
                }

                // Eclampsia
                disablePncIllnessView(RMNCH.ID_ECLAMPSIA)
                setPncIllnessValue(RMNCH.ID_ECLAMPSIA) {
                    details?.pregnantWomanExistingIllness?.contains("Eclampsia", true) == true
                }

                // DM
                disablePncIllnessView(RMNCH.ID_DM_PATIENT)
                setPncIllnessValue(RMNCH.ID_DM_PATIENT) {
                    // Mandatory during 1st PNC visit in case of
                    // Woman had GDM or was a known DM patient
                    bloodSugarMandatory = details?.pregnantWomanExistingIllness?.contains("DM", true) == true
                    bloodSugarMandatory
                }

                // GDM
                disablePncIllnessView(RMNCH.ID_GDM_PATIENT)
                setPncIllnessValue(RMNCH.ID_GDM_PATIENT) {
                    // Mandatory during 1st PNC visit in case of
                    // Woman had GDM or was a known DM patient
                    bloodSugarMandatory = details?.pregnantWomanExistingIllness?.contains("GDM", true) == true
                    bloodSugarMandatory
                }

                if (details?.highRiskPregnantWoman?.contains("Anemia", true) == true) {
                    // Mandatory during 1st PNC visit in case of
                    // Woman who had Anemia during pregnancy
                    mandatoryHemoglobin = true
                }
            } else {
                // Mandatory during 1st PNC visit in case of
                // Woman's first interaction with SK during PNC (i,e no ANC received from SK) only during 1st PNC visit
                bloodSugarMandatory = true

                // Mandatory  during 1st PNC Visit only for
                // Woman's first interaction with SK during PNC (i,e no ANC received from SK) only during 1st PNC visit
                mandatoryHemoglobin = true
            }
        } else {
            // For subsequent visit hide the fields and autopopulate the data from PNC details

            // Hide HTN
            (formGenerator.getViewByTag(RMNCH.ID_KNOWN_HTN + formGenerator.rootSuffix))?.gone()

            // Hide Eclampsia
            (formGenerator.getViewByTag(RMNCH.ID_ECLAMPSIA + formGenerator.rootSuffix))?.gone()

            // Hide DM
            (formGenerator.getViewByTag(RMNCH.ID_DM_PATIENT + formGenerator.rootSuffix))?.gone()

            // Hide GD
            (formGenerator.getViewByTag(RMNCH.ID_GDM_PATIENT + formGenerator.rootSuffix))?.gone()

            details?.pncIllness?.let { pncIllness ->
                val pncIllnessObject = JsonParser.parseString(pncIllness).asJsonObject
                val dmPatient = pncIllnessObject.get(RMNCH.ID_DM_PATIENT).asString
                val gdmPatient = pncIllnessObject.get(RMNCH.ID_GDM_PATIENT).asString
                val htnPatient = pncIllnessObject.get(RMNCH.ID_KNOWN_HTN).asString
                val eclampsiaPatient = pncIllnessObject.get(RMNCH.ID_ECLAMPSIA).asString
                val isHighBloodSugar = pncIllnessObject.get(RMNCH.ID_BLOOD_SUGAR).asBoolean
                // Mandatory during subsequent PNC visit if -
                // Woman who had GDM or was a known DM patient OR
                // Blood Sugar values were higher than normal
                if (dmPatient.equals(DefinedParams.YES_SMALL, true) ||
                    gdmPatient.equals(DefinedParams.YES_SMALL, true) ||
                    isHighBloodSugar
                ) {
                    bloodSugarMandatory = true
                }
                setPncIllnessValue(RMNCH.ID_DM_PATIENT) {
                    isValueEquals(dmPatient, DefinedParams.YES_SMALL)
                }
                setPncIllnessValue(RMNCH.ID_GDM_PATIENT) {
                    isValueEquals(gdmPatient, DefinedParams.YES_SMALL)
                }
                setPncIllnessValue(RMNCH.ID_KNOWN_HTN) {
                    isValueEquals(htnPatient, DefinedParams.YES_SMALL)
                }
                setPncIllnessValue(RMNCH.ID_ECLAMPSIA) {
                    isValueEquals(eclampsiaPatient, DefinedParams.YES_SMALL)
                }
                val anemiaLevel = try {
                    AnemiaLevel.valueOf(pncIllnessObject.get(RMNCH.ID_ANEMIA).asString)
                } catch (_: Exception) {
                    AnemiaLevel.None
                }

                // Mandatory during subsequent PNC  visit in case of Woman with moderate and/or severe anemia
                mandatoryHemoglobin = anemiaLevel == AnemiaLevel.Moderate || anemiaLevel == AnemiaLevel.Severe
            }
        }

        formGenerator.markMandatory(RMNCH.ID_HEMOGLOBIN, mandatoryHemoglobin)

        formGenerator.markMandatory(RMNCH.ID_BLOOD_SUGAR, bloodSugarMandatory)
        formGenerator.markMandatory(RMNCH.ID_FASTING_BLOOD_SUGAR, bloodSugarMandatory)
        formGenerator.markMandatory(RMNCH.ID_RANDOM_BLOOD_SUGAR, bloodSugarMandatory)
    }

    /**
     * Sets radio button status and updates map for pnc illness's
     * like dm, gdm, htn, eclampsia
     */
    private fun setPncIllnessValue(
        id: String,
        hasIllness: () -> Boolean,
    ) {
        val illnessView = formGenerator.getViewByTag(id + formGenerator.rootSuffix)
        illnessView?.let {
            if (hasIllness()) {
                (illnessView.findViewWithTag(DefinedParams.YES_SMALL) as? RadioButton)?.isChecked = true
            } else {
                (illnessView.findViewWithTag(DefinedParams.NO_SMALL) as? RadioButton)?.isChecked = true
            }
        }
    }

    private fun disablePncIllnessView(id: String) {
        val illnessView = formGenerator.getViewByTag(id + formGenerator.rootSuffix)
        illnessView?.let {
            illnessView.isEnabled = false
            (illnessView.findViewWithTag(DefinedParams.YES_SMALL) as? RadioButton)?.isEnabled = false
            (illnessView.findViewWithTag(DefinedParams.NO_SMALL) as? RadioButton)?.isEnabled = false
        }
    }

    /**
     * Manipulates form based on pregnancy details
     */
    private fun manageChildFormBasedOnPregnancyDetail(details: PregnancyDetail?) {
        val visitNumber = getChildVisitNumber()
        if (visitNumber > 1) {
            // CongenitalDefect
            val congenitalView = formGenerator.getViewByTag(AssessmentDefinedParams.ID_CONGENITAL_DEFECT + formGenerator.rootSuffix)
            congenitalView?.let {
                (congenitalView.findViewWithTag("${DefinedParams.YES_SMALL}_${AssessmentDefinedParams.ID_CONGENITAL_DEFECT}") as? View)?.isEnabled = false
                (congenitalView.findViewWithTag("${DefinedParams.NO_SMALL}_${AssessmentDefinedParams.ID_CONGENITAL_DEFECT}") as? View)?.isEnabled = false
                formGenerator.disableView(congenitalView)
                if (isValueEquals(details?.childCongenitalDefect, DefinedParams.YES_SMALL)) {
                    formGenerator.getResultMap()[AssessmentDefinedParams.ID_CONGENITAL_DEFECT] = details?.childCongenitalDefect ?: ""
                    (congenitalView.findViewWithTag("${DefinedParams.YES_SMALL}_${AssessmentDefinedParams.ID_CONGENITAL_DEFECT}") as? View)?.isSelected = true
                } else if (isValueEquals(details?.childCongenitalDefect, DefinedParams.NO_SMALL)) {
                    formGenerator.getResultMap()[AssessmentDefinedParams.ID_CONGENITAL_DEFECT] = details?.childCongenitalDefect ?: ""
                    (congenitalView.findViewWithTag("${DefinedParams.NO_SMALL}_${AssessmentDefinedParams.ID_CONGENITAL_DEFECT}") as? View)?.isSelected = true
                } else {
                    congenitalView.gone()
                }
            }
        }
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        when (id) {
            MUAC -> {
                val rootSuffixTag = muacStatus + rootSuffix
                val summaryKeyTag = muacStatus + summaryKey
                val muacStatusTag = muacStatus

                if (selectedId is String && selectedId != DefinedParams.DEFAULT_ID) {
                    formGenerator.getViewByTag(rootSuffixTag)?.visibility = View.VISIBLE

                    (formGenerator.getViewByTag(summaryKeyTag) as? TextView)?.text =
                        requireContext().getString(
                            R.string.firstname_lastname,
                            MUAC.uppercase(),
                            selectedId,
                        )

                    (formGenerator.getViewByTag(muacStatusTag) as? TextView)?.text =
                        getNutritionStatus(selectedId, requireContext())
                    formGenerator.getViewByTag(MUAC)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(
                            resources.getDimensionPixelSize(R.dimen._4sdp),
                            getMuacColorCode(selectedId, requireContext()),
                        )
                    }
                } else {
                    formGenerator.getViewByTag(rootSuffixTag)?.visibility = View.GONE
                    formGenerator.getViewByTag(MUAC)?.apply {
                        val background = background as? GradientDrawable
                        background?.setStroke(
                            resources.getDimensionPixelSize(R.dimen._1sdp),
                            getMuacColorCode(selectedId as String, requireContext()),
                        )
                    }
                }
            }
        }
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
        /*
       Never used
         */
    }

    override fun onQRScanRequested() {
    }

    fun getCurrentAnsweredStatus(): Boolean = formGenerator.getResultMap().isNotEmpty()

    /**
     * Calculates gestational age in weeks from LMP date
     * Returns null if LMP is not available
     */
    private fun calculateGestationalAgeInWeeks(): Double? =
        try {
            // Try to get from pregnancy detail
            viewModel.pregnancyDetailLiveData.value?.lastMenstrualPeriod?.let { lmp ->
                val lmpCalendar = DateUtils.getLastMenstrualDate(lmp)
                val gestationalAge = DateUtils.calculateGestationalAge(lmpCalendar)
                gestationalAge.first.toDouble() + (gestationalAge.second / AssessmentDefinedParams.DAYS_PER_WEEK)
            }
        } catch (_: Exception) {
            null
        }

    /**
     * Updates field visibility based on ANC visit number and gestational age conditions
     */
    private fun updateANCConditionalFieldVisibility() {
        val visitNumber = getANCVisitNumber()
        val gestationalAgeWeeks = calculateGestationalAgeInWeeks()

        // Pre-fill existing illness from last visit
        if (visitNumber > 1) {
            viewModel.pregnancyDetailLiveData.value?.pregnantWomanExistingIllness?.let { existingIllness ->
                val existingIllnessList = StringConverter.convertStringToList<String>(existingIllness)
                formGenerator.getFormLayout(AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS)?.let { formLayout ->
                    formLayout.optionsList?.filter { existingIllnessList.contains(it[DefinedParams.Value]) }?.let { existingIllnessValue ->
                        val existingIllnessValue = ArrayList(
                            existingIllnessValue.map {
                                hashMapOf<String, Any>().apply {
                                    putAll(it)
                                }
                            },
                        )
                        formGenerator.validateCheckboxDialogue(AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS, formLayout, existingIllnessValue)
                    }
                }
            }
        }

        // Show previous pregnancy complications only for 1st visit
        updateFieldVisibility(AssessmentDefinedParams.PREVIOUS_PREGNANCY_COMPLICATIONS, visitNumber == AssessmentDefinedParams.ANC_VISIT_NUMBER_1)

        // 1. Height - show only if ANC visit is 1
        updateFieldVisibility(AssessmentDefinedParams.HEIGHT, visitNumber == AssessmentDefinedParams.ANC_VISIT_NUMBER_1)

        // 2. BMI - show only if ANC visit 1 AND gestational age < 12 weeks
        val showBMI =
            visitNumber == AssessmentDefinedParams.ANC_VISIT_NUMBER_1 &&
                (gestationalAgeWeeks == null || gestationalAgeWeeks < AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12)
        updateFieldVisibility(AssessmentDefinedParams.BMI, showBMI)

        // 3. Edema - show only if gestational age >= 12 weeks
        val showEdema = gestationalAgeWeeks != null && gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12
        updateFieldVisibility(AssessmentDefinedParams.EDEMA, showEdema)

        // 4. Fundal Height - show only if gestational age >= 24 weeks
        val showFundalHeight = gestationalAgeWeeks != null && gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_24
        updateFieldVisibility(AssessmentDefinedParams.FUNDAL_HEIGHT, showFundalHeight)

        // 5. Folic Acid Total Consumed - show only if gestational age <= 12 weeks
        val showFolicAcid = gestationalAgeWeeks == null || gestationalAgeWeeks <= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12
        updateFieldVisibility(AssessmentDefinedParams.FOLIC_ACID_TABLETS, showFolicAcid)
        updateFieldVisibility(AssessmentDefinedParams.FOLIC_ACID_TOTAL_CONSUMED, showFolicAcid)
        updateFieldVisibility(AssessmentDefinedParams.FOLIC_ACID_PROVIDED, showFolicAcid)

        // 6. IFA/Iron Total Consumed - show only if gestational age > 12 weeks
        val showIFA = gestationalAgeWeeks == null || gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12
        updateFieldVisibility(AssessmentDefinedParams.IFA_TABLETS, showIFA)
        updateFieldVisibility(AssessmentDefinedParams.IFA_TOTAL_CONSUMED, showIFA)
        updateFieldVisibility(AssessmentDefinedParams.IFA_PROVIDED, showIFA)

        // 7. Calcium Total Consumed - show only if gestational age > 12 weeks
        val showCalcium = gestationalAgeWeeks == null || gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12
        updateFieldVisibility(AssessmentDefinedParams.CALCIUM_TABLETS, showCalcium)
        updateFieldVisibility(AssessmentDefinedParams.CALCIUM_TOTAL_CONSUMED, showCalcium)
        updateFieldVisibility(AssessmentDefinedParams.CALCIUM_PROVIDED, showCalcium)

        // 8. Danger Signs - show based on gestational age ranges
        // Show dangerSignsExperienced12 if gestational age <= 12 weeks
        val showDangerSigns12 = gestationalAgeWeeks == null || gestationalAgeWeeks <= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_12
        updateFieldVisibility(AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_12, showDangerSigns12)

        // Show dangerSignsExperienced13To27 if gestational age >= 13 AND <= 27 weeks
        val showDangerSigns1327 =
            gestationalAgeWeeks != null &&
                gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_13 &&
                gestationalAgeWeeks <= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_27
        updateFieldVisibility(AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_13_27, showDangerSigns1327)

        // Show dangerSignsExperienced28To40 if gestational age >= 28 AND <= 40 weeks
        val showDangerSigns2840 =
            gestationalAgeWeeks != null &&
                gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_28 &&
                gestationalAgeWeeks <= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_40
        updateFieldVisibility(AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_28_40, showDangerSigns2840)

        updateFieldVisibility(
            AssessmentDefinedParams.GROUP_DANGER_SIGNS_RISK_IDENTIFICATION,
            showDangerSigns12 || showDangerSigns1327 || showDangerSigns2840,
        )

        // 9. ANC Services and Birth Preparedness - show only if gestational age >= 28 weeks
        val showANCServices = gestationalAgeWeeks != null && gestationalAgeWeeks >= AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_28
        updateFieldVisibility(AssessmentDefinedParams.ULTRASOUND, showANCServices)
        updateFieldVisibility(AssessmentDefinedParams.ANC_FROM_MEDICAL_DOCTOR, showANCServices)
    }

    /**
     * Helper method to show or hide a field by its ID
     */
    private fun updateFieldVisibility(
        fieldId: String,
        shouldShow: Boolean,
    ) {
        val rootSuffix = formGenerator.rootSuffix
        val view = formGenerator.getViewByTag(fieldId + rootSuffix)
        if (shouldShow) {
            view?.visible()
        } else {
            view?.gone()
        }
    }

    /**
     * ANC : Evaluates status condition for a field and returns status text if condition matches
     * Returns Pair<statusText, statusTextCulture> or null if no condition matches
     */
    private fun evaluateAncFieldStatus(
        fieldId: String,
        value: Any?,
    ): Pair<String?, String?>? {
        val resultMap = formGenerator.getResultMap()

        return when (fieldId) {
            AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT -> evaluatePregnantWomanOnTreatmentStatus(value, resultMap)
            AssessmentDefinedParams.SYSTOLIC -> evaluateSystolicStatus(value)
            AssessmentDefinedParams.DIASTOLIC -> evaluateDiastolicStatus(value)
            AssessmentDefinedParams.EDEMA -> evaluateEdemaStatus(value, resultMap)
            AssessmentDefinedParams.TEMPERATURE -> evaluateTemperatureStatus(value)
            AssessmentDefinedParams.PULSE -> evaluatePulseStatus(value)
            AssessmentDefinedParams.FUNDAL_HEIGHT -> ANCAssessmentEvaluator.evaluateFundalHeightStatus(value, calculateGestationalAgeInWeeks())
            AssessmentDefinedParams.HEMOGLOBIN -> evaluateHemoglobinStatus(value)
            AssessmentDefinedParams.URINARY_ALBUMIN -> evaluateUrinaryAlbuminStatus(value, resultMap)
            AssessmentDefinedParams.URINARY_SUGAR -> evaluateUrinarySugarStatus(value)
            AssessmentDefinedParams.URINARY_BILIRUBIN -> evaluateUrinaryBilirubinStatus(value)
            AssessmentDefinedParams.FOLIC_ACID_TABLETS -> evaluateFolicAcidStatus(resultMap)
            AssessmentDefinedParams.IFA_TABLETS -> evaluateIFAStatus(resultMap)
            AssessmentDefinedParams.CALCIUM_TABLETS -> evaluateCalciumStatus(resultMap)
            AssessmentDefinedParams.ANC_FROM_MEDICAL_DOCTOR -> evaluateANCFromMedicalDoctorStatus(value)
            AssessmentDefinedParams.ULTRASOUND -> evaluateUltrasoundStatus(value)
            AssessmentDefinedParams.BLOOD_SUGAR_FASTING -> evaluateBloodSugarFastingStatus(value)
            AssessmentDefinedParams.BLOOD_SUGAR_RANDOM -> evaluateBloodSugarRandomStatus(value)
            AssessmentDefinedParams.FACILITY_IDENTIFIED_FOR_DELIVERY -> evaluateFacilityIdentifiedForDeliveryStatus(value)
            AssessmentDefinedParams.HEIGHT -> evaluateHeightStatus(value)
            AssessmentDefinedParams.WEIGHT -> evaluateWeightStatus(value)
            else -> null
        }
    }

    private fun evaluateAncPreviousValue(
        fieldId: String,
        value: Any?,
    ): String? =
        when (fieldId) {
            AssessmentDefinedParams.WEIGHT -> {
                val currentValue = CommonUtils.getDoubleOrNull(value)
                val previousValue = CommonUtils.getDoubleOrNull(viewModel.previousAssessment?.observations?.weight)
                if (previousValue == null || currentValue == null) {
                    null
                } else if (currentValue < previousValue) {
                    previousValue.toString()
                } else {
                    null
                }
            }
            AssessmentDefinedParams.HEMOGLOBIN -> {
                val currentValue = CommonUtils.getDoubleOrNull(value)
                val previousValue = CommonUtils.getDoubleOrNull(viewModel.previousAssessment?.observations?.hemoglobin)
                if (previousValue == null || currentValue == null) {
                    null
                } else if (currentValue < previousValue) {
                    previousValue.toString()
                } else {
                    null
                }
            }
            AssessmentDefinedParams.FUNDAL_HEIGHT -> {
                val currentValue = CommonUtils.getDoubleOrNull(value)
                val previousValue = CommonUtils.getDoubleOrNull(viewModel.previousAssessment?.observations?.fundalHeight)
                if (previousValue == null || currentValue == null) {
                    null
                } else if (currentValue < previousValue) {
                    previousValue.toString()
                } else {
                    null
                }
            }

            else -> null
        }

    /**
     * PNC : Evaluates status condition for a field and returns status text if condition matches
     * Returns Pair<statusText, statusTextCulture> or null if no condition matches
     */
    private fun evaluatePncFieldStatus(
        fieldId: String,
        value: Any?,
    ): Pair<String?, String?>? =
        when (fieldId) {
            RMNCH.ID_IFA_TABLETS -> {
                evaluateIfaTabletsStatus()
            }

            RMNCH.ID_CALCIUM_TABLETS -> {
                evaluateCalciumTabletStatus()
            }

            RMNCH.ID_VITAMIN_A_CONSUMED -> {
                if (isValueEquals(value, DefinedParams.YES)) {
                    null
                } else {
                    AssessmentDefinedParams.STATUS_GAP to AssessmentDefinedParams.BN_STATUS_GAP
                }
            }

            RMNCH.ID_FAMILY_PLANNING_METHODS -> {
                if (isValueEquals(value, DefinedParams.None)) {
                    AssessmentDefinedParams.STATUS_GAP to AssessmentDefinedParams.BN_STATUS_GAP
                } else {
                    null
                }
            }

            RMNCH.ID_TEMPERATURE -> {
                evaluateTemperatureStatus(value)
            }

            RMNCH.ID_URINARY_BILIRUBIN -> {
                evaluateUrinaryBilirubinStatus(value)
            }

            RMNCH.ID_PULSE -> {
                evaluatePulseStatus(value)
            }

            RMNCH.ID_POSTPARTUM_DANGER_SIGNS -> {
                evaluatePostpartumDangerSigns(value)
            }

            RMNCH.ID_HEMOGLOBIN -> {
                CommonUtils.getDouble(value).takeIf { it > 0 }?.let {
                    evaluateHemoglobinStatus(it)
                }
            }

            RMNCH.ID_ECLAMPSIA,
            RMNCH.ID_KNOWN_HTN,
            RMNCH.ID_GDM_PATIENT,
            RMNCH.ID_DM_PATIENT,
            -> {
                if (isValueEquals(value, DefinedParams.YES)) {
                    AssessmentDefinedParams.STATUS_HIGH_RISK to AssessmentDefinedParams.BN_STATUS_HIGH_RISK
                } else {
                    null
                }
            }

            else -> null
        }

    private fun evaluatePncPreviousValue(
        fieldId: String,
        value: Any?,
    ): String? =
        when (fieldId) {
            RMNCH.ID_HEMOGLOBIN -> {
                val currentValue = CommonUtils.getDoubleOrNull(value)
                val previousValue = CommonUtils.getDoubleOrNull(viewModel.previousAssessment?.observations?.hemoglobin)
                if (previousValue == null || currentValue == null) {
                    null
                } else if (currentValue < previousValue) {
                    previousValue.toString()
                } else {
                    null
                }
            }

            else -> null
        }

    /**
     * Evaluates Postpartum Danger Signs, if danger signs are there than high risk
     */
    private fun evaluatePostpartumDangerSigns(value: Any?): Pair<String, String?>? {
        val dangerSigns = value as? List<*>
        val selectedSigns = dangerSigns?.filterIsInstance<Map<String, Any>>()?.mapNotNull { it[DefinedParams.Value] as? String }
        // Since we are checking with none, it is safe to take first element from the list
        return if (dangerSigns.isNullOrEmpty() || isValueEquals(selectedSigns?.firstOrNull(), DefinedParams.None)) {
            null
        } else {
            AssessmentDefinedParams.STATUS_HIGH_RISK to AssessmentDefinedParams.BN_STATUS_HIGH_RISK
        }
    }

    /**
     * Evaluates calcium tablets status, if consumption is less than (days since delivery i.e, from next day of delivery) then gap
     */
    private fun evaluateCalciumTabletStatus(): Pair<String, String?>? {
        val daysSinceDelivery = getDaysSinceDelivery()
        return daysSinceDelivery?.let {
            val resultMap = formGenerator.getResultMap()
            val calciumConsumed = CommonUtils.getInteger(resultMap[RMNCH.ID_CALCIUM_TABLETS_CONSUMED])
            if (calciumConsumed < daysSinceDelivery) {
                AssessmentDefinedParams.STATUS_GAP to AssessmentDefinedParams.BN_STATUS_GAP
            } else {
                null
            }
        } ?: run {
            null
        }
    }

    /**
     * Evaluates ifa tablets status, if consumption is less than (days since delivery i.e, from next day of delivery) then gap
     */
    private fun evaluateIfaTabletsStatus(): Pair<String, String?>? {
        val daysSinceDelivery = getDaysSinceDelivery()
        return daysSinceDelivery?.let {
            val resultMap = formGenerator.getResultMap()
            val ifaConsumed = CommonUtils.getInteger(resultMap[RMNCH.ID_IFA_TABLETS_CONSUMED])
            if (ifaConsumed < daysSinceDelivery) {
                AssessmentDefinedParams.STATUS_GAP to AssessmentDefinedParams.BN_STATUS_GAP
            } else {
                null
            }
        } ?: run {
            null
        }
    }

    /**
     * 1. Pregnant woman on treatment for any existing illness - if count of selected options < total available options
     */
    private fun evaluatePregnantWomanOnTreatmentStatus(
        value: Any?,
        resultMap: HashMap<String, Any>,
    ): Pair<String?, String?>? {
        // If user hasn't selected any treatment already, then ignore highlighting
        if (!ancSelectedTreatment) return null
        val treatmentOptions = value as? ArrayList<HashMap<String, Any>> ?: arrayListOf()
        val existingIllnessOptions = resultMap[AssessmentDefinedParams.PREGNANT_WOMAN_EXISTING_ILLNESS] as? ArrayList<HashMap<String, Any>> ?: return null

        // Filter out "none" option from existing illness options for count calculation
        val filteredExistingIllnessOptions = existingIllnessOptions
            .filterNot { illnessItem ->
                val value = illnessItem[DefinedParams.Value]?.toString()
                DefinedParams.None.equals(value, true)
            }.mapNotNull { illnessItem ->
                illnessItem[DefinedParams.Value]?.toString()
            }.toSet()

        // Filter out "none" option from treatment options for count calculation
        val filteredIllnessTreatmentOptions = treatmentOptions
            .filterNot { treatmentItem ->
                val value = treatmentItem[DefinedParams.Value]?.toString()
                DefinedParams.None.equals(value, true)
            }.mapNotNull { treatmentItem ->
                treatmentItem[DefinedParams.Value]?.toString()
            }.toSet()

        return if (filteredExistingIllnessOptions.isNotEmpty() && filteredExistingIllnessOptions.minus(filteredIllnessTreatmentOptions).isNotEmpty()) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 2. Systolic - High Risk if >= 140
     */
    private fun evaluateSystolicStatus(value: Any?): Pair<String?, String?>? {
        val systolic = CommonUtils.getDoubleOrNull(value) ?: return null
        if (systolic == 0.0) return null
        return if (systolic >= AssessmentDefinedParams.HIGH_BP_SYSTOLIC_THRESHOLD || systolic <= AssessmentDefinedParams.LOW_BP_SYSTOLIC_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 2. Diastolic - High Risk if >= 90
     */
    private fun evaluateDiastolicStatus(value: Any?): Pair<String?, String?>? {
        val diastolic = CommonUtils.getDoubleOrNull(value) ?: return null
        if (diastolic == 0.0) return null
        return if (diastolic >= AssessmentDefinedParams.HIGH_BP_DIASTOLIC_THRESHOLD || diastolic <= AssessmentDefinedParams.LOW_BP_DIASTOLIC_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 3. Edema - (edema=present AND BP >= 140/90) OR (urine albumin = present)
     */
    private fun evaluateEdemaStatus(
        value: Any?,
        resultMap: HashMap<String, Any>,
    ): Pair<String?, String?>? {
        val edemaValue = value as? String
        if (edemaValue != AssessmentDefinedParams.VALUE_PRESENT) return null

        // Check BP from individual systolic and diastolic fields
        val systolic = (resultMap[AssessmentDefinedParams.SYSTOLIC] as? String)?.toDouble() ?: 0.0
        val diastolic = (resultMap[AssessmentDefinedParams.DIASTOLIC] as? String)?.toDouble() ?: 0.0
        val isHighBP = systolic >= AssessmentDefinedParams.HIGH_BP_SYSTOLIC_THRESHOLD || diastolic >= AssessmentDefinedParams.HIGH_BP_DIASTOLIC_THRESHOLD

        // Check urinary albumin
        val urinaryAlbumin = resultMap[AssessmentDefinedParams.URINARY_ALBUMIN] as? String

        val condition2 = urinaryAlbumin == AssessmentDefinedParams.VALUE_PRESENT

        return if (isHighBP || condition2) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 4. Temperature - "High Risk - Fever" 100-101.9 F, "High Risk - High Fever" >= 102F
     */
    private fun evaluateTemperatureStatus(value: Any?): Pair<String?, String?>? {
        val temp = (value as? Number)?.toDouble() ?: return null
        if (temp == 0.0) return null

        return when {
            temp >= AssessmentDefinedParams.TEMP_HIGH_FEVER_THRESHOLD -> Pair(
                AssessmentDefinedParams.STATUS_HIGH_FEVER,
                AssessmentDefinedParams.BN_STATUS_HIGH_FEVER,
            )

            temp >= AssessmentDefinedParams.TEMP_FEVER_MIN_THRESHOLD && temp <= AssessmentDefinedParams.TEMP_FEVER_MAX_THRESHOLD -> Pair(
                AssessmentDefinedParams.STATUS_FEVER,
                AssessmentDefinedParams.BN_STATUS_FEVER,
            )

            else -> null
        }
    }

    /**
     * 5. Pulse - "abnormal" >90 or <60
     */
    private fun evaluatePulseStatus(value: Any?): Pair<String?, String?>? {
        val pulse = (value as? Number)?.toDouble() ?: return null
        if (pulse == 0.0) return null

        return if (pulse > AssessmentDefinedParams.PULSE_HIGH_THRESHOLD || pulse < AssessmentDefinedParams.PULSE_LOW_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_ABNORMAL, AssessmentDefinedParams.BN_STATUS_ABNORMAL)
        } else {
            null
        }
    }

    /**
     * 7. Hemoglobin - "Moderate Anemia" if Hb<10, "Severe Anemia" if Hb <8
     */
    private fun evaluateHemoglobinStatus(value: Any?): Pair<String?, String?>? {
        val hb = (value as? Number)?.toDouble() ?: return null
        if (hb == 0.0) return null

        return when {
            hb < AssessmentDefinedParams.HEMOGLOBIN_SEVERE_ANEMIA_THRESHOLD -> Pair(
                AssessmentDefinedParams.STATUS_SEVERE_ANEMIA,
                AssessmentDefinedParams.BN_STATUS_SEVERE_ANEMIA,
            )

            hb < AssessmentDefinedParams.HEMOGLOBIN_MODERATE_ANEMIA_THRESHOLD -> Pair(
                AssessmentDefinedParams.STATUS_MODERATE_ANEMIA,
                AssessmentDefinedParams.BN_STATUS_MODERATE_ANEMIA,
            )

            hb < AssessmentDefinedParams.HEMOGLOBIN_MILD_ANEMIA_THRESHOLD -> Pair(
                AssessmentDefinedParams.STATUS_MILD_ANEMIA,
                AssessmentDefinedParams.BN_STATUS_MILD_ANEMIA,
            )

            else -> null
        }
    }

    /**
     * 8. Urinary Albumin - Positive AND (BP≥140/90 OR edema=present)
     */
    private fun evaluateUrinaryAlbuminStatus(
        value: Any?,
        resultMap: HashMap<String, Any>,
    ): Pair<String?, String?>? {
        val urinaryAlbumin = value as? String
        if (urinaryAlbumin != AssessmentDefinedParams.VALUE_PRESENT) return null

        // Check BP from individual systolic and diastolic fields
        val systolic = (resultMap[AssessmentDefinedParams.SYSTOLIC] as? String)?.toDouble() ?: 0.0
        val diastolic = (resultMap[AssessmentDefinedParams.DIASTOLIC] as? String)?.toDouble() ?: 0.0
        val isHighBP = systolic >= AssessmentDefinedParams.HIGH_BP_SYSTOLIC_THRESHOLD || diastolic >= AssessmentDefinedParams.HIGH_BP_DIASTOLIC_THRESHOLD

        // Check edema
        val edema = resultMap[AssessmentDefinedParams.EDEMA] as? String
        val condition2 = edema == AssessmentDefinedParams.VALUE_PRESENT

        return if (isHighBP || condition2) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 9. Urinary Sugar - High risk if present
     */
    private fun evaluateUrinarySugarStatus(value: Any?): Pair<String?, String?>? {
        val urinarySugar = value as? String
        return if (urinarySugar == AssessmentDefinedParams.VALUE_PRESENT) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 10. Urinary Bilirubin - High risk if present
     */
    private fun evaluateUrinaryBilirubinStatus(value: Any?): Pair<String?, String?>? {
        val urinaryBilirubin = value as? String
        return if (urinaryBilirubin == AssessmentDefinedParams.VALUE_PRESENT) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * 11. Folic Acid Tablets - "gap" if <30 tabs consumed
     */
    private fun evaluateFolicAcidStatus(resultMap: HashMap<String, Any>): Pair<String?, String?>? {
        val consumed = (resultMap[AssessmentDefinedParams.FOLIC_ACID_TOTAL_CONSUMED] as String?)?.toIntOrNull()

        return if (consumed != null && consumed < AssessmentDefinedParams.TABLET_CONSUMPTION_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_GAP, AssessmentDefinedParams.BN_STATUS_GAP)
        } else {
            null
        }
    }

    /**
     * 12. IFA Tablets - "gap" if <30 tabs consumed
     */
    private fun evaluateIFAStatus(resultMap: HashMap<String, Any>): Pair<String?, String?>? {
        val consumed = (resultMap[AssessmentDefinedParams.IFA_TOTAL_CONSUMED] as String?)?.toIntOrNull()

        return if (consumed != null && consumed < AssessmentDefinedParams.TABLET_CONSUMPTION_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_GAP, AssessmentDefinedParams.BN_STATUS_GAP)
        } else {
            null
        }
    }

    /**
     * 13. Calcium Tablets - "gap" if <30 tabs consumed
     */
    private fun evaluateCalciumStatus(resultMap: HashMap<String, Any>): Pair<String?, String?>? {
        val consumed = (resultMap[AssessmentDefinedParams.CALCIUM_TOTAL_CONSUMED] as String?)?.toIntOrNull()
        return if (consumed != null && consumed < AssessmentDefinedParams.TABLET_CONSUMPTION_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_GAP, AssessmentDefinedParams.BN_STATUS_GAP)
        } else {
            null
        }
    }

    /**
     * 14. ANC from Medical Doctor - "gap" if value is "no" AND gestational age > 36 weeks
     */
    private fun evaluateANCFromMedicalDoctorStatus(value: Any?): Pair<String?, String?>? {
        val ancFromDoctor = value as? String ?: return null
        val gestationalAgeWeeks = calculateGestationalAgeInWeeks()

        return if (ancFromDoctor.equals(AssessmentDefinedParams.VALUE_NO, ignoreCase = true) &&
            gestationalAgeWeeks != null &&
            gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_36
        ) {
            Pair(AssessmentDefinedParams.STATUS_GAP, AssessmentDefinedParams.BN_STATUS_GAP)
        } else {
            null
        }
    }

    /**
     * 15. Ultrasound - "gap" if value is "notDone" AND gestational age > 36 weeks
     */
    private fun evaluateUltrasoundStatus(value: Any?): Pair<String?, String?>? {
        val ultrasound = value as? String ?: return null
        val gestationalAgeWeeks = calculateGestationalAgeInWeeks()

        return if (ultrasound.equals(AssessmentDefinedParams.VALUE_NOT_DONE, ignoreCase = true) &&
            gestationalAgeWeeks != null &&
            gestationalAgeWeeks > AssessmentDefinedParams.GESTATIONAL_AGE_WEEK_36
        ) {
            Pair(AssessmentDefinedParams.STATUS_GAP, AssessmentDefinedParams.BN_STATUS_GAP)
        } else {
            null
        }
    }

    /**
     * Blood Sugar Fasting - High Risk if >= 5.1
     */
    private fun evaluateBloodSugarFastingStatus(value: Any?): Pair<String?, String?>? {
        val fastingValue = (value as? Number)?.toDouble()
        if (fastingValue == null || fastingValue == 0.0) return null

        return if (fastingValue >= AssessmentDefinedParams.BLOOD_SUGAR_FASTING_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    private fun evaluateHeightStatus(value: Any?): Pair<String?, String?>? {
        val height = CommonUtils.getDoubleOrNull(value)
        return if (height != null && height < 145.0) {
            AssessmentDefinedParams.STATUS_HIGH_RISK to
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK
        } else {
            null
        }
    }

    private fun evaluateWeightStatus(value: Any?): Pair<String?, String?>? {
        val weight = CommonUtils.getDoubleOrNull(value) ?: return null

        if (weight == 0.0) return null

        return if (weight < 45.0) {
            Pair(
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            ANCAssessmentEvaluator.evaluateAncWeightStatus(
                value,
                getANCVisitNumber(),
                viewModel.pregnancyDetailLiveData.value,
            )
        }
    }

    /**
     * Blood Sugar Random - High Risk if >= 8.5
     */
    private fun evaluateBloodSugarRandomStatus(value: Any?): Pair<String?, String?>? {
        val randomValue = (value as? Number)?.toDouble()
        if (randomValue == null || randomValue == 0.0) return null
        return if (randomValue >= AssessmentDefinedParams.BLOOD_SUGAR_RANDOM_THRESHOLD) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * Facility Identified For Delivery - High Risk if "Not identified yet" or "Planned for home delivery"
     */
    private fun evaluateFacilityIdentifiedForDeliveryStatus(value: Any?): Pair<String?, String?>? {
        val selectedId = value as? String
        return if (selectedId == AssessmentDefinedParams.FACILITY_NOT_IDENTIFIED ||
            selectedId == AssessmentDefinedParams.FACILITY_HOME_DELIVERY
        ) {
            Pair(AssessmentDefinedParams.STATUS_HIGH_RISK, AssessmentDefinedParams.BN_STATUS_HIGH_RISK)
        } else {
            null
        }
    }

    /**
     * Updates status for all fields that need evaluation
     */
    private fun updateAllFieldStatuses() {
        val resultMap = formGenerator.getResultMap()
        // Fields that always need evaluation (depend on other fields)
        val fieldsToAlwaysCheck = listOf(
            AssessmentDefinedParams.EDEMA,
            AssessmentDefinedParams.URINARY_ALBUMIN,
            AssessmentDefinedParams.FOLIC_ACID_TABLETS,
            AssessmentDefinedParams.IFA_TABLETS,
            AssessmentDefinedParams.CALCIUM_TABLETS,
            AssessmentDefinedParams.ANC_FROM_MEDICAL_DOCTOR,
            AssessmentDefinedParams.ULTRASOUND,
            AssessmentDefinedParams.SYSTOLIC, // Depends on HTN illness status
            AssessmentDefinedParams.DIASTOLIC, // Depends on HTN illness status
            AssessmentDefinedParams.BLOOD_SUGAR_FASTING, // Depends on DM illness status
            AssessmentDefinedParams.BLOOD_SUGAR_RANDOM, // Depends on DM illness status
            AssessmentDefinedParams.PREGNANT_WOMAN_ON_TREATMENT, // Depends on existing illness
        )

        // Fields that only need evaluation if they have a value
        val fieldsToCheckIfValueExists = listOf(
            AssessmentDefinedParams.TEMPERATURE,
            AssessmentDefinedParams.PULSE,
            AssessmentDefinedParams.FUNDAL_HEIGHT,
            AssessmentDefinedParams.HEMOGLOBIN,
            AssessmentDefinedParams.URINARY_SUGAR,
            AssessmentDefinedParams.URINARY_BILIRUBIN,
            AssessmentDefinedParams.FACILITY_IDENTIFIED_FOR_DELIVERY,
        )

        // Always evaluate fields that depend on other fields
        fieldsToAlwaysCheck.forEach { fieldId ->
            handleAncFieldStatusUpdate(fieldId)
        }

        // Evaluate fields only if they have values
        fieldsToCheckIfValueExists.forEach { fieldId ->
            val value = resultMap[fieldId]
            if (value != null) {
                handleAncFieldStatusUpdate(fieldId)
            }
        }
    }

    /**
     * Handles status evaluation and title update for ANC field
     */
    private fun handleAncFieldStatusUpdate(fieldId: String) {
        // TextLabel fields (folicAcidTablets, ifaTablets, calciumTablets) use just 'id' as tag, not 'id + titleSuffix'
        val textLabelFields = listOf(AssessmentDefinedParams.FOLIC_ACID_TABLETS, AssessmentDefinedParams.IFA_TABLETS, AssessmentDefinedParams.CALCIUM_TABLETS)
        handleFieldStatusUpdate(fieldId, textLabelFields, ::evaluateAncFieldStatus, ::evaluateAncPreviousValue)
    }

    /**
     * Handles status evaluation and title update for PNC field
     */
    private fun handlePncFieldStatusUpdate(fieldId: String) {
        val textLabelFields = listOf(RMNCH.ID_IFA_TABLETS, RMNCH.ID_CALCIUM_TABLETS)
        handleFieldStatusUpdate(fieldId, textLabelFields, ::evaluatePncFieldStatus, ::evaluatePncPreviousValue)
    }

    /**
     * Handles status evaluation and title update for PNC BP fields
     */
    private fun handlePncBpHighRiskHighlight() {
        val resultMap = formGenerator.getResultMap()
        val systolic = CommonUtils.getInteger(resultMap[AssessmentDefinedParams.SYSTOLIC])
        val diastolic = CommonUtils.getInteger(resultMap[AssessmentDefinedParams.DIASTOLIC])
        val edema = resultMap[RMNCH.ID_EDEMA] as? String
        val urinaryAlbumin = resultMap[RMNCH.ID_URINARY_ALBUMIN] as? String

        val isHighBp = PNCAssessmentEvaluator.isHighBp(systolic, diastolic)
        val isLowBp = PNCAssessmentEvaluator.isLowBp(systolic, diastolic)
        val isEdemaPresent = isValueEquals(edema)
        val isAlbuminPositive = isValueEquals(urinaryAlbumin)

        // Common high risk triggers for BP (Point 1, 2, 3)
        val bpHighRiskTrigger = isHighBp || (isEdemaPresent && isAlbuminPositive)

        // Systolic
        if (systolic > 0 && (bpHighRiskTrigger || isLowBp)) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.SYSTOLIC,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.SYSTOLIC,
            )
        }

        // Diastolic
        if (diastolic > 0 && (bpHighRiskTrigger || isLowBp)) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.DIASTOLIC,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                AssessmentDefinedParams.DIASTOLIC,
            )
        }

        // Edema (Point 2)
        if (isEdemaPresent && (isHighBp || isAlbuminPositive)) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_EDEMA,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_EDEMA,
            )
        }

        // Urinary Albumin (Point 3)
        if (isAlbuminPositive && (isHighBp || isEdemaPresent)) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_URINARY_ALBUMIN,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_URINARY_ALBUMIN,
            )
        }
    }

    /**
     * Handles status evaluation and title update for PNC Blood Sugar fields
     */
    private fun handlePncBloodSugarHighRiskHighlight() {
        val resultMap = formGenerator.getResultMap()
        val fastingSugar = CommonUtils.getDouble(resultMap[RMNCH.ID_FASTING_BLOOD_SUGAR])
        val randomSugar = CommonUtils.getDouble(resultMap[RMNCH.ID_RANDOM_BLOOD_SUGAR])

        // Fasting (Point 1)
        if (fastingSugar >= 7.0) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_FASTING_BLOOD_SUGAR,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_FASTING_BLOOD_SUGAR,
            )
        }

        // Random (Point 2)
        if (randomSugar >= 11.1) {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_RANDOM_BLOOD_SUGAR,
                AssessmentDefinedParams.STATUS_HIGH_RISK,
                AssessmentDefinedParams.BN_STATUS_HIGH_RISK,
            )
        } else {
            AssessmentUtil.updateFieldTitleWithStatus(
                formGenerator,
                originalTitles,
                isTranslationEnabled,
                RMNCH.ID_RANDOM_BLOOD_SUGAR,
            )
        }
    }

    /**
     * Handles status evaluation and title update for a field
     */
    private fun handleFieldStatusUpdate(
        fieldId: String,
        textLabelFieldIds: List<String>,
        evaluateStatus: (
            fieldId: String,
            value: Any?,
        ) -> Pair<String?, String?>?,
        evaluatePreviousValue: (fieldId: String, value: Any?) -> String?,
    ) {
        val resultMap = formGenerator.getResultMap()
        val value = resultMap[fieldId]

        val status = evaluateStatus(fieldId, value)
        val previousValue = evaluatePreviousValue(fieldId, value)
        AssessmentUtil.updateFieldTitleWithStatus(
            formGenerator,
            originalTitles,
            isTranslationEnabled,
            fieldId,
            status?.first,
            status?.second,
            textLabelFieldIds,
            previousValue,
        )
    }

    /**
     * Evaluate highRiskPregnantWoman and gapsInAnc and add to result map
     */
    private fun evaluateAndAddAncSummaryData(resultMap: HashMap<String, Any>) {
        val ancHashMap = resultMap[RMNCH.ANC] as? HashMap<String, Any> ?: return

        // Store anc visit date
        ancHashMap[AssessmentDefinedParams.ANC_VISIT_DATE] = DateUtils.getDateString(
            Calendar.getInstance().timeInMillis,
            inputFormat = DateUtils.DATE_FORMAT_yyyyMMdd,
            outputFormat = DATE_FORMAT_yyyyMMddHHmmssZZZZZ,
        )

        val summaryGroup = (ancHashMap[AssessmentDefinedParams.GROUP_SUMMARY] as? HashMap<String, Any>)
            ?: HashMap<String, Any>().also {
                ancHashMap[AssessmentDefinedParams.GROUP_SUMMARY] = it
            }

        // Evaluate highRiskPregnantWoman
        val (dangerSignsList, hasOtherSelected) = getDangerSignsValues(ancHashMap)
        val emergencyConditions = ANCAssessmentEvaluator
            .evaluateEmergencyReferralConditions(
                ancHashMap,
                getANCVisitNumber(),
                viewModel.pregnancyDetailLiveData.value,
                calculateGestationalAgeInWeeks(),
            ).toMutableList()
        val nonEmergencyConditions = ANCAssessmentEvaluator.evaluateNonEmergencyReferralConditions(
            ancHashMap,
            viewModel.memberDetailsLiveData.value
                ?.data
                ?.dateOfBirth,
            hasOtherSelected,
            viewModel.pregnancyDetailLiveData.value,
        )

        val highRiskMap = hashMapOf<String, Any>()
        dangerSignsList.forEach { dangerSign ->
            emergencyConditions.add(dangerSign)
        }
        // Combine all high risk conditions
        if (emergencyConditions.isNotEmpty()) {
            highRiskMap[AncPncReferralType.URGENT.name] = emergencyConditions
        }
        if (nonEmergencyConditions.isNotEmpty()) {
            highRiskMap[AncPncReferralType.NON_URGENT.name] = nonEmergencyConditions
        }

        // Add to summary group
        if (highRiskMap.isNotEmpty()) {
            summaryGroup[AssessmentDefinedParams.HIGH_RISK_PREGNANT_WOMAN] = highRiskMap
        }

        // Evaluate gapsInAnc
        val gapsList =
            ANCAssessmentEvaluator.evaluateGapsInANC(ancHashMap, calculateGestationalAgeInWeeks(), viewModel.pregnancyDetailLiveData.value, formGenerator)
        if (gapsList.isNotEmpty()) {
            summaryGroup[AssessmentDefinedParams.GAPS_IN_ANC] = gapsList
        }
    }

    /**
     * Get danger signs values from all three danger signs fields
     * Filters out "None" and Other option from the results
     * @return Pair of (filtered danger signs list, boolean indicating if "other" option is selected)
     */
    private fun getDangerSignsValues(map: HashMap<String, Any>): Pair<List<String>, Boolean> {
        val dangerSignsList = mutableListOf<String>()
        var hasOtherSelected = false
        val ancMap = map[AssessmentDefinedParams.GROUP_DANGER_SIGNS_RISK_IDENTIFICATION] as? Map<*, *>
        ancMap?.let { anc ->
            // Check all three danger signs fields
            listOf(
                AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_12,
                AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_13_27,
                AssessmentDefinedParams.DANGER_SIGNS_EXPERIENCED_28_40,
            ).forEach { dangerSignId ->
                val dangerSignValue = anc[dangerSignId]
                if (dangerSignValue != null) {
                    // Check raw value for "other" option before formatting
                    if (dangerSignValue is ArrayList<*>) {
                        dangerSignValue.forEach { item ->
                            if (item is Map<*, *>) {
                                val value = item[DefinedParams.Value]?.toString()
                                val name = item[DefinedParams.NAME]?.toString() ?: ""
                                val culturalValue = item[DefinedParams.CULTURE_VALUE]?.toString() ?: ""
                                // Check if "other" option is selected, then don't add other to the list
                                if (DefinedParams.OTHER.equals(value, true) ||
                                    DefinedParams.OTHER.equals(name, true)
                                ) {
                                    hasOtherSelected = true
                                } else if (!(
                                        DefinedParams.None.equals(value, true) ||
                                            DefinedParams.None.equals(name, true)
                                    )
                                ) {
                                    // Check if none option is selected, then don't add to the list
                                    dangerSignsList.add("$name::$culturalValue")
                                }
                            }
                        }
                    }
                }
            }
        }
        return Pair(dangerSignsList, hasOtherSelected)
    }

    /**
     * Gets the current ANC visit number.
     * Visit number is calculated as visitCount + 1 (visitCount is 0-based)
     */
    private fun getANCVisitNumber(): Int {
        val visitCount = viewModel.pregnancyDetailLiveData.value?.ancVisitNo ?: 0L
        return (visitCount + 1).toInt()
    }

    /**
     * Returns pnc visit count from the DB plus 1 to count current visit
     */
    private fun getPncVisitNumber(): Int = (viewModel.pregnancyDetailLiveData.value?.pncVisitNo ?: 0L).toInt() + 1

    /**
     * Returns child visit number from the DB + 1 to count current visit
     */
    private fun getChildVisitNumber(): Int = (viewModel.pregnancyDetailLiveData.value?.childVisitNo ?: 0L).toInt() + 1

    /**
     * Returns anc visit count
     */
    private fun getAncVisitNumberForPnc(): Int = (viewModel.pregnancyDetailLiveData.value?.ancVisitNo ?: 0L).toInt()

    /**
     * Calculates days since delivery from the delivery date
     */
    private fun getDaysSinceDelivery(): Int? =
        viewModel.pregnancyDetailLiveData.value?.dateOfDelivery?.let { clinicalDate ->
            DateUtils.parseDate(clinicalDate)?.let { date ->
                DateUtils.getDaysDifference(date.getLongTime())
            }
        }
}
