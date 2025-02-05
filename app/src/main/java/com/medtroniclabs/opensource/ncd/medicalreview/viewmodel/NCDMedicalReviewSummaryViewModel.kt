package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.MRSummaryResponse
import com.medtroniclabs.opensource.ncd.data.MedicalReviewResponse
import com.medtroniclabs.opensource.ncd.data.NCDMRSummaryRequestResponse
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDMedicalReviewSummaryViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : BaseViewModel(dispatcherIO) {
    val summaryResponse = MutableLiveData<Resource<MRSummaryResponse>>()
    val createNCDMRSummaryCreate = MutableLiveData<Resource<HashMap<String, Any>>>()
    var nextFollowupDate: String? = null
    fun fetchSummaryResponse(request: MedicalReviewResponse) {
        viewModelScope.launch(dispatcherIO) {
            summaryResponse.postLoading()
            summaryResponse.postValue(
                ncdMedicalReviewRepository.fetchNCDMRSummary(request)
            )
        }
    }

    fun createNCDMRSummaryCreate(request: NCDMRSummaryRequestResponse, menuId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            createNCDMRSummaryCreate.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDMedicalReviewSummaryCreation + " " + menuId,
                isCompleted = true
            )
            createNCDMRSummaryCreate.postValue(ncdMedicalReviewRepository.createNCDMRSummaryCreate(request))
        }
    }
}