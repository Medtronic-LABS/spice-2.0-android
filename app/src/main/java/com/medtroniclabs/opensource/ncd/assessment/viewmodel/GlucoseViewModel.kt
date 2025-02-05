package com.medtroniclabs.opensource.ncd.assessment.viewmodel

import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.assessment.repo.GlucoseRepo
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.assessment.AssessmentDefinedParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GlucoseViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val glucoseRepo: GlucoseRepo
) : BaseViewModel(dispatcherIO) {
    var glucoseLogCreateResponseLiveData = SingleLiveEvent<Resource<APIResponse<HashMap<String, Any>>>>()
    var glucoseLogListResponseLiveData = SingleLiveEvent<Resource<BPBGListModel>>()

    fun glucoseLogCreate(
        hashMap: HashMap<String, Any>,
        patientDetails: PatientListRespModel,
        menuId: String?
    ) {
        hashMap.apply {
            with(patientDetails) {
                NCDMRUtil.getBioDataBioMetrics(hashMap, this, isGlucose = true)
                id?.let { requestRelatedPersonFhirId ->
                    put(DefinedParams.RelatedPersonFhirId, requestRelatedPersonFhirId)
                }
                patientId?.let { requestPatientId ->
                    put(DefinedParams.PATIENT_ID, requestPatientId)
                }
            }
            put(AssessmentDefinedParams.assessmentProcessType, CommonUtils.requestFrom())
            put(DefinedParams.AssessmentOrganizationId, SecuredPreference.getOrganizationFhirId())
            put(DefinedParams.Provenance, ProvanceDto())
        }
        viewModelScope.launch(dispatcherIO) {
            glucoseLogCreateResponseLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDBloodGlucoseCreation + " " + menuId,
                isCompleted = true
            )
            glucoseLogCreateResponseLiveData.postValue(glucoseRepo.glucoseLogCreate(hashMap))
        }
    }

    fun glucoseLogList(patientId: String) {
        val request = BPBGListModel().apply {
            limit = 10
            skip = 0
            memberId = patientId
            latestRequired = true
            sortOrder = -1
        }
        viewModelScope.launch(dispatcherIO) {
            glucoseLogListResponseLiveData.postLoading()
            glucoseLogListResponseLiveData.postValue(glucoseRepo.glucoseLogList(request))
        }
    }
}