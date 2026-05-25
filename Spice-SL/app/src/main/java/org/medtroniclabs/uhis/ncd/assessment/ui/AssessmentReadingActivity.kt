package org.medtroniclabs.uhis.ncd.assessment.ui

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.google.gson.Gson
import com.google.gson.internal.LinkedTreeMap
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.removeTrailingPointZero
import org.medtroniclabs.uhis.common.CommonUtils.calculateAverageBloodPressure
import org.medtroniclabs.uhis.common.CommonUtils.calculateBMI
import org.medtroniclabs.uhis.common.CommonUtils.calculateBloodGlucose
import org.medtroniclabs.uhis.common.CommonUtils.calculateCVDRiskFactor
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.model.RecommendedDosageListModel
import org.medtroniclabs.uhis.databinding.ActivityAssessmentReadingBinding
import org.medtroniclabs.uhis.db.entity.RiskClassificationModel
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.extension.customSerializableExtra
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.formgeneration.listener.FormEventListener
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.formgeneration.ui.FormResultComposer
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.model.PatientListRespModel
import org.medtroniclabs.uhis.ncd.assessment.viewmodel.AssessmentReadingViewModel
import org.medtroniclabs.uhis.ncd.assessment.viewmodel.BloodPressureViewModel
import org.medtroniclabs.uhis.ncd.assessment.viewmodel.GlucoseViewModel
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil
import org.medtroniclabs.uhis.ncd.medicalreview.viewmodel.NCDFormViewModel
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.assessment.AssessmentDefinedParams
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
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.add_new_reading),
        )
        init()
        renderAssessmentForm()
        attachObservers()
    }

    override fun consumeImeInsets() = true

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
            this,
            binding.llForm,
            listener = this,
            scrollView = binding.scrollView,
            translate = SecuredPreference.getIsTranslationEnabled(),
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
                formGenerator.setValueForView(
                    it.height
                        ?.takeIf { it > 0.0 }
                        .removeTrailingPointZero(),
                    view,
                )
            }
            formGenerator.getViewByTag(Screening.Weight)?.let { view ->
                formGenerator.setValueForView(
                    it.weight
                        ?.takeIf { it > 0.0 }
                        .removeTrailingPointZero(),
                    view,
                )
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

    override fun loadLocalCache(
        id: String,
        localDataCache: Any,
        selectedParent: Long?,
    ) {
        /*
        Never used
         */
    }

    override fun onPopulate(targetId: String) {
        /*
        Never used
         */
    }

    override fun onCheckBoxDialogueClicked(
        id: String,
        formLayout: FormLayout,
        resultMap: Any?,
    ) {
        /*
        Never used
         */
    }

    override fun onInstructionClicked(
        id: String,
        title: String,
        informationList: ArrayList<String>?,
        description: String?,
        dosageListModel: ArrayList<RecommendedDosageListModel>?,
    ) {
        /*
        Never used
         */
    }

    override fun onFormSubmit(
        resultMap: HashMap<String, Any>?,
        serverData: List<FormLayout>?,
    ) {
        withNetworkCheck(connectivityManager, {
            resultMap?.let { map ->
                processValuesAndProceed(map, serverData)
            }
        })
    }

    override fun onRenderingComplete() {
        /*
        Never used
         */
    }

    override fun onUpdateInstruction(
        id: String,
        selectedId: Any?,
    ) {
        /*
        Never used
         */
    }

    override fun onInformationHandling(
        id: String,
        noOfDays: Int,
        enteredDays: Int?,
        resultMap: HashMap<String, Any>?,
    ) {
        /*
        Never used
         */
    }

    override fun onAgeCheckForPregnancy() {
        /*
        Never used
         */
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

    private fun processValuesAndProceed(
        resultMap: HashMap<String, Any>,
        serverData: List<FormLayout>?,
    ) {
        val map = HashMap<String, Any>()
        map.putAll(resultMap)

        when (viewModel.formTypeId) {
            DefinedParams.BP_LOG -> {
                // Average Systolic and Diastolic Calculation
                serverData?.first { it?.viewType == ViewType.VIEW_TYPE_FORM_BP }?.let {
                    bpViewModel.calculateBPValues(it, map)
                }
                calculateAverageBloodPressure(map)

                // BMI Calculation
                calculateBMI(map)

                // CVD Risk Calculation
                viewModel.patientDetails?.let { details ->
                    details.isRegularSmoker?.let { regularSmoke ->
                        map[Screening.IS_REGULAR_SMOKER] = regularSmoke
                    }
                    details.dateOfBirth?.let { dob ->
                        map[Screening.DateOfBirth] = dob
                    }
                    details.gender?.let { sex ->
                        map[DefinedParams.GENDER] = sex
                    }
                }
                val resultOne = bpViewModel.getRiskEntityListLiveData.value
                val baseType: Type =
                    object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
                if (resultOne?.isNotEmpty() == true) {
                    val resultList = Gson().fromJson<ArrayList<RiskClassificationModel>>(
                        resultOne[0].nonLabEntity,
                        baseType,
                    )
                    calculateCVDRiskFactor(
                        map,
                        ArrayList(resultList),
                        bpViewModel.getSystolicAverage(),
                    )
                }

                // BP Log Create - API
                val result = serverData?.let {
                    FormResultComposer().groupValues(
                        serverData = it,
                        map,
                    )
                }
                result?.first?.let {
                    StringConverter.stringToMap(it).let { requestMap ->
                        // Removing unwanted params
                        val bpLog = requestMap[DefinedParams.BP_LOG]
                        (bpLog as? LinkedTreeMap<String, Any>?)?.let { value ->
                            requestMap.putAll(value)
                            requestMap.remove(DefinedParams.BP_LOG)
                        }
                        requestMap.remove(Screening.DateOfBirth)
                        requestMap.remove(DefinedParams.GENDER)

                        viewModel.patientDetails?.let { details ->
                            bpViewModel.createBpLog(
                                requestMap,
                                patientDetails = details,
                                intent.getStringExtra(NCDMRUtil.MENU_Name),
                            )
                        }
                    }
                }
            }

            DefinedParams.GLUCOSE_LOG -> {
                if (map.containsKey(Screening.BloodGlucoseID) ||
                    map.containsKey(
                        AssessmentDefinedParams.hba1c,
                    )
                ) {
                    // Glucose Calculation
                    calculateBloodGlucose(map) {}

                    // Glucose Log Create - API
                    val result = serverData?.let {
                        FormResultComposer().groupValues(
                            serverData = it,
                            map,
                        )
                    }
                    result?.first?.let {
                        StringConverter.stringToMap(it).let { requestMap ->
                            // Removing unwanted params
                            val glucoseLog = requestMap[DefinedParams.GLUCOSE_LOG]
                            (glucoseLog as? LinkedTreeMap<String, Any>?)?.let { value ->
                                requestMap.putAll(value)
                                requestMap.remove(DefinedParams.GLUCOSE_LOG)
                            }

                            viewModel.patientDetails?.let { details ->
                                glucoseViewModel.glucoseLogCreate(
                                    requestMap,
                                    patientDetails = details,
                                    intent.getStringExtra(NCDMRUtil.MENU_Name),
                                )
                            }
                        }
                    }
                } else {
                    showErrorDialogue(message = getString(R.string.error_label)) {}
                }
            }
        }
    }
}
