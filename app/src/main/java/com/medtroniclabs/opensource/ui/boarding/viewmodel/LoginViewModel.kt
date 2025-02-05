package com.medtroniclabs.opensource.ui.boarding.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.LoginResponse
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.DeviceInformation
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.AssessmentRepository
import com.medtroniclabs.opensource.repo.FollowUpRepository
import com.medtroniclabs.opensource.repo.HouseHoldRepository
import com.medtroniclabs.opensource.ui.boarding.repo.LoginRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginRepository: LoginRepository,
    private val houseHoldRepository: HouseHoldRepository,
    private val assessmentRepository: AssessmentRepository,
    private val followUpRepository: FollowUpRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
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
        password: String
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

}