package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDMedicalReviewUpdateModel
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HrioViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : BaseViewModel(dispatcherIO) {
    val nextVisitResponse = SingleLiveEvent<Resource<HashMap<String, Any>>>()
    fun ncdUpdateNextVisitDate(request: NCDMedicalReviewUpdateModel) {
        viewModelScope.launch(dispatcherIO) {
            nextVisitResponse.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDScheduleCreation,
                isCompleted = true
            )
            val response = ncdMedicalReviewRepository.ncdUpdateNextVisitDate(request)
            nextVisitResponse.postValue(response)
        }
    }

    var toTriggerPatientDetails = MutableLiveData<Boolean>()
    fun toTriggerPatientDetails() {
        toTriggerPatientDetails.value = true
    }
}