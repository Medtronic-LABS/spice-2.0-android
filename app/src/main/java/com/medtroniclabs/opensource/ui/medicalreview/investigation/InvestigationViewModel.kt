package com.medtroniclabs.opensource.ui.medicalreview.investigation

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.CodeDetailsObject
import com.medtroniclabs.opensource.data.EncounterDetails
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.config.ViewType
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.formgeneration.model.FormResponse
import com.medtroniclabs.opensource.model.LabTestCreateRequest
import com.medtroniclabs.opensource.model.LabTestDetails
import com.medtroniclabs.opensource.model.LabTestListRequest
import com.medtroniclabs.opensource.model.LabTestListResponse
import com.medtroniclabs.opensource.model.LabTestResultObject
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.RemoveLabTestRequest
import com.medtroniclabs.opensource.model.medicalreview.InvestigationModel
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse
import com.medtroniclabs.opensource.model.medicalreview.SearchRequestLabTest
import com.medtroniclabs.opensource.ncd.data.PredictionRequest
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.InvestigationRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InvestigationViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val connectivityManager: ConnectivityManager,
    private val investigationRepository: InvestigationRepository
) : BaseViewModel(dispatcherIO) {

    var patientId: String? = null
    var patientReference: String? = null
    var encounterId: String? = null
    var visitId: String? = null
    var origin: String? = null

    val investigationSearchResponseListLiveData =
        MutableLiveData<Resource<ArrayList<SearchLabTestResponse>>>()

    val investigationListLiveData = MutableLiveData<ArrayList<InvestigationModel>>()

    val createLabTestLiveData = MutableLiveData<Resource<Map<String, Any>>>()

    val labTestListLiveData = MutableLiveData<Resource<ArrayList<LabTestListResponse>>>()

    val removeLabTestLiveData = MutableLiveData<Resource<Map<String, Any>>>()

    val labTestPredictionLiveData = MutableLiveData<Resource<HashMap<String, Any>>>()

    val markAsReviewedLiveData = MutableLiveData<Resource<APIResponse<HashMap<String, Any>>>>()

    fun searchInvestigationByName(searchTerm: String) {
        viewModelScope.launch(dispatcherIO) {
            try {
                investigationSearchResponseListLiveData.postLoading()
                val request = SearchRequestLabTest(searchTerm)
                val response = investigationRepository.searchInvestigationByName(request)
                response.data?.let {
                    investigationSearchResponseListLiveData.postSuccess(it)
                }
            } catch (e: Exception) {
                investigationSearchResponseListLiveData.postError()
            }
        }
    }

    fun addInvestigationModelToUI(investigationResponse: SearchLabTestResponse) {
        viewModelScope.launch(dispatcherIO) {
            val investigationList = investigationListLiveData.value ?: ArrayList()
            if (investigationResponse.formInput != null) {
                investigationList.forEach { it.dropdownState = false }
                try {
                    investigationList.add(
                        InvestigationModel(
                            investigationResponse.testName,
                            getInvestigationModelWithResult(investigationResponse),
                            SecuredPreference.getUserDetails()?.firstName ?: "",
                            SecuredPreference.getUserFhirId(),
                            DateUtils.getTodayDateDDMMYYYY(),
                            null,
                            investigationResponse.codeDetails,
                            dropdownState = true
                        )
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    investigationListLiveData.postValue(investigationList)
                }
            } else {
                investigationListLiveData.postValue(investigationList)
            }
        }
    }

    private fun getInvestigationModelWithResult(investigationResponse: SearchLabTestResponse?): FormResponse? {
        if (investigationResponse != null) {
            try {
                val formFieldsType = object : TypeToken<FormResponse>() {}.type
                val formResponse: FormResponse =
                    Gson().fromJson(investigationResponse.formInput, formFieldsType)
                return formResponse
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    fun removeInvestigationModel(investigation: InvestigationModel) {
        viewModelScope.launch(dispatcherIO) {
            if (investigation.id != null) {
                removeLabTestLiveData.postLoading()
                try {
                    setAnalyticsData(
                        UserDetail.startDateTime,
                        eventName = AnalyticsDefinedParams.NCDInvestigationDelete,
                        isCompleted = true
                    )
                    val resourceState =
                        investigationRepository.removeLabTest(RemoveLabTestRequest(investigation.id))
                    resourceState.data?.let {
                        removeLabTestLiveData.postSuccess(it)
                    } ?: kotlin.run {
                        removeLabTestLiveData.postError()
                    }
                } catch (e: Exception) {
                    removeLabTestLiveData.postError()
                }
            } else {
                val list = investigationListLiveData.value
                list?.let {
                    it.remove(investigation)
                    investigationListLiveData.postValue(it)
                }
            }
        }
    }

    fun removeInvestigationByID(id: String) {
        val list = investigationListLiveData.value
        list?.let { list ->
            list.removeIf { it.id == id }
            investigationListLiveData.postValue(list)
        }
    }


    fun createLabTest(
        resultFromInvestigation: List<InvestigationModel>?,
        patientDetail: PatientListRespModel
    ) {
        viewModelScope.launch(dispatcherIO) {
            try {
                createLabTestLiveData.postLoading()
                val labTestList = ArrayList<LabTestDetails>()
                resultFromInvestigation?.forEach { data ->
                    val detail = LabTestDetails(
                        testName = data.testName,
                        recommendedOn = data.recommendedOn,
                        recommendedBy = data.recommendedBy,
                        recommendedName = data.recommendedByName,
                        codeDetails = data.codeDetails,
                        labTestResults = getResultListObject(data),
                        id = data.id
                    )
                    labTestList.add(detail)
                }

                val request = LabTestCreateRequest(
                    EncounterDetails(
                        id = encounterId,
                        patientReference = if (CommonUtils.isNonCommunity() ) patientDetail.patientId.orEmpty() else patientDetail.id.orEmpty(),
                        patientId = if (CommonUtils.isNonCommunity() ) null else patientDetail.patientId.orEmpty(),
                        memberId = if (!CommonUtils.isNonCommunity() ) patientDetail.memberId.orEmpty() else patientDetail.id.orEmpty(),
                        provenance = ProvanceDto(),
                        visitId = visitId
                    ),
                    labTestList,
                    enrollmentType = patientDetail.enrollmentType,
                    identityValue = patientDetail.identityValue
                )
                setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDInvestigationCreation,
                    isCompleted = true
                )
                createLabTestLiveData.postValue(investigationRepository.createLabTest(request))
            } catch (e: Exception) {
                createLabTestLiveData.postError()
            }
        }
    }

    private fun getResultListObject(data: InvestigationModel): ArrayList<LabTestResultObject>? {
        val labTestResultObjectList = ArrayList<LabTestResultObject>()
        var testedOn: String? = null
        data.resultHashMap?.let { map ->
            if (map.containsKey(DefinedParams.TestedOn) && map[DefinedParams.TestedOn] is String) {
                testedOn = map[DefinedParams.TestedOn] as String
            }
            data.resultList?.formLayout?.filter { it.viewType != ViewType.VIEW_TYPE_FORM_CARD_FAMILY && it.id != DefinedParams.TestedOn }
                ?.let { formLayoutList ->
                    getValidResultObject(formLayoutList, map, testedOn)?.let {
                        labTestResultObjectList.addAll(it)
                    }
                }
        }

        return if (labTestResultObjectList.size > 0) {
            labTestResultObjectList
        } else {
            null
        }
    }

    private fun getValidResultObject(
        formLayoutList: List<FormLayout>,
        resultMap: HashMap<String, Any>,
        testedOn: String?
    ): ArrayList<LabTestResultObject>? {
        val list = ArrayList<LabTestResultObject>()
        var validResultList = true
        formLayoutList.forEach { formData ->
            if (resultMap.containsKey(formData.id)) {
                val actualValue = resultMap[formData.id]
                var unitValue: String? = null
                if (resultMap.containsKey(formData.id + DefinedParams.Unit)) {
                    val unitValueAny = resultMap[formData.id + DefinedParams.Unit]
                    if (unitValueAny is String) {
                        unitValue = unitValueAny
                    }
                }
                actualValue?.let { value ->
                    val resultObject = LabTestResultObject(
                        name = formData.id,
                        value = value,
                        SecuredPreference.getUserFhirId(),
                        getCodeDetailsObject(formData),
                        testedOn = testedOn,
                        resource = formData.resource,
                        unitValue
                    )
                    list.add(resultObject)
                }
            } else {
                validResultList = false
            }
        }
        return if (validResultList) {
            list
        } else {
            null
        }
    }

    private fun getCodeDetailsObject(formData: FormLayout): CodeDetailsObject? {
        if (formData.code != null && formData.url != null) {
            return CodeDetailsObject(formData.code!!, formData.url!!)
        }
        return null
    }


    fun getLabTestList(data: PatientListRespModel) {
        viewModelScope.launch(dispatcherIO) {
            val patientId = if (CommonUtils.isNonCommunity() ) data.patientId else data.id
            patientId?.let { id ->
                labTestListLiveData.postLoading()
                val response = investigationRepository.getLabTestList(LabTestListRequest(id))
                response.data?.let {
                    labTestListLiveData.postSuccess(it)
                } ?: kotlin.run {
                    labTestListLiveData.postError()
                }
            }
        }
    }


    fun addExistingLabTestListToUI(list: ArrayList<LabTestListResponse>) {
        viewModelScope.launch(dispatcherIO) {
            val investigationList = investigationListLiveData.value ?: ArrayList()
            list.forEach { investigationExisting ->
                investigationList.add(
                    InvestigationModel(
                        testName = investigationExisting.testName,
                        recommendedBy = investigationExisting.recommendedBy,
                        recommendedOn = investigationExisting.recommendedOn,
                        recommendedByName = investigationExisting.recommendedName,
                        labTestResultList = investigationExisting.labTestResults,
                        id = investigationExisting.id,
                        resultList = getInvestigationModelWithResult(investigationExisting.labTestCustomization),
                        dropdownState = false,
                        isReview = investigationExisting.isReview
                    )
                )
            }
            investigationListLiveData.postValue(investigationList)
        }
    }

    fun getLabTestNudgeList() {
        viewModelScope.launch(dispatcherIO) {
            labTestPredictionLiveData.postLoading()
            if (!connectivityManager.isNetworkAvailable()) {
                labTestPredictionLiveData.postError()
                return@launch
            }
            labTestPredictionLiveData.postValue(
                investigationRepository.getLabTestNudgeList(
                    predictionRequest = PredictionRequest(
                        patientReference = patientReference
                    )
                )
            )
        }
    }

    fun markAsReviewed(request: HashMap<String, Any>) {
        viewModelScope.launch(dispatcherIO) {
            markAsReviewedLiveData.postLoading()
            markAsReviewedLiveData.postValue(investigationRepository.markAsReviewed(request))
        }
    }
}