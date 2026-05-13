package org.medtroniclabs.uhis.ui.landing.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.UserProfile
import org.medtroniclabs.uhis.db.entity.HealthFacilityEntity
import org.medtroniclabs.uhis.db.entity.MenuEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountResponse
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferUpdateRequest
import org.medtroniclabs.uhis.ncd.data.NCDSupportRequest
import org.medtroniclabs.uhis.ncd.data.PatientTransferListResponse
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationRequest
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationResponse
import org.medtroniclabs.uhis.network.SingleLiveEvent
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import javax.inject.Inject

@HiltViewModel
class LandingViewModel @Inject constructor(
    private val metaRepository: MetaRepository,
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val isNonProd: Boolean,
) : BaseViewModel(dispatcherIO) {
    val menuListLiveData = MutableLiveData<Resource<List<MenuEntity>>>()
    val userProfileLiveData = MutableLiveData<Resource<UserProfile>>()
    val userHealthFacilityLiveData = MutableLiveData<Resource<ArrayList<HealthFacilityEntity>>>()
    val patientListResponse = MutableLiveData<Resource<PatientTransferListResponse>>()
    val patientTransferNotificationCountResponse =
        MutableLiveData<Resource<NCDPatientTransferNotificationCountResponse>>()

    var selectedSiteEntity: HealthFacilityEntity? = null
    val patientUpdateResponse = SingleLiveEvent<Resource<String>>()
    var transferPatientViewId: Long? = null
    var enteredSupportReason: String? = null
    private val supportResponseMutableLiveData = SingleLiveEvent<Resource<String>>()
    val supportResponseLiveData: LiveData<Resource<String>>
        get() = supportResponseMutableLiveData

    var isSupport = false
    val cbsNotificationListResponse = MutableLiveData<Resource<ArrayList<PeerSupervisorNotificationResponse>>>()
    val cbsNotificationUpdateListResponse = MutableLiveData<Resource<ArrayList<PeerSupervisorNotificationResponse>>>()
    val cbsNotificationUpdateResponse = MutableLiveData<Resource<Unit>>()
    var notificationIsViewed = false

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

    fun createSupportRequest(request: NCDSupportRequest) {
        viewModelScope.launch(dispatcherIO) {
            supportResponseMutableLiveData.postLoading()
            patientUpdateResponse.postValue(metaRepository.createSupportRequest(request))
        }
    }

    fun getCBSNotificationList(request: PeerSupervisorNotificationRequest) {
        viewModelScope.launch(dispatcherIO) {
            supportResponseMutableLiveData.postLoading()
            cbsNotificationListResponse.postValue(metaRepository.getCBSNotificationDetails(request))
        }
    }

    fun getCBSUpdatedNotificationList(request: PeerSupervisorNotificationRequest) {
        viewModelScope.launch(dispatcherIO) {
            supportResponseMutableLiveData.postLoading()
            cbsNotificationUpdateListResponse.postValue(metaRepository.getCBSNotificationDetails(request))
        }
    }

    fun updateCBSNotification() {
        viewModelScope.launch(dispatcherIO) {
            supportResponseMutableLiveData.postLoading()
            val result = metaRepository.updateCBSNotification(PeerSupervisorNotificationRequest(ids = SecuredPreference.notificationIds))
            cbsNotificationUpdateResponse.postValue(result)
        }
    }

    /**
     * Sequentially asks the backend whether this build is still allowed to run.
     * Returns a [Resource] following the same contract as [MetaRepository.checkAppVersion].
     */
    suspend fun checkAppVersion(): Resource<String> =
        withContext(dispatcherIO) {
            metaRepository.checkAppVersion()
        }

    fun isNonProdEnv() = isNonProd
}
