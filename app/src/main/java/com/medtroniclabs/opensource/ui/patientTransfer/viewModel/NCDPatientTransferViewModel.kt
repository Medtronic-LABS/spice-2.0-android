package com.medtroniclabs.opensource.ui.patientTransfer.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.patientEdit.NCDPatientEditRepository
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferValidate
import com.medtroniclabs.opensource.ncd.data.NCDRegionSiteModel
import com.medtroniclabs.opensource.ncd.data.NCDTransferCreateRequest
import com.medtroniclabs.opensource.ncd.data.RegionSiteResponse
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleModel
import com.medtroniclabs.opensource.ncd.data.NCDSiteRoleResponse
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPatientTransferViewModel @Inject constructor(
    private var ncdPatientEditRepo: NCDPatientEditRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    val validateTransferResponse = SingleLiveEvent<Resource<HashMap<String, Any>>>()
    var patientTransferResponse = SingleLiveEvent<Resource<String>>()
    val searchSiteResponse = MutableLiveData<Resource<ArrayList<RegionSiteResponse>>>()
    val searchRoleUserResponse = MutableLiveData<Resource<ArrayList<NCDSiteRoleResponse>>>()

    fun validatePatientTransfer(request: NCDPatientTransferValidate) {
        viewModelScope.launch(dispatcherIO) {
            validateTransferResponse.postLoading()
            validateTransferResponse.postValue(
                ncdPatientEditRepo.validatePatientTransfer(request)
            )
        }
    }

    fun createPatientTransfer(request: NCDTransferCreateRequest) {
        viewModelScope.launch(dispatcherIO) {
            patientTransferResponse.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPatientTransfer,
                isCompleted = true
            )
            patientTransferResponse.postValue(
                ncdPatientEditRepo.createPatientTransfer(request)
            )
        }
    }

    fun searchSite(request: NCDRegionSiteModel) {
        viewModelScope.launch(dispatcherIO) {
            searchSiteResponse.postLoading()
            searchSiteResponse.postValue(
                ncdPatientEditRepo.searchSite(request)
            )
        }
    }

    fun searchRoleUser(request: NCDSiteRoleModel) {
        viewModelScope.launch(dispatcherIO) {
            searchRoleUserResponse.postLoading()
            searchRoleUserResponse.postValue(
                ncdPatientEditRepo.searchRoleUser(request)
            )
        }
    }
}