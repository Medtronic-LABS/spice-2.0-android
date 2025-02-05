package com.medtroniclabs.opensource.ui.landing.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.UserProfile
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.MenuEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferUpdateRequest
import com.medtroniclabs.opensource.ncd.data.PatientTransferListResponse
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.boarding.repo.MetaRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    val menuListLiveData = MutableLiveData<Resource<List<MenuEntity>>>()
    val userProfileLiveData = MutableLiveData<Resource<UserProfile>>()
    val userHealthFacilityLiveData = MutableLiveData<Resource<ArrayList<HealthFacilityEntity>>>()
    val patientListResponse = MutableLiveData<Resource<PatientTransferListResponse>>()
    val patientTransferNotificationCountResponse =
        MutableLiveData<Resource<NCDPatientTransferNotificationCountResponse>>()

    var selectedSiteEntity: HealthFacilityEntity ?= null
    val patientUpdateResponse = SingleLiveEvent<Resource<String>>()
    var transferPatientViewId: Long? = null


    fun getMenus() {
        viewModelScope.launch(dispatcherIO) {
            menuListLiveData.postLoading()
            menuListLiveData.postValue(metaRepository.getMenu())
        }
    }

    fun getUserProfile() {
        viewModelScope.launch(dispatcherIO) {
            userProfileLiveData.postLoading()
            userProfileLiveData.postValue(metaRepository.getUserProfile())
        }
    }

    fun getUserHealthFacility() {
        viewModelScope.launch(dispatcherIO) {
            userHealthFacilityLiveData.postLoading()
            userHealthFacilityLiveData.postValue(metaRepository.getUserHealthFacility())
        }
    }

    fun getPatientListTransfer(request: NCDPatientTransferNotificationCountRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientListResponse.postLoading()
            patientListResponse.postValue(metaRepository.getPatientListTransfer(request))
        }
    }

    fun patientTransferNotificationCount(request: NCDPatientTransferNotificationCountRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientTransferNotificationCountResponse.postLoading()
            patientTransferNotificationCountResponse.postValue(metaRepository.patientTransferNotificationCount(request))
        }
    }

    fun patientTransferUpdate(request: NCDPatientTransferUpdateRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientUpdateResponse.postLoading()
            patientUpdateResponse.postValue(metaRepository.patientTransferUpdate(request))
        }
    }

}