package com.medtroniclabs.opensource.ncd.assessment.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.removeTrailingPointZero
import com.medtroniclabs.opensource.common.CommonUtils.calculateAverageBloodPressure
import com.medtroniclabs.opensource.common.CommonUtils.calculateBMI
import com.medtroniclabs.opensource.common.CommonUtils.calculateBloodGlucose
import com.medtroniclabs.opensource.common.CommonUtils.calculateCVDRiskFactor
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.model.RecommendedDosageListModel
import com.medtroniclabs.opensource.databinding.ActivityAssessmentReadingBinding
import com.medtroniclabs.opensource.db.entity.RiskClassificationModel
import com.medtroniclabs.opensource.formgeneration.FormGenerator
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.extension.customSerializableExtra
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.formgeneration.listener.FormEventListener
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.ui.FormResultComposer
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.AssessmentReadingViewModel
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.BloodPressureViewModel
import com.medtroniclabs.opensource.ncd.assessment.viewmodel.GlucoseViewModel
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.ncd.medicalreview.viewmodel.NCDFormViewModel
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import dagger.hilt.android.AndroidEntryPoint
import java.lang.reflect.Type

@AndroidEntryPoint
class AssessmentReadingActivity : BaseActivity(), FormEventListener, View.OnClickListener {

    private lateinit var binding: ActivityAssessmentReadingBinding
    private val viewModel: AssessmentReadingViewModel by viewModels()
    private val ncdFormViewModel: NCDFormViewModel by viewModels()
    private val bpViewModel: BloodPressureViewModel by viewModels()
    private val glucoseViewModel: GlucoseViewModel by viewModels()

