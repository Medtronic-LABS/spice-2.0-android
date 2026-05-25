package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.AssessmentListRequest
import org.medtroniclabs.uhis.data.registration.BPLogListResponse
import org.medtroniclabs.uhis.data.registration.BPResponse
import org.medtroniclabs.uhis.data.registration.BloodGlucoseListResponse
import org.medtroniclabs.uhis.data.registration.ConfirmDiagnosesRequest
import org.medtroniclabs.uhis.data.registration.GraphModel
import org.medtroniclabs.uhis.data.registration.InitialDiagnosis
import org.medtroniclabs.uhis.data.registration.InstructionModel
import org.medtroniclabs.uhis.data.registration.LabTestSearchResponse
import org.medtroniclabs.uhis.data.registration.Lifestyle
import org.medtroniclabs.uhis.data.registration.MedicationSearchReqModel
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.data.registration.PatientPregnancyModel
import org.medtroniclabs.uhis.data.registration.PatientRemoveRequest
import org.medtroniclabs.uhis.data.registration.PregnancyCreateRequest
import org.medtroniclabs.uhis.data.registration.PregnancyRiskUpdate
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.RequestPatientDetail
import org.medtroniclabs.uhis.data.registration.ResponsePatientDetail
import org.medtroniclabs.uhis.data.registration.SessionGraphItem
import org.medtroniclabs.uhis.data.registration.SessionGraphModel
import org.medtroniclabs.uhis.data.registration.SessionModelResponse
import org.medtroniclabs.uhis.data.registration.SummaryResponse
import org.medtroniclabs.uhis.data.registration.TerminateSessionModel
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import retrofit2.Response
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class PatientDetailViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    val patientDetailsResponse = MutableLiveData<Resource<PatientDetailsModel>>()
    val assessmentPatientDetails = MutableLiveData<Resource<PatientDetailsModel>>()
    val patientBPLogListResponse = MutableLiveData<Resource<BPLogListResponse>>()
    val patientBPLogGraphListResponse = MutableLiveData<Resource<BPLogListResponse>>()

    val patientBloodGlucoseListResponse = MutableLiveData<Resource<BloodGlucoseListResponse>>()
    val sessionGraphResponse = MutableLiveData<Resource<SessionGraphModel>>()
    var patientId: Long? = null
    var patientVisitId: Long? = null
    var origin: String? = null
    var patientPregnancyId: Long? = null
    val searchResponse = MutableLiveData<Resource<ArrayList<LabTestSearchResponse>?>>()
    var selectedLabTest: LabTestSearchResponse? = null
    val medicationSearchResponse = MutableLiveData<Resource<ArrayList<PrescriptionModel>>>()
    var selectedMedication: PrescriptionModel? = null
    val medicalReviewSummaryResponse = MutableLiveData<Resource<SummaryResponse>>()
    val pregnancyDetails = MutableLiveData<String>()
    var allowDismiss: Boolean = true
    var patientTrackId: Long = -1
    var mentalHealthQuestions = MutableLiveData<Resource<LocalSpinnerResponse>>()
    var mentalHealthCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var pregnancyCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var patientPregnancyDetailResponse = MutableLiveData<Resource<PregnancyCreateRequest>>()

    val confirmDiagnosisRequestData = ConfirmDiagnosesRequest()
    val confirmDiagnosisRequest = MutableLiveData<Resource<HashMap<String, Any>>>()
    var topCardShow = true
    var showMentalHealthCard = false
    var isFromCMR = false
    var isMentalHealthUpdated = MutableLiveData<Boolean>()
    var mentalHealthDetails = MutableLiveData<Resource<HashMap<String, Any>>>()
    val treatmentPlanDetailsResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val updateTreatmentPlanResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val refreshMedicalReview = MutableLiveData<Resource<Boolean>>()
    val treatmentPlanData = MutableLiveData<Resource<HashMap<String, Any>>>()
    var treatmentPlanResultMap = HashMap<String, Any>()
    val screeningDetailResponse = MutableLiveData<Resource<ResponsePatientDetail>>()
    var totalBGCount: Int = 0
    var totalBPCount: Int = 0
    var totalBPTotalCount: Int? = null
    var totalMHTotalCount: Int? = null
    var latestBpLogResponse: BPResponse? = null
    var selectedBGDropDown = MutableLiveData<Int>()
    var screeningId: Long? = null
    var patientTrackerID: String? = null
    var patientDetails: PatientDetailsModel? = null
    val createBPvitalResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val createBloodGlucoseResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var height: FormLayout? = null
    var weight: FormLayout? = null
    var bpLog: FormLayout? = null
    var tobaccoQuestion: FormLayout? = null
    var bloodGlucose: FormLayout? = null
    var hbA1c: FormLayout? = null
    val bgResultHashMap = HashMap<String, Any>()
    val resultHashMap = HashMap<String, Any>()
    var isSummaryDetailsLoaded = false
    val refreshGraphDetails = MutableLiveData<Resource<Boolean>>()
    val patientRemoveResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var diagnosisList = ArrayList<DiagnosisEntity>()
    val onBPValueSelectedObserver = MutableLiveData<GraphModel>()
    val onBGValueSelectedObserver = MutableLiveData<GraphModel>()
    val onMHValueSelectedObserver = MutableLiveData<GraphModel>()
    val clearRedRiskResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val instructionModelResponse = MutableLiveData<Resource<InstructionModel>>()
    val siteEntity = MutableLiveData<SiteEntity>()
    val autoPopulateCounty = MutableLiveData<String>()
    val autoPopulateSubCounty = MutableLiveData<String>()
    val isAPUpdated = MutableLiveData<Resource<Boolean>>()
    val isNextMRDateMandate = MutableLiveData<Resource<Boolean>>()
    val updatePregnancyRisk = MutableLiveData<Resource<Boolean>>()
    val patientFamilyDetails = MutableLiveData<Resource<HashMap<String, Any>>>()
    val isClientDetailsUpdated = MutableLiveData<Resource<Boolean>>()
    var sessionQuestions = MutableLiveData<Resource<ArrayList<SessionModelResponse>>>()
    val postTerminateSession = MutableLiveData<Resource<HashMap<String, Any>>>()
    var graphPointsList: ArrayList<SessionGraphItem>? = null
    var srqMapType: String = ""
    val shortageReasonList = MutableLiveData<Resource<List<ShortageReasonEntity>>>()
    val recommendationResponse = MutableLiveData<Resource<ArrayList<PrescriptionModel>>>()
    val autoPopulateUpazila = MutableLiveData<String>()

    var patientTenantId: Long? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    var ouSitesLiveData = MutableLiveData<Resource<ArrayList<SiteEntity>>>()

    fun getPatientDetails(
        context: Context,
        isAssessmentDataRequired: Boolean,
        isPrescriberRequired: Boolean = false,
        isLifestyleRequired: Boolean = false,
        showLoader: Boolean = true,
        isAssessment: Boolean = false,
    ) {
        patientId?.let {
            val request =
                PatientDetailsModel(
                    it,
                    isAssessmentDataRequired = isAssessmentDataRequired,
                    isPrescriberRequired = isPrescriberRequired,
                    isLifestyleRequired = isLifestyleRequired,
                    sessionRequired = false,
                )
            if (connectivityManager.isNetworkAvailable()) {
                fetchPatientDetails(request, showLoader, isAssessment)
            } else if (showLoader) {
                patientDetailsResponse.setError(context.getString(R.string.no_internet_error))
            }
            ""
        }
    }

    private fun fetchPatientDetails(
        request: PatientDetailsModel,
        showLoader: Boolean,
        isAssessment: Boolean,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (showLoader) {
                patientDetailsResponse.postLoading()
            }
            try {
                request.apply {
                    tenantId = SecuredPreference.getTenantId()
                }
                val response = medicalReviewRepo.getPatientDetails(request)
                handleResponseState(response, showLoader)
            } catch (e: Exception) {
                if (showLoader) {
                    patientDetailsResponse.postError()
                }
            }
        }
    }

    private fun handleResponseState(
        response: Response<APIResponse<PatientDetailsModel>>,
        showLoader: Boolean,
    ) {
        if (response.isSuccessful) {
            val entity = response.body()?.entity
            if (entity == null) {
                patientDetailsResponse.postError()
            } else {
                screeningId = entity.screeningLogId
                patientDetailsResponse.postSuccess(data = entity)
            }
        } else if (showLoader) {
            patientDetailsResponse.postError()
        }
    }

    fun getPatientBPLogList(
        context: Context,
        request: AssessmentListRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                patientBPLogListResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientBPLogList(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientBPLogListResponse.postSuccess(res.entity)
                        } else {
                            patientBPLogListResponse.postError()
                        }
                    } else {
                        patientBPLogListResponse.postError()
                    }
                } catch (e: Exception) {
                    patientBPLogListResponse.postError()
                }
            }
        } else {
            patientBPLogListResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getPatientBPLogListForGraph(
        context: Context,
        request: AssessmentListRequest,
        forward: Boolean? = null,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                patientBPLogGraphListResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientBPLogList(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientBPLogGraphListResponse.postSuccess(res.entity, forward)
                        } else {
                            patientBPLogGraphListResponse.postError()
                        }
                    } else {
                        patientBPLogGraphListResponse.postError()
                    }
                } catch (e: Exception) {
                    patientBPLogGraphListResponse.postError()
                }
            }
        } else {
            patientBPLogGraphListResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getPatientBloodGlucoseList(
        context: Context,
        request: AssessmentListRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    patientBloodGlucoseListResponse.postLoading()
                    val response = medicalReviewRepo.getPatientBloodGlucoseList(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientBloodGlucoseListResponse.postSuccess(res.entity)
                        } else {
                            patientBloodGlucoseListResponse.postError()
                        }
                    } else {
                        patientBloodGlucoseListResponse.postError()
                    }
                } catch (e: Exception) {
                    patientBloodGlucoseListResponse.postError()
                }
            }
        } else {
            patientBloodGlucoseListResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getSessionGraph(
        context: Context,
        request: AssessmentListRequest,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    sessionGraphResponse.postLoading()
                    val response = medicalReviewRepo.getSessionGraph(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            sessionGraphResponse.postSuccess(res.entity)
                        } else {
                            sessionGraphResponse.postError()
                        }
                    } else {
                        sessionGraphResponse.postError()
                    }
                } catch (e: Exception) {
                    sessionGraphResponse.postError()
                }
            }
        } else {
            sessionGraphResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

//    fun fetchMentalHealthQuestions(id: String, type: String) {
//        viewModelScope.launch(dispatcherIO) {
//            mentalHealthQuestions.postLoading()
//            try {
//                val questions = medicalReviewRepo.getMHQuestionsByType(type = type)
//                mentalHealthQuestions.postSuccess(
//                    LocalSpinnerResponse(
//                        tag = id,
//                        response = questions
//                    )
//                )
//            } catch (e: Exception) {
//                mentalHealthQuestions.postError()
//            }
//        }
//    }

//    fun createOrUpdateMentalHealth(context: Context,request: HashMap<String, Any>, isUpdate: Boolean) {
//        if (connectivityManager.isNetworkAvailable()) {
//            viewModelScope.launch(dispatcherIO) {
//                mentalHealthCreateResponse.postLoading()
//                try {
//                    val response: Response<APIResponse<HashMap<String, Any>>> = if (isUpdate)
//                        medicalReviewRepo.updateMentalHealth(request)
//                    else
//                        medicalReviewRepo.createMentalHealth(request)
//                    if (response.isSuccessful) {
//                        val res = response.body()
//                        if (res?.status == true) {
//                            isMentalHealthUpdated.postValue(true)
//                            mentalHealthCreateResponse.postSuccess(res.entity)
//                        } else
//                            mentalHealthCreateResponse.postError()
//                    } else
//                        mentalHealthCreateResponse.postError()
//                } catch (e: Exception) {
//                    mentalHealthCreateResponse.postError()
//                }
//            }
//        } else {
//            mentalHealthCreateResponse.setError(context.getString(R.string.no_internet_error))
//        }
//    }

    fun searchMedication(
        context: Context,
        requestModel: MedicationSearchReqModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                medicationSearchResponse.postLoading()
                try {
                    val response = medicalReviewRepo.searchMedication(requestModel)

                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            medicationSearchResponse.postSuccess(res.entityList ?: ArrayList())
                        } else {
                            medicationSearchResponse.postError()
                        }
                    } else {
                        medicationSearchResponse.postError()
                    }
                } catch (e: Exception) {
                    medicationSearchResponse.postError()
                }
            }
        } else {
            medicationSearchResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun createPregnancy(
        context: Context,
        request: HashMap<String, Any>,
        isFromUpdate: Boolean,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                pregnancyCreateResponse.postLoading()
                try {
                    val gson = Gson()
                    val json = gson.toJson(request)
                    val type: Type = object : TypeToken<PregnancyCreateRequest>() {}.type
                    val pregnancyCreateRequest = gson.fromJson<PregnancyCreateRequest>(json, type)
                    if (!isFromUpdate) {
                        pregnancyCreate(pregnancyCreateRequest)
                    } else {
                        pregnancyUpdate(pregnancyCreateRequest)
                    }
                } catch (e: Exception) {
                    pregnancyCreateResponse.postError()
                }
            }
        } else {
            pregnancyCreateResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    private suspend fun pregnancyUpdate(pregnancyCreateRequest: PregnancyCreateRequest) {
        val response = medicalReviewRepo.updatePregnancy(pregnancyCreateRequest)
        if (response.isSuccessful) {
            val res = response.body()
            if (res?.status == true) {
                pregnancyCreateResponse.postSuccess(res.entity)
            } else {
                pregnancyCreateResponse.postError()
            }
        } else {
            pregnancyCreateResponse.postError()
        }
    }

    private suspend fun pregnancyCreate(pregnancyCreateRequest: PregnancyCreateRequest) {
        val response = medicalReviewRepo.createPregnancy(pregnancyCreateRequest)
        if (response.isSuccessful) {
            val res = response.body()
            if (res?.status == true) {
                pregnancyCreateResponse.postSuccess(res.entity)
            } else {
                pregnancyCreateResponse.postError()
            }
        } else {
            pregnancyCreateResponse.postError()
        }
    }

    fun getPatientPregnancyDetails(
        context: Context,
        request: PatientPregnancyModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                patientPregnancyDetailResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getPatientPregnancyDetails(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientPregnancyDetailResponse.postSuccess(res.entity)
                        } else {
                            patientPregnancyDetailResponse.postError()
                        }
                    } else {
                        patientPregnancyDetailResponse.postError()
                    }
                } catch (e: Exception) {
                    patientPregnancyDetailResponse.postError()
                }
            }
        } else {
            patientPregnancyDetailResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun confirmDiagnosis(
        context: Context,
        request: ConfirmDiagnosesRequest,
    ) {
        try {
            viewModelScope.launch(dispatcherIO) {
                try {
                    if (connectivityManager.isNetworkAvailable()) {
                        confirmDiagnosisRequest.postLoading()
                        val response = medicalReviewRepo.confirmDiagnosis(request)
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                confirmDiagnosisRequest.postSuccess(res.entity)
                            } else {
                                confirmDiagnosisRequest.postError()
                            }
                        } else {
                            confirmDiagnosisRequest.postError()
                        }
                    } else {
                        confirmDiagnosisRequest.postError(context.getString(R.string.no_internet_error))
                    }
                } catch (e: Exception) {
                    confirmDiagnosisRequest.postError()
                }
            }
        } catch (e: Exception) {
            confirmDiagnosisRequest.postError()
        }
    }

    fun getMentalHealthDetails(
        context: Context,
        request: HashMap<String, Any>,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                mentalHealthDetails.postLoading()
                try {
                    val response = medicalReviewRepo.getMentalHealthDetails(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            mentalHealthDetails.postSuccess(res.entity)
                        } else {
                            mentalHealthDetails.postError()
                        }
                    } else {
                        mentalHealthDetails.postError()
                    }
                } catch (e: Exception) {
                    mentalHealthDetails.postError()
                }
            }
        } else {
            mentalHealthDetails.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getPatientMedicalReviewSummary(
        context: Context,
        request: MedicalReviewBaseRequest,
        refreshPatientDetails: Boolean = false,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    medicalReviewSummaryResponse.postLoading()
                    val response = medicalReviewRepo.getPatientMedicalReviewSummary(request)
                    if (response.isSuccessful) {
                        val res = response.body()?.entity
                        res?.refreshPatientDetails = refreshPatientDetails
                        medicalReviewSummaryResponse.postSuccess(res)
                    } else {
                        medicalReviewSummaryResponse.postError()
                    }
                } catch (e: Exception) {
                    medicalReviewSummaryResponse.postError()
                }
            }
        } else {
            medicalReviewSummaryResponse.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun treatmentPlanDetails(context: Context) {
        try {
            val request = HashMap<String, Any>()
            SecuredPreference.getTenantId().let {
                request[DefinedParams.TENANT_ID] = it
            }
            request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId

            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    try {
                        treatmentPlanDetailsResponse.postLoading()
                        val response = medicalReviewRepo.treatmentPlanDetails(request)
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                treatmentPlanDetailsResponse.postSuccess(res.entity)
                            } else {
                                treatmentPlanDetailsResponse.postError()
                            }
                        } else {
                            treatmentPlanDetailsResponse.postError()
                        }
                    } catch (e: Exception) {
                        treatmentPlanDetailsResponse.postError()
                    }
                }
            } else {
                treatmentPlanDetailsResponse.setError(context.getString(R.string.no_internet_error))
            }
        } catch (e: Exception) {
            treatmentPlanDetailsResponse.postError()
        }
    }

    fun updateTreatmentPlan(context: Context) {
        try {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    updateTreatmentPlanResponse.postLoading()
                    try {
                        val response = medicalReviewRepo.updateTreatmentPlan(treatmentPlanResultMap)
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                // isAfricaOrNot(treatmentPlanResultMap)
                                val resultMap = HashMap<String, Any>()
                                res.message?.let { successMessage ->
                                    resultMap[DefinedParams.MESSAGE] = getSuccessMessage(successMessage, context)
                                }
                                updateTreatmentPlanResponse.postSuccess(resultMap)
                            } else {
                                updateTreatmentPlanResponse.postError()
                            }
                        } else {
                            updateTreatmentPlanResponse.postError()
                        }
                    } catch (e: Exception) {
                        updateTreatmentPlanResponse.postError()
                    }
                }
            } else {
                updateTreatmentPlanResponse.setError(context.getString(R.string.no_internet_error))
            }
        } catch (e: Exception) {
            updateTreatmentPlanResponse.postError()
        }
    }

    private fun getSuccessMessage(
        successMessage: String,
        context: Context,
    ): Any = if (SecuredPreference.getIsTranslationEnabled()) context.getString(R.string.treatment_plan_success_message) else successMessage

    fun canShowDiagnosisAlert(
        data: InitialDiagnosis?,
        confirmedList: ArrayList<String>?,
    ): Pair<Int, Boolean> {
        val confirmedDiagnosisList = if (confirmedList.isNullOrEmpty()) ArrayList<String>() else ArrayList(confirmedList)
        var statusCode = -1
        var showDiagnosis = false
        data?.apply {
            if (!diabetesPatientType.equals(
                    DefinedParams.KNOWN,
                    ignoreCase = true,
                ) &&
                !htnPatientType.equals(DefinedParams.KNOWN, ignoreCase = true)
            ) {
                return Pair(statusCode, false)
            }

            if (confirmedDiagnosisList.isEmpty()) {
                return Pair(statusCode, true)
            }

            checkDiabetesForKnown(
                statusCode,
                showDiagnosis,
                diabetesPatientType,
                diabetesDiagControlledType,
                confirmedDiagnosisList,
                diabetesDiagnosis,
            )?.let { data ->
                statusCode = data.first
                showDiagnosis = data.second
            }

            if (!showDiagnosis && htnPatientType == DefinedParams.KNOWN) {
                showDiagnosis =
                    (confirmedDiagnosisList.contains(DefinedParams.PRE_HYPERTENSION) || confirmedDiagnosisList.contains(DefinedParams.HYPERTENSION)) == false
                if (showDiagnosis) {
                    statusCode = 1
                }
            }
        }

        return Pair(statusCode, showDiagnosis)
    }

    private fun checkDiabetesForKnown(
        statusCode: Int,
        showDiagnosis: Boolean,
        diabetesPatientType: String?,
        diabetesDiagControlledType: String?,
        confirmedDiagnosisList: ArrayList<String>,
        diabetesDiagnosis: String?,
    ): Pair<Int, Boolean> {
        var status = statusCode
        var isShowDiagnosis = showDiagnosis
        if (diabetesPatientType == DefinedParams.KNOWN) {
            if (diabetesDiagControlledType == DefinedParams.PRE_DIABETIC) {
                status = updateListItem(confirmedDiagnosisList, DefinedParams.PRE_DIABETIC)
                isShowDiagnosis = confirmedDiagnosisList.contains(DefinedParams.PRE_DIABETIC) == false
            } else {
                updateStatusShowDiagnosis(diabetesDiagnosis, statusCode, showDiagnosis, confirmedDiagnosisList).let { data ->
                    status = data.first
                    isShowDiagnosis = data.second
                }
            }
        }
        return Pair(status, isShowDiagnosis)
    }

    private fun updateStatusShowDiagnosis(
        diabetesDiagnosis: String?,
        statusCode: Int,
        showDiagnosis: Boolean,
        confirmedDiagnosisList: ArrayList<String>,
    ): Pair<Int, Boolean> {
        var status = statusCode
        var isShowDiagnosis = showDiagnosis
        when (diabetesDiagnosis) {
            DefinedParams.TYPE_ONE -> {
                status = updateListItem(confirmedDiagnosisList, DefinedParams.TYPE_ONE)
                isShowDiagnosis = confirmedDiagnosisList.contains(DefinedParams.DMT_ONE) == false
            }
            DefinedParams.TYPE_TWO -> {
                status = updateListItem(confirmedDiagnosisList, DefinedParams.TYPE_TWO)
                isShowDiagnosis =
                    confirmedDiagnosisList.contains(DefinedParams.DMT_TWO) == false
            }
            DefinedParams.GESTATIONAL_DIABETES -> {
                status = updateListItem(confirmedDiagnosisList, DefinedParams.GESTATIONAL_DIABETES)
                isShowDiagnosis =
                    confirmedDiagnosisList.contains(DefinedParams.GESTATIONAL_DIABETES) == false
            }
        }
        return Pair(status, isShowDiagnosis)
    }

    private fun updateListItem(
        confirmedDiagnosisList: ArrayList<String>,
        type: String,
    ): Int {
        when (type) {
            DefinedParams.PRE_DIABETIC -> {
                return removeIfContains(
                    confirmedDiagnosisList,
                    DefinedParams.DMT_ONE,
                    DefinedParams.DMT_TWO,
                    DefinedParams.GESTATIONAL_DIABETES,
                    DefinedParams.PRE_DIABETIC,
                )
            }

            DefinedParams.TYPE_ONE -> {
                return removeIfContains(
                    confirmedDiagnosisList,
                    DefinedParams.DMT_TWO,
                    DefinedParams.GESTATIONAL_DIABETES,
                    DefinedParams.PRE_DIABETIC,
                    DefinedParams.DMT_ONE,
                )
            }

            DefinedParams.TYPE_TWO -> {
                return removeIfContains(
                    confirmedDiagnosisList,
                    DefinedParams.DMT_ONE,
                    DefinedParams.GESTATIONAL_DIABETES,
                    DefinedParams.PRE_DIABETIC,
                    DefinedParams.DMT_TWO,
                )
            }

            DefinedParams.GESTATIONAL_DIABETES -> {
                return removeIfContains(
                    confirmedDiagnosisList,
                    DefinedParams.DMT_ONE,
                    DefinedParams.DMT_TWO,
                    DefinedParams.PRE_DIABETIC,
                    DefinedParams.GESTATIONAL_DIABETES,
                )
            }
        }

        return -1
    }

    private fun removeIfContains(
        confirmedDiagnosisList: ArrayList<String>,
        param1: String,
        param2: String,
        param3: String,
        param4: String,
    ): Int {
        if (confirmedDiagnosisList.contains(param1)) {
            confirmedDiagnosisList.remove(param1)
            return 1
        } else if (confirmedDiagnosisList.contains(param2)) {
            confirmedDiagnosisList.remove(param2)
            return 1
        } else if (confirmedDiagnosisList.contains(param3)) {
            confirmedDiagnosisList.remove(param3)
            return 1
        } else if (!confirmedDiagnosisList.contains(param4)) {
            return 1
        }
        return -1
    }

    fun getTreatmentPlanData(context: Context) {
        try {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    treatmentPlanData.postLoading()
                    val response = medicalReviewRepo.getTreatmentPlanData()
                    val map = LinkedHashMap<String, Any>()
//                    response.forEach {
//                        map[it.frequencyKey] = it.value
//                    }
                    treatmentPlanData.postSuccess(data = map)
                }
            } else {
                treatmentPlanData.setError(context.getString(R.string.no_internet_error))
            }
        } catch (e: Exception) {
            treatmentPlanData.postError()
        }
    }

    fun getScreeningDetails(request: RequestPatientDetail) {
        try {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    screeningDetailResponse.postLoading()
                    val response = medicalReviewRepo.getScreeningDetails(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            screeningDetailResponse.postSuccess(res.entity)
                        } else {
                            screeningDetailResponse.postError()
                        }
                    } else {
                        screeningDetailResponse.postError()
                    }
                }
            } else {
                screeningDetailResponse.postError()
            }
        } catch (e: Exception) {
            screeningDetailResponse.postError()
        }
    }

    fun getCountySubCountyIds() {
        try {
            viewModelScope.launch(dispatcherIO) {
                (medicalReviewRepo.getUpazilaList() as? List<*>?)?.let { list ->
                    if (list.isNotEmpty()) {
                        (list[0] as? SiteEntity?)?.let { site ->
                            siteEntity.postValue(site)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Exception - Catch block
        }
    }

    fun getCountyById() {
        viewModelScope.launch(dispatcherIO) {
            siteEntity.value?.countyId?.let {
                //  autoPopulateCounty.postValue(medicalReviewRepo.getCountyById(it).name)
            }
        }
    }

    fun getSubCountyById() {
        viewModelScope.launch(dispatcherIO) {
            siteEntity.value?.subCountyId?.let {
                // autoPopulateSubCounty.postValue(medicalReviewRepo.getSubCountyById(it).name)
            }
        }
    }

    fun getUpazilaById() {
        viewModelScope.launch(dispatcherIO) {
            siteEntity.value?.subCountyId?.let {
                // autoPopulateUpazila.postValue(medicalReviewRepo.getSubCountyById(it).name)
            }
        }
    }

    fun getPatientTenantId(): Long = (patientDetailsResponse.value?.data?.tenantId) ?: -1

    fun createBpLog(request: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            createBPvitalResponse.postLoading()
            try {
                val response = medicalReviewRepo.createBpLog(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        createBPvitalResponse.postSuccess(res.entity)
                    } else {
                        createBPvitalResponse.postError()
                    }
                } else {
                    createBPvitalResponse.postError()
                }
            } catch (e: Exception) {
                createBPvitalResponse.postError()
            }
        }
    }

    fun createGlucoseLog(request: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            createBloodGlucoseResponse.postLoading()
            try {
                val response = medicalReviewRepo.createGlucoseLog(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        createBloodGlucoseResponse.postSuccess(res.entity)
                    } else {
                        createBloodGlucoseResponse.postError()
                    }
                } else {
                    createBloodGlucoseResponse.postError()
                }
            } catch (e: Exception) {
                createBloodGlucoseResponse.postError()
            }
        }
    }

    fun patientRemove(
        context: Context,
        request: PatientRemoveRequest,
    ) {
        viewModelScope.launch(dispatcherIO) {
            patientRemoveResponse.postLoading()
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    val response = medicalReviewRepo.patientRemove(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            val resultMap = HashMap<String, Any>()
                            res.message?.let { successMessage ->
                                resultMap[DefinedParams.MESSAGE] = successMessage
                            }
                            patientRemoveResponse.postSuccess(resultMap)
                        } else {
                            patientRemoveResponse.postError()
                        }
                    } else {
                        patientRemoveResponse.postError()
                    }
                } else {
                    patientRemoveResponse.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                patientRemoveResponse.postError()
            }
        }
    }

    fun getAssessmentPatientDetails(context: Context) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    patientId?.let {
                        assessmentPatientDetails.postLoading()
                        val apiRequest =
                            PatientDetailsModel(
                                it,
                                isAssessmentDataRequired = false,
                                isPrescriberRequired = false,
                                isLifestyleRequired = false,
                                tenantId = SecuredPreference.getTenantId(),
                            )
                        val response = medicalReviewRepo.getPatientDetails(apiRequest)
                        if (response.isSuccessful) {
                            val entity = response.body()?.entity
                            if (entity == null) {
                                assessmentPatientDetails.postError()
                            } else {
                                assessmentPatientDetails.postSuccess(data = entity)
                            }
                        } else {
                            assessmentPatientDetails.postError()
                        }
                    }
                } catch (e: Exception) {
                    assessmentPatientDetails.postError()
                }
            }
        } else {
            assessmentPatientDetails.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getDiagnosisList() {
        try {
            viewModelScope.launch(dispatcherIO) {
                val res = medicalReviewRepo.getDiagnosisList()
                if (res.isNotEmpty()) {
                    diagnosisList = ArrayList(res)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun clearRedRisk(
        context: Context,
        comments: String,
    ) {
        viewModelScope.launch(dispatcherIO) {
            clearRedRiskResponse.postLoading()
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    val request = HashMap<String, Any>()
                    request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId
                    request[getComments()] =
                        comments
                    val response = medicalReviewRepo.clearRedRisk(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            val resultMap = HashMap<String, Any>()
                            res.message?.let { successMessage ->
                                resultMap[DefinedParams.MESSAGE] = successMessage
                            }
                            clearRedRiskResponse.postSuccess(resultMap)
                        } else {
                            clearRedRiskResponse.postError()
                        }
                    } else {
                        clearRedRiskResponse.postError()
                    }
                } else {
                    clearRedRiskResponse.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                clearRedRiskResponse.postError()
            }
        }
    }

    private fun getComments(): String = DefinedParams.RED_RISK_COMMENTS

    fun getInstructions(context: Context) {
        viewModelScope.launch(dispatcherIO) {
            instructionModelResponse.postLoading()
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    val response = medicalReviewRepo.getInstructions()
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            val resultMap = HashMap<String, Any>()
                            res.message?.let { successMessage ->
                                resultMap[DefinedParams.MESSAGE] = successMessage
                            }
                            instructionModelResponse.postSuccess(res.entity)
                        } else {
                            instructionModelResponse.postError()
                        }
                    } else {
                        instructionModelResponse.postError()
                    }
                } else {
                    instructionModelResponse.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                instructionModelResponse.postError()
            }
        }
    }

    fun updatePregnancyRisk(
        context: Context,
        isOn: Boolean,
    ) {
        viewModelScope.launch(dispatcherIO) {
            updatePregnancyRisk.postLoading()
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    val response = medicalReviewRepo.updatePregnancyRisk(
                        PregnancyRiskUpdate(
                            isOn,
                            patientTrackId,
                        ),
                    )
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            updatePregnancyRisk.postSuccess(isOn)
                        } else {
                            updatePregnancyRisk.postError()
                        }
                    } else {
                        updatePregnancyRisk.postError()
                    }
                } else {
                    updatePregnancyRisk.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                updatePregnancyRisk.postError()
            }
        }
    }

    fun fetchSessionQuestions(
        context: Context,
        session: Int,
        age: Int,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    sessionQuestions.postLoading()
                    val response = medicalReviewRepo.getSessionQuestions(session = session, age = age)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            sessionQuestions.postSuccess(
                                response.body()?.entity?.let {
                                    it
                                },
                            )
                        } else {
                            sessionQuestions.postError()
                        }
                    } else {
                        sessionQuestions.postError()
                    }
                } catch (e: Exception) {
                    sessionQuestions.postError()
                }
            }
        } else {
            sessionQuestions.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getPatientFamilyOtherDetails(
        enrollmentId: Long?,
        patientTrackId: Long,
        context: Context,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    enrollmentId?.let {
                        patientFamilyDetails.postLoading()
                        val response = medicalReviewRepo.getPatientFamilyOtherDetails(
                            Lifestyle(
                                id = it,
                                patientTrackId = patientTrackId,
                            ),
                        )
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                patientFamilyDetails.postSuccess(res.entity)
                            } else {
                                patientFamilyDetails.postError()
                            }
                        } else {
                            patientFamilyDetails.postError()
                        }
                    }
                } catch (e: Exception) {
                    patientFamilyDetails.postError()
                }
            }
        } else {
            patientFamilyDetails.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun getShortageReasonList(type: String) {
        viewModelScope.launch(dispatcherIO) {
            try {
                shortageReasonList.postLoading()
                shortageReasonList.postSuccess(medicalReviewRepo.getShortageReasonList(type))
            } catch (e: Exception) {
                shortageReasonList.postError()
            }
        }
    }

    fun postSessionTerminate(
        request: TerminateSessionModel,
        context: Context,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    postTerminateSession.postLoading()
                    val response = medicalReviewRepo.postSessionTerminate(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            postTerminateSession.postSuccess(res.entity)
                        } else {
                            postTerminateSession.postError()
                        }
                    } else {
                        postTerminateSession.postError()
                    }
                } catch (e: Exception) {
                    postTerminateSession.postError()
                }
            }
        } else {
            postTerminateSession.setError(context.getString(R.string.no_internet_error))
        }
    }

    fun fetchOUSiteList() {
        viewModelScope.launch(dispatcherIO) {
            ouSitesLiveData.postLoading()
//            try {
//                SecuredPreference.getSelectedSiteEntity()?.id?.let { currentSiteId ->
//                    val ouSiteList = medicalReviewRepo.getOperatingUnitSites(currentSiteId)
//                    val siteList = Resource(ResourceState.SUCCESS, ArrayList(ouSiteList))
//                    ouSitesLiveData.postValue(siteList)
//                }
//            } catch (e: Exception) {
//                ouSitesLiveData.postValue(Resource(ResourceState.ERROR))
//            }
        }
    }

    fun getRecommendation(
        context: Context,
        requestModel: MedicationSearchReqModel,
        isDefaultList: Boolean = false,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                recommendationResponse.postLoading()
                try {
                    val response = medicalReviewRepo.medicationList(requestModel)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            recommendationResponse.postSuccess(res.entityList ?: ArrayList())
                        } else {
                            recommendationResponse.postError()
                        }
                    } else {
                        recommendationResponse.postError()
                    }
                } catch (e: Exception) {
                    recommendationResponse.postError()
                }
            }
        } else {
            recommendationResponse.setError(context.getString(R.string.no_internet_error))
        }
    }
}
