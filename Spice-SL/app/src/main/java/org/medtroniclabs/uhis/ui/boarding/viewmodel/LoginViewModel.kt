package org.medtroniclabs.uhis.ui.boarding.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.data.LoginResponse
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.network.DeviceInformation
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.AssessmentRepository
import org.medtroniclabs.uhis.repo.FollowUpRepository
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.ui.boarding.repo.LoginRepository
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val houseHoldRepository: HouseHoldRepository,
    private val assessmentRepository: AssessmentRepository,
    private val followUpRepository: FollowUpRepository,
    @param:IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val isNonProd: Boolean,
) : ViewModel() {
    @Inject
    lateinit var connectivityManager: ConnectivityManager

    var loginResponseLiveData = MutableLiveData<Resource<LoginResponse>>()
    val unSyncedDataCountLiveData = MutableLiveData<Int>()

    init {
        getUnSyncedDataCount()
    }

    fun doLogin(
        context: Context,
        username: String,
        password: String,
    ) {
        viewModelScope.launch(dispatcherIO) {
            loginResponseLiveData.postLoading()
            loginResponseLiveData.postValue(loginRepository.doLogin(username, password, DeviceInformation.getDeviceDetails(context)))
        }
    }

    private fun getUnSyncedDataCount() {
        viewModelScope.launch(dispatcherIO) {
            val count = houseHoldRepository.getUnSyncedHouseholdCount() +
                houseHoldRepository.getUnSyncedHouseholdMemberCount() +
                assessmentRepository.getUnSyncedAssessmentCount() +
                followUpRepository.getUnSyncedFollowUpCount()
            unSyncedDataCountLiveData.postValue(count)
        }
    }

    fun isNonProdEnv() = isNonProd
}