    private lateinit var formGenerator: FormGenerator

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAssessmentReadingBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root, isToolbarVisible = true, title = getString(R.string.add_new_reading)
        )
        init()
        renderAssessmentForm()
        attachObservers()
    }

    private fun init() {
        viewModel.apply {
            formTypeId = intent.getStringExtra(DefinedParams.FORM_TYPE_ID)
            (intent.customSerializableExtra(DefinedParams.IntentPatientDetails) as PatientListRespModel?)?.let { intentPatientDetails ->
                patientDetails = intentPatientDetails
            }
        }

        binding.btnSubmit.safeClickListener(this)
    }

    private fun renderAssessmentForm() {
        formGenerator = FormGenerator(
            this, binding.llForm, listener = this, scrollView = binding.scrollView, translate = SecuredPreference.getIsTranslationEnabled()
        ) { map, id ->
            when (id) {
                Screening.Weight, Screening.Height -> {
                    bpViewModel.renderBMIValue(this, formGenerator, map)
                }
            }
        }

        ncdFormViewModel.getNCDForm(MenuConstants.ASSESSMENT.lowercase())
        bpViewModel.getRiskEntityList()
    }

    private fun populateHeightWeight() {
        viewModel.patientDetails?.let {
            formGenerator.getViewByTag(Screening.Height)?.let { view ->
                formGenerator.setValueForView(it.height?.takeIf { it > 0.0 }
                    .removeTrailingPointZero(), view)
            }
            formGenerator.getViewByTag(Screening.Weight)?.let { view ->
                formGenerator.setValueForView(it.weight?.takeIf { it > 0.0 }
                    .removeTrailingPointZero(), view)
            }
        }
    }

    private fun attachObservers() {
        ncdFormViewModel.ncdFormResponse.observe(this) { resources ->
            when (resources.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resources.data?.let { responseData ->
                        val filter = responseData.filter {
                            it.id == viewModel.formTypeId || it.family == viewModel.formTypeId
                        }
                        formGenerator.populateViews(filter)
                        populateHeightWeight()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
        bpViewModel.getRiskEntityListLiveData.observe(this) {}
        bpViewModel.bpLogCreateResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    showSuccessDialogue(
                        title = getString(R.string.blood_pressure),
                        message = resourceState.data?.message ?: "",
                    ) {
                        bpViewModel.bpLogListResponseLiveData.postError()
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.error),
                        message = resourceState.message
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
                }
            }
        }
        glucoseViewModel.glucoseLogCreateResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    showSuccessDialogue(
                        title = getString(R.string.blood_glucose),
                        message = resourceState.data?.message ?: "",
                    ) {
                        glucoseViewModel.glucoseLogListResponseLiveData.postError()
                        setResult(RESULT_OK, intent)
                        finish()
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    showErrorDialogue(
                        title = getString(R.string.error),
                        message = resourceState.message
                            ?: getString(R.string.something_went_wrong_try_later),
                        positiveButtonName = getString(R.string.ok),
                    ) {}
                }
            }
        }
    }

    override fun onClick(view: View) {
        when (view.id) {
            binding.btnSubmit.id -> {
                formGenerator.formSubmitAction(view)
            }
        }
    }

    override fun loadLocalCache(id: String, localDataCache: Any, selectedParent: Long?) {/*
        Never used
         */
    }

    override fun onPopulate(targetId: String) {/*
        Never used
         */
    }

    override fun onCheckBoxDialogueClicked(
        id: String, serverViewModel: FormLayout, resultMap: Any?
    ) {/*
        Never used
         */
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?
    ) {/*
        Never used
         */
    }

    override fun onFormSubmit(resultMap: HashMap<String, Any>?, serverData: List<FormLayout?>?) {
        withNetworkCheck(connectivityManager, {
            resultMap?.let { map ->
                processValuesAndProceed(map, serverData)
            }
        })
    }

    override fun onRenderingComplete() {/*
        Never used
         */
    }

    override fun onUpdateInstruction(id: String, selectedId: Any?) {/*
        Never used
         */
    }

    override fun onInformationHandling(
        id: String, noOfDays: Int, enteredDays: Int?, resultMap: HashMap<String, Any>?
    ) {/*
        Never used
         */
    }

    override fun onAgeCheckForPregnancy() {/*
        Never used
         */
    }

    override fun handleMandatoryCondition(serverData: FormLayout?) {

    }

    override fun onAgeUpdateListener(
        age: Int,
        serverData: List<FormLayout?>?,
        resultHashMap: HashMap<String, Any>
    ) {
        /*
       Never used
        */
    }

    private fun processValuesAndProceed(
        resultMap: HashMap<String, Any>, serverData: List<FormLayout?>?
    ) {
        val map = HashMap<String, Any>()
        map.putAll(resultMap)

        when (viewModel.formTypeId) {
            DefinedParams.BP_LOG -> {
                //Average Systolic and Diastolic Calculation
                serverData?.first { it?.viewType == ViewType.VIEW_TYPE_FORM_BP }?.let {
                    bpViewModel.calculateBPValues(it, map)
                }
                calculateAverageBloodPressure(map)

                //BMI Calculation
                calculateBMI(map)

                //CVD Risk Calculation
                viewModel.patientDetails?.let { details ->
                    details.isRegularSmoker?.let { regularSmoke ->
                        map[Screening.is_regular_smoker] = regularSmoke
                    }
                    details.dateOfBirth?.let { dob ->
                        map[Screening.DateOfBirth] = dob
                    }
                    details.gender?.let { sex ->
                        map[DefinedParams.Gender] = sex
                    }
                }
                val resultOne = bpViewModel.getRiskEntityListLiveData.value
                val baseType: Type =
                    object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
                if (resultOne?.isNotEmpty() == true) {
                    val resultList = Gson().fromJson<ArrayList<RiskClassificationModel>>(
                        resultOne[0].nonLabEntity, baseType
                    )
                    calculateCVDRiskFactor(
                        map, ArrayList(resultList), bpViewModel.getSystolicAverage()
                    )
                }

                //BP Log Create - API
                val result = serverData?.let {
                    FormResultComposer().groupValues(
                        context = this, serverData = it, map
                    )
                }
                result?.first?.let {
                    StringConverter.stringToMap(it).let { requestMap ->
                        //Removing unwanted params
                        val bpLog = requestMap[DefinedParams.BP_LOG]
                        (bpLog as? LinkedTreeMap<String, Any>?)?.let { value ->
                            requestMap.putAll(value)
                            requestMap.remove(DefinedParams.BP_LOG)
                        }
                        requestMap.remove(Screening.DateOfBirth)
                        requestMap.remove(DefinedParams.Gender)

                        viewModel.patientDetails?.let { details ->
                            bpViewModel.createBpLog(
                                requestMap,
                                patientDetails = details,
                                intent.getStringExtra(NCDMRUtil.MENU_Name)
                            )
                        }
                    }
                }
            }

            DefinedParams.GLUCOSE_LOG -> {
                if (map.containsKey(Screening.BloodGlucoseID) || map.containsKey(
                        AssessmentDefinedParams.hba1c
                    )
                ) {
                    //Glucose Calculation
                    calculateBloodGlucose(map) {}

                    //Glucose Log Create - API
                    val result = serverData?.let {
                        FormResultComposer().groupValues(
                            context = this, serverData = it, map
                        )
                    }
                    result?.first?.let {
                        StringConverter.stringToMap(it).let { requestMap ->
                            //Removing unwanted params
                            val glucoseLog = requestMap[DefinedParams.GLUCOSE_LOG]
                            (glucoseLog as? LinkedTreeMap<String, Any>?)?.let { value ->
                                requestMap.putAll(value)
                                requestMap.remove(DefinedParams.GLUCOSE_LOG)
                            }

                            viewModel.patientDetails?.let { details ->
                                glucoseViewModel.glucoseLogCreate(
                                    requestMap,
                                    patientDetails = details,
                                    intent.getStringExtra(NCDMRUtil.MENU_Name)
                                )
                            }
                        }
                    }
                } else
                    showErrorDialogue(message = getString(R.string.error_label)) {}
            }
        }
    }
}