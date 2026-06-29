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
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.data.registration.InvestigationNudgesModel
import org.medtroniclabs.uhis.data.registration.LabTestListResponse
import org.medtroniclabs.uhis.data.registration.LabTestModel
import org.medtroniclabs.uhis.data.registration.LabTestSearchResponse
import org.medtroniclabs.uhis.data.registration.SearchModel
import org.medtroniclabs.uhis.db.entity.DiagnosisEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.ViewType
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.model.LabTestCreateRequest
import org.medtroniclabs.uhis.model.LabTestListRequest
import org.medtroniclabs.uhis.model.RemoveLabTestRequest
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.InvestigationRepository
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import javax.inject.Inject
import kotlin.text.set

@HiltViewModel
class LabTestViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    private val investigationRepo: InvestigationRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    var editModel: LabTestModel? = null
    val labTestResultResponse = MutableLiveData<Resource<ArrayList<HashMap<String, Any>>>>()
    val searchResponse = MutableLiveData<Resource<ArrayList<LabTestSearchResponse>?>>()
    val labTestListResponse = MutableLiveData<Resource<LabTestListResponse>>()
    val referLabTestResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val createResultResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val resultDetailsResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val removeLabTestResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    val reviewResultResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var selectedLabTest: LabTestSearchResponse? = null
    var patientTrackId: Long = -1L
    var patientVisitId: Long = -1L
    var patientReference: String? = null
    val isToRefer = MutableLiveData(false)
    var labTestLists = ArrayList<LabTestModel>()
    var labTestAddLists = ArrayList<LabTestModel>()
    var labTestUnitList = ArrayList<UnitMetricEntity>()
    var diagnosisList = ArrayList<DiagnosisEntity>()
    val nudgesList = MutableLiveData<Resource<InvestigationNudgesModel?>>()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    init {
        getLabTestUnitList()
    }

    fun searchLabTest(searchValue: String) {
        viewModelScope.launch(dispatcherIO) {
            searchResponse.postLoading()
            try {
                val request = SearchModel(searchValue = searchValue, country = SecuredPreference.getUserDetails()?.country?.id, isActive = true)
                val response = medicalReviewRepo.searchLabTest(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        searchResponse.postSuccess(data = res.entity)
                    } else {
                        searchResponse.postError()
                    }
                } else {
                    searchResponse.postError()
                }
            } catch (e: Exception) {
                searchResponse.postError()
            }
        }
    }

    fun getLabTestResults(
        context: Context,
        labTestId: Long,
        labTestName: String?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (connectivityManager.isNetworkAvailable()) {
                labTestResultResponse.postLoading()
                try {
                    val response = medicalReviewRepo.getLabTestResult(labTestId)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            handleResponseList(res.entityList, labTestName)
                        } else {
                            labTestResultResponse.postError()
                        }
                    } else {
                        labTestResultResponse.postError()
                    }
                } catch (e: Exception) {
                    labTestResultResponse.postError()
                }
            } else {
                labTestResultResponse.postError(context.getString(R.string.no_internet_error))
            }
        }
    }

    private fun handleResponseList(
        list: ArrayList<HashMap<String, Any>>?,
        labTestName: String?,
    ) {
        val resultList = ArrayList<HashMap<String, Any>>()
        if (list.isNullOrEmpty()) {
            val resultMap = HashMap<String, Any>()
            resultMap[DefinedParams.NAME] = labTestName ?: ""
            resultMap[DefinedParams.DISPLAY_ORDER] = 1
            resultList.add(resultMap)
            editModel?.patientLabtestResults = resultList
            labTestResultResponse.postSuccess(resultList)
        } else {
            list.sortBy { if (it.containsKey(DefinedParams.DISPLAY_ORDER)) (it[DefinedParams.DISPLAY_ORDER] as Number).toInt() else null }
            editModel?.patientLabtestResults = list
            labTestResultResponse.postSuccess(list)
        }
    }

    /**
     * Builds the result-entry rows from the lab test's inline form definition
     * (labTestCustomization.formInput) instead of the removed relational
     * patient-labtest/result/list endpoint. Rows are normalized through Gson so the nested
     * maps/lists match the runtime types [LabTestResultsAdapter] expects (LinkedTreeMap/Double).
     */
    fun buildLabTestResultFields(labTestName: String?) {
        val model = editModel
        if (model == null) {
            labTestResultResponse.postError()
            return
        }
        labTestResultResponse.postLoading()
        try {
            val gson = Gson()
            val rows = ArrayList<HashMap<String, Any>>()
            val formInput = model.formInput
            if (!formInput.isNullOrBlank()) {
                val formResponse = gson.fromJson(formInput, FormResponse::class.java)
                formResponse
                    ?.formLayout
                    ?.filter { it.viewType != ViewType.VIEW_TYPE_FORM_CARD_FAMILY && it.id != TESTED_ON_FIELD }
                    ?.forEachIndexed { index, field ->
                        val row = HashMap<String, Any>()
                        row[DefinedParams.NAME] = field.title.ifBlank { labTestName ?: "" }
                        row[DefinedParams.DISPLAY_ORDER] = field.orderId ?: (index + 1)
                        row[FHIR_FIELD_ID] = field.id
                        field.resource?.let { row[FHIR_RESOURCE] = it }
                        field.code?.let { row[FHIR_CODE] = it }
                        field.url?.let { row[FHIR_URL] = it }
                        val ranges = field.ranges
                        if (!ranges.isNullOrEmpty()) {
                            val rangeList = ArrayList<HashMap<String, Any>>()
                            ranges.distinctBy { it.unitType }.forEachIndexed { rIndex, range ->
                                val rangeMap = HashMap<String, Any>()
                                rangeMap[DefinedParams.ID] = (rIndex + 1).toDouble()
                                rangeMap[DefinedParams.UNIT] = range.unitType
                                rangeMap[DefinedParams.MINIMUM_VALUE] = range.minRange
                                rangeMap[DefinedParams.MAXIMUM_VALUE] = range.maxRange
                                rangeMap[DefinedParams.DISPLAY_NAME] = range.displayRange
                                rangeList.add(rangeMap)
                            }
                            row[DefinedParams.LAB_RESULT_RANGE] = rangeList
                        } else {
                            // No ranges defined: fall back to the field's unitList so the unit
                            // dropdown still populates (the adapter reads units from this list).
                            val rangeList = ArrayList<HashMap<String, Any>>()
                            field.unitList?.forEachIndexed { uIndex, unit ->
                                val unitName = (unit[DefinedParams.NAME] as? String)
                                    ?: (unit[DefinedParams.ID] as? String)
                                if (!unitName.isNullOrBlank()) {
                                    val rangeMap = HashMap<String, Any>()
                                    rangeMap[DefinedParams.ID] = (uIndex + 1).toDouble()
                                    rangeMap[DefinedParams.UNIT] = unitName
                                    rangeList.add(rangeMap)
                                }
                            }
                            if (rangeList.isNotEmpty()) {
                                row[DefinedParams.LAB_RESULT_RANGE] = rangeList
                            }
                        }
                        rows.add(row)
                    }
            }
            if (rows.isEmpty()) {
                val row = HashMap<String, Any>()
                row[DefinedParams.NAME] = labTestName ?: ""
                row[DefinedParams.DISPLAY_ORDER] = 1
                rows.add(row)
            }
            val normalizedType = object : TypeToken<ArrayList<HashMap<String, Any>>>() {}.type
            val normalized: ArrayList<HashMap<String, Any>> =
                gson.fromJson(gson.toJson(rows), normalizedType)
            model.patientLabtestResults = normalized
            labTestResultResponse.postSuccess(normalized)
        } catch (e: Exception) {
            labTestResultResponse.postError()
        }
    }

    /** Saves an entered lab test result through the FHIR investigation/create endpoint. */
    fun createLabTestResultFhir(request: LabTestCreateRequest) {
        viewModelScope.launch(dispatcherIO) {
            if (!connectivityManager.isNetworkAvailable()) {
                createResultResponse.postError()
                return@launch
            }
            createResultResponse.postLoading()
            try {
                val resource = investigationRepo.createLabTest(request)
                if (resource.state == ResourceState.SUCCESS) {
                    createResultResponse.postSuccess(HashMap(resource.data ?: emptyMap()))
                } else {
                    createResultResponse.postError(resource.message)
                }
            } catch (e: Exception) {
                createResultResponse.postError()
            }
        }
    }

    /** Removes a referred lab test through the FHIR investigation/remove endpoint. */
    fun removeLabTestFhir(model: LabTestModel) {
        viewModelScope.launch(dispatcherIO) {
            if (!connectivityManager.isNetworkAvailable()) {
                removeLabTestResponse.postError()
                return@launch
            }
            removeLabTestResponse.postLoading()
            try {
                val id = model.fhirId ?: model._id?.toString()
                if (id.isNullOrBlank()) {
                    removeLabTestResponse.postError()
                    return@launch
                }
                val resource = investigationRepo.removeLabTest(RemoveLabTestRequest(id))
                if (resource.state == ResourceState.SUCCESS) {
                    val entity = HashMap<String, Any>(resource.data ?: emptyMap())
                    entity[DefinedParams.OTHER] = model
                    removeLabTestResponse.postSuccess(entity)
                } else {
                    removeLabTestResponse.postError()
                }
            } catch (e: Exception) {
                removeLabTestResponse.postError()
            }
        }
    }

    fun getLabTestList(isFromTechnicianLogin: Boolean = false) {
        viewModelScope.launch(dispatcherIO) {
            labTestListResponse.postLoading()
            try {
                val response = medicalReviewRepo.getPatientLabTests(
                    LabTestListRequest(patientReference = patientReference ?: ""),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    labTestListResponse.postSuccess(
                        NurseFhirMapper.mapLabTestList(response.body()?.entityList, resultUpdated = true),
                    )
                } else {
                    labTestListResponse.postError()
                }
            } catch (e: Exception) {
                labTestListResponse.postError()
            }
        }
    }

    fun getNudgesList() {
        val request = HashMap<String, Any>()
        request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = medicalReviewRepo.getNudgesList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        nudgesList.postSuccess(res.entity)
                    }
                }
            } catch (e: Exception) {
                // error Block
            }
        }
    }

    fun referLabTest(
        context: Context,
        labTestList: ArrayList<LabTestModel>,
    ) {
        val request = HashMap<String, Any>()
        request[DefinedParams.LAB_TEST] = labTestList
        request[DefinedParams.PATIENT_TRACK_ID] = patientTrackId
        request[DefinedParams.PATIENT_VISIT_ID] = patientVisitId
        request[DefinedParams.TENANT_ID] = SecuredPreference.getTenantId()
        viewModelScope.launch(dispatcherIO) {
            if (connectivityManager.isNetworkAvailable()) {
                labTestListResponse.postLoading()
                try {
                    val response = medicalReviewRepo.referLabTest(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            val resultMap = HashMap<String, Any>()
                            res.message?.let { successMessage ->
                                resultMap[DefinedParams.MESSAGE] = successMessage
                            }
                            referLabTestResponse.postSuccess(resultMap)
                        } else {
                            referLabTestResponse.postError()
                        }
                    } else {
                        referLabTestResponse.postError(StringConverter.getErrorMessage(response.errorBody()))
                    }
                } catch (e: Exception) {
                    referLabTestResponse.postError()
                }
            } else {
                referLabTestResponse.postError(context.getString(R.string.no_internet_error))
            }
        }
    }

    fun createLabTestResult(
        context: Context,
        request: HashMap<String, Any>,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (connectivityManager.isNetworkAvailable()) {
                createResultResponse.postLoading()
                try {
                    val response = medicalReviewRepo.createLabTestResult(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            createResultResponse.postSuccess(res.entity)
                        } else {
                            createResultResponse.postError()
                        }
                    } else {
                        createResultResponse.postError()
                    }
                } catch (e: Exception) {
                    createResultResponse.postError()
                }
            } else {
                createResultResponse.postError(context.getString(R.string.no_internet_error))
            }
        }
    }

    fun getResultDetails(labTestId: Long) {
        val request = HashMap<String, Any>()
        request[DefinedParams.TENANT_ID] = SecuredPreference.getTenantId()
        request[DefinedParams.PATIENT_LABTEST_ID] = labTestId
        viewModelScope.launch(dispatcherIO) {
            resultDetailsResponse.postLoading()
            try {
                val response = medicalReviewRepo.getLabTestResultDetails(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        resultDetailsResponse.postSuccess(res.entity)
                    } else {
                        resultDetailsResponse.postError()
                    }
                } else {
                    resultDetailsResponse.postError()
                }
            } catch (e: Exception) {
                resultDetailsResponse.postError()
            }
        }
    }

    private fun getLabTestUnitList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                val list = medicalReviewRepo.getUnitList(DefinedParams.LABTEST)
                list.let {
                    if (it.isNotEmpty()) {
                        labTestUnitList.addAll(it)
                    }
                }
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }
    }

    fun removeLabTest(
        context: Context,
        request: HashMap<String, Any>,
        model: LabTestModel,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (connectivityManager.isNetworkAvailable()) {
                removeLabTestResponse.postLoading()
                try {
                    val response = medicalReviewRepo.removeLabTest(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            val entity = res.entity ?: HashMap()
                            entity[DefinedParams.OTHER] = model
                            removeLabTestResponse.postSuccess(entity)
                        } else {
                            removeLabTestResponse.postError()
                        }
                    } else {
                        removeLabTestResponse.postError()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    removeLabTestResponse.postError()
                }
            } else {
                removeLabTestResponse.postError(context.getString(R.string.no_internet_error))
            }
        }
    }

    fun reviewLabTestResult(
        context: Context,
        request: HashMap<String, Any>,
    ) {
        viewModelScope.launch(dispatcherIO) {
            if (connectivityManager.isNetworkAvailable()) {
                reviewResultResponse.postLoading()
                try {
                    val response = medicalReviewRepo.reviewLabTestResult(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            reviewResultResponse.postSuccess(res.entity)
                        } else {
                            reviewResultResponse.postError()
                        }
                    } else {
                        reviewResultResponse.postError()
                    }
                } catch (e: Exception) {
                    reviewResultResponse.postError()
                }
            } else {
                reviewResultResponse.postError(context.getString(R.string.no_internet_error))
            }
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

    companion object {
        // The "Tested On" date field is captured separately by the dialog, so it is excluded
        // from the generated result-entry rows.
        private const val TESTED_ON_FIELD = "TestedOn"

        // Keys used to stash FHIR field metadata on each result row so the save request can
        // rebuild the observation (name/resource/codeDetails) from the entered values.
        const val FHIR_FIELD_ID = "fhirFieldId"
        const val FHIR_RESOURCE = "fhirResource"
        const val FHIR_CODE = "fhirCode"
        const val FHIR_URL = "fhirUrl"
    }
}
