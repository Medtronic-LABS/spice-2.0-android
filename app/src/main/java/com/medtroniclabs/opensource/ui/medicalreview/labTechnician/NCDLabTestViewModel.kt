package com.medtroniclabs.opensource.ui.medicalreview.labTechnician

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
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.RoleConstant
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
import com.medtroniclabs.opensource.model.medicalreview.InvestigationModel
import com.medtroniclabs.opensource.model.medicalreview.SearchLabTestResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDLabTestViewModel @Inject constructor(
    private val labTestRepository: NCDLabTestRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    val labTestListLiveData = MutableLiveData<Resource<ArrayList<LabTestListResponse>>>()
    val investigationListLiveData = MutableLiveData<ArrayList<InvestigationModel>>()
    val createLabTestLiveData = MutableLiveData<Resource<APIResponse<Map<String, Any>>>>()
    var encounterId: String? = null

    fun getLabTestList(data: PatientListRespModel) {
        viewModelScope.launch(dispatcherIO) {
            val patientId = if (CommonUtils.isNonCommunity() ) data.patientId else data.id
            patientId?.let { id ->
                labTestListLiveData.postLoading()
                val response = labTestRepository.getLabTestList(LabTestListRequest(id, roleName = roleName()))
                response.data?.let {
                    labTestListLiveData.postSuccess(it)
                } ?: kotlin.run {
                    labTestListLiveData.postError()
                }
            }
        }
    }

    private fun roleName(): String? {
        return when {
            CommonUtils.isNCDProvider() -> RoleConstant.PROVIDER
            CommonUtils.isLabTechnician() -> RoleConstant.LAB_TECHNICIAN
            else -> null
        }
    }

    fun addExistingLabTestListToUI(list: ArrayList<LabTestListResponse>) {
        viewModelScope.launch(dispatcherIO) {
            val investigationList = ArrayList<InvestigationModel>()
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

    fun updateLabTest(
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
                    ),
                    labTestList
                )
                setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDInvestigationResultCreation,
                    isCompleted = true
                )
                createLabTestLiveData.postLoading()
                createLabTestLiveData.postValue(labTestRepository.updateLabTest(request))
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

}