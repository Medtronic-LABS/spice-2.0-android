package com.medtroniclabs.opensource.ncd.medicalreview.dialog

import android.content.res.Configuration
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.viewModels
import com.google.gson.Gson
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.invisible
import com.medtroniclabs.opensource.appextensions.loadAsGif
import com.medtroniclabs.opensource.appextensions.resetImageView
import com.medtroniclabs.opensource.appextensions.setDialogPercent
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.CommonUtils.calculateCAGEAIDSCore
import com.medtroniclabs.opensource.common.CommonUtils.calculateSuicidalIdeation
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.FormAutofill
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.databinding.FragmentNCDMentalHealthQuestionDialogBinding
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.NCDMentalHealthMedicalReviewDetails
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDMentalHealthViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import com.medtroniclabs.opensource.ui.assessment.viewmodel.AssessmentViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class NCDMentalHealthQuestionDialog(private val callback: ((successDialog: Pair<String, String>?, errorResponse: String?) -> Unit)) :
    DialogFragment(), FormEventListener, View.OnClickListener {
    private lateinit var binding: FragmentNCDMentalHealthQuestionDialogBinding
    private lateinit var formGenerator: FormGenerator

    private val ncdFormViewModel: NCDFormViewModel by viewModels()
    private val assessmentViewModel: AssessmentViewModel by viewModels()
    private val viewModel: NCDMentalHealthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNCDMentalHealthQuestionDialogBinding.inflate(inflater, container, false)
        dialog?.window?.setBackgroundDrawableResource(android.R.color.transparent)
        isCancelable = false
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initializeFormGenerator()
        attachObserver()
    }

    override fun onStart() {
        super.onStart()
        handleOrientation()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        handleOrientation()
    }

    private fun handleOrientation() {
        val isTablet = CommonUtils.checkIsTablet(requireContext())
        val isLandscape = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val width = when {
            isTablet && isLandscape -> 75
            else -> 100
        }
        val height = when {
            isTablet && isLandscape -> 95
            else -> 100
        }
        setDialogPercent(width, height)
    }

    private fun initializeFormGenerator() {
        binding.btnCancel.safeClickListener(this@NCDMentalHealthQuestionDialog)
        binding.btnConfirm.safeClickListener(this@NCDMentalHealthQuestionDialog)
        binding.ivClose.safeClickListener(this@NCDMentalHealthQuestionDialog)
        binding.loadingProgress.safeClickListener(this@NCDMentalHealthQuestionDialog)

        formGenerator = FormGenerator(
            requireContext(),
            binding.llForm,
            listener = this,
            scrollView = binding.mentalHealthScrollView,
            translate = SecuredPreference.getIsTranslationEnabled()
        )

        fetchForms()

        binding.tvMentalHealthLabel.text = getTitle(myFormType())
    }

    private fun fetchForms() {
        if (screeningCards().contains(myFormType()))
            ncdFormViewModel.getNCDForm(DefinedParams.Screening)
        else
            ncdFormViewModel.getNCDForm(
                DefinedParams.Assessment,
                workFlow = NCDMRUtil.mentalHealth
            )
    }

    private fun myFormType(): String? {
        return requireArguments().getString(Screening.type)
    }

    private fun assessmentCards(): List<String> {
        return listOf(
            AssessmentDefinedParams.phq4,
            AssessmentDefinedParams.phq9,
            AssessmentDefinedParams.gad7
        )
    }

    private fun screeningCards(): List<String> {
        return listOf(AssessmentDefinedParams.suicidalIdeation, AssessmentDefinedParams.cageAid)
    }

    private fun getTitle(type: String?): String {
        return when (type) {
            AssessmentDefinedParams.phq4 -> getString(R.string.phq4_assessment)
            AssessmentDefinedParams.phq9 -> getString(R.string.phq9_assessment)
            AssessmentDefinedParams.gad7 -> getString(R.string.gad7_assessment)
            AssessmentDefinedParams.suicidalIdeation -> getString(R.string.suicide_screener)
            AssessmentDefinedParams.cageAid -> getString(R.string.cage_aid)
            else -> getString(R.string.assessment)
        }
    }

    private fun attachObserver() {
        ncdFormViewModel.ncdFormResponse.observe(viewLifecycleOwner) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resources.data?.let { it ->
                        it.filter {
                            myFormType()?.let { type -> it.family?.contains(type) } == true
                        }.let { json ->
                            if (json.isNotEmpty())
                                formGenerator.populateViews(json)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        assessmentViewModel.mentalHealthQuestions.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { localMHResponse ->
                        localMHResponse.forEach {
                            formGenerator.loadMentalHealthQuestions(it.value)
                        }
                    }
                }
            }
        }
        viewModel.mentalHealthDetails.observe(viewLifecycleOwner) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    dismiss()
                    callback.invoke(null, resourceState.message)
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { data ->
                        val prefill = HashMap<String, Any>()

                        if (isAssessment()) {
                            data[Screening.MentalHealthDetails]?.let {
                                viewModel.questionarieId =
                                    data[NCDMRUtil.questionnaireId] as? String
                                viewModel.encounterId = data[DefinedParams.EncounterId] as? String

                                prefill.put(getFormattedKey(), it)
                            }
                        } else {
                            (data[AssessmentDefinedParams.encounter] as? Map<String, Any>)?.let {
                                viewModel.encounterId = it[AssessmentDefinedParams.id] as? String
                            }

                            (data[Screening.suicideScreener] as? Map<String, Any>)?.let {
                                prefill.putAll(
                                    modifiedPrefills(it)
                                )
                            }

                            (data[Screening.substanceAbuse] as? Map<String, Any>)?.let {
                                prefill.putAll(
                                    modifiedPrefills(it)
                                )
                            }
                        }

                        if (prefill.isNotEmpty())
                            FormAutofill.start(requireContext(), formGenerator, prefill)
                    }
                }
            }
        }
        viewModel.assessmentMentalHealth.observe(viewLifecycleOwner) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    dismiss()
                    callback.invoke(null, resource.message)
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    dismiss()
                    callback.invoke(
                        Pair(
                            getString(R.string.tab_medical_review),
                            resource.data ?: getString(R.string.mental_health_success)
                        ), null
                    )
                }
            }
        }
    }

    private fun modifiedPrefills(map: Map<String, Any>): HashMap<String, Any> {
        viewModel.observationId = map[AssessmentDefinedParams.ObservationID] as? String

        val returnMap = HashMap<String, Any>(map)
        returnMap.remove(AssessmentDefinedParams.ObservationID)
        return returnMap
    }

    private fun getFormattedKey(): String {
        return when (myFormType()) {
            AssessmentDefinedParams.phq9 -> AssessmentDefinedParams.PHQ9_Mental_Health
            AssessmentDefinedParams.gad7 -> AssessmentDefinedParams.GAD7_Mental_Health
            else -> Screening.MentalHealthDetails
        }
    }

    private fun callDetailsAPI() {
        val isAssessment = isAssessment()
        val request = NCDMentalHealthMedicalReviewDetails(
            memberReference = requireArguments().getString(NCDMRUtil.MEMBER_REFERENCE) as String,
            type = if (isAssessment) myFormType()?.uppercase() else myFormType()
        )
        if (requireArguments().getBoolean(EditAssessment))
            viewModel.ncdMentalHealthMedicalReviewDetails(request, isAssessment)
    }

    companion object {
        const val TAG = "NCDMentalHealthQuestionDialog"
        const val EditAssessment = "EditAssessment"
        fun newInstance(
            type: String,
            patientId: String?,
            patientFHIRId: String?,
            isEditAssessment: Boolean,
            callback: ((successDialog: Pair<String, String>?, errorResponse: String?) -> Unit)
        ): NCDMentalHealthQuestionDialog {
            val fragment = NCDMentalHealthQuestionDialog(callback)
            val args = Bundle()
            args.putString(Screening.type, type)
            args.putString(NCDMRUtil.PATIENT_REFERENCE, patientId)
            args.putString(NCDMRUtil.MEMBER_REFERENCE, patientFHIRId)
            args.putBoolean(EditAssessment, isEditAssessment)
            fragment.arguments = args
            return fragment
        }
    }

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {
        if (localDataCache is String) {
            when (localDataCache) {
                Screening.PHQ4, AssessmentDefinedParams.PHQ9, AssessmentDefinedParams.GAD7 -> {
                    assessmentViewModel.fetchMentalHealthQuestions(localDataCache)
                }

                AssessmentDefinedParams.Fetch_MH_Questions -> {
                    formGenerator.fetchMHQuestions(
                        id,
                        assessmentViewModel.mentalHealthQuestions.value?.data?.get(id)
                    )
                }
            }
        }
    }

    override fun onPopulate(targetId: String) {

    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        serverViewModel: FormLayout,
        resultMap: Any?
    ) {

    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {

    }

    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {

    }

    override fun onRenderingComplete() {
        callDetailsAPI()
    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {

    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?
    ) {

    }

    override fun onAgeCheckForPregnancy() {

    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {

    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {

    }

    fun showLoading() {
        binding.apply {
            btnConfirm.invisible()
            btnCancel.invisible()
            loadingProgress.visible()
            loaderImage.apply {
                loadAsGif(R.drawable.loader_spice)
            }
        }
    }

    fun hideLoading() {
        binding.apply {
            btnConfirm.visible()
            btnCancel.visible()
            loadingProgress.gone()
            loaderImage.apply {
                resetImageView()
            }
        }
    }

    private fun processValuesAndProceed(
        resultHashMap: HashMap<String, Any>,
        serverData: List<FormLayout?>?
    ) {
        val map = HashMap<String, Any>()
        map.putAll(resultHashMap)

        //Patient ID
        getPatientReference()?.let { patientId ->
            map[Screening.Patient_Id] = patientId
        }

        //Member or FHIR ID
        getMemberReference()?.let { memberId ->
            map[AssessmentDefinedParams.memberReference] = memberId
        }

        //Encounter object [Encounter ID and Provance]
        val provenance = HashMap<String, Any>()
        provenance[DefinedParams.Provenance] = ProvanceDto()
        map[AssessmentDefinedParams.encounter] = provenance

        when (myFormType()) {
            AssessmentDefinedParams.phq4 -> CommonUtils.calculatePHQScore(map)
            AssessmentDefinedParams.phq9 -> CommonUtils.calculatePHQScore(
                map,
                type = AssessmentDefinedParams.PHQ9
            )

            AssessmentDefinedParams.gad7 -> CommonUtils.calculatePHQScore(
                map,
                type = AssessmentDefinedParams.GAD7
            )

            AssessmentDefinedParams.cageAid -> calculateCAGEAIDSCore(map, serverData)
            AssessmentDefinedParams.suicidalIdeation -> calculateSuicidalIdeation(map)
        }

        try {
            var result = serverData?.let {
                FormResultComposer().groupValues(
                    context = requireContext(),
                    serverData = it,
                    map
                )
            }
            if (result != null) {
                CommonUtils.mentalHealths(viewModel.questionarieId, viewModel.observationId, result)
                result = Pair(StringConverter.convertGivenMapToString(result.second), result.second)
                result.first?.let {
                    val reqMap = StringConverter.convertStringToMap(it)
                    val request = StringConverter.getJsonObject(
                        Gson().toJson(reqMap)
                    )
                    viewModel.setAnalyticsData(
                        UserDetail.startDateTime,
                        eventName = AnalyticsDefinedParams.NCDMentalHealthCreation + " " + myFormType().takeIf { data -> !data.isNullOrBlank() },
                        isCompleted = true
                    )
                    viewModel.ncdMentalHealthMedicalReviewCreate(request, isAssessment())
                }
            }
        } catch (_: Exception) {
            //Exception - Catch block
        }
    }

    private fun getPatientReference(): String? {
        return requireArguments().getString(NCDMRUtil.PATIENT_REFERENCE)
    }

    private fun getMemberReference(): String? {
        return requireArguments().getString(NCDMRUtil.MEMBER_REFERENCE)
    }

    private fun isAssessment(): Boolean {
        return assessmentCards().contains(myFormType())
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            binding.ivClose.id, binding.btnCancel.id -> dismiss()
            binding.btnConfirm.id -> {
                val assessment = formGenerator.formSubmitAction(v)
                if (assessment) {
                    formGenerator.let {
                        processValuesAndProceed(
                            it.getResultMap(),
                            it.getServerData()
                        )
                    }
                }
            }
        }
    }
}