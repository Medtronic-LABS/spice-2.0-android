package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModel
import com.medtroniclabs.opensource.ncd.data.NCDTreatmentPlanModelDetails
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDTreatmentPlanRepo
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDTreatmentPlanViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdTreatmentPlanRepo: NCDTreatmentPlanRepo
) : BaseViewModel(dispatcherIO) {
    var patientReference: String? = null
    var memberReference: String? = null
    var carePlanId: String? = null

    var updateNCDTreatmentPlanLiveData =
        MutableLiveData<Resource<APIResponse<NCDTreatmentPlanModel>>>()
    var getNCDTreatmentPlanLiveData =
        MutableLiveData<Resource<APIResponse<NCDTreatmentPlanModelDetails>>>()

    var medicalReviewFrequency: TreatmentPlanEntity? = null
    var bpCheckFrequency: TreatmentPlanEntity? = null
    var bgCheckFrequency: TreatmentPlanEntity? = null
    var hba1cCheckFrequency: TreatmentPlanEntity? = null
    var choCheckFrequency: TreatmentPlanEntity? = null

    private var frequencies = MutableLiveData<Boolean>()
    val allFrequencies: LiveData<List<TreatmentPlanEntity>> =
        frequencies.switchMap { ncdTreatmentPlanRepo.getFrequencies() }

    fun getFrequencies() {
        frequencies.value = true
    }

    fun getNCDTreatmentPlan(request: NCDTreatmentPlanModelDetails) {
        viewModelScope.launch(dispatcherIO) {
            getNCDTreatmentPlanLiveData.postLoading()
            getNCDTreatmentPlanLiveData.postValue(
                ncdTreatmentPlanRepo.getNCDTreatmentPlan(
                    request
                )
            )
        }
    }

    fun updateNCDTreatmentPlan(request: NCDTreatmentPlanModel) {
        viewModelScope.launch(dispatcherIO) {
            updateNCDTreatmentPlanLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDTreatmentPlanCreation,
                isCompleted = true
            )
            updateNCDTreatmentPlanLiveData.postValue(
                ncdTreatmentPlanRepo.updateNCDTreatmentPlan(
                    request
                )
            )
        }
    }
}