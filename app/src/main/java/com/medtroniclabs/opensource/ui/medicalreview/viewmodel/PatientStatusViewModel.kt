package com.medtroniclabs.opensource.ui.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.PatientStatusResponse
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.PatientStatusRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PatientStatusViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO : CoroutineDispatcher,
    private val patientStatusRepository: PatientStatusRepository
): ViewModel(){
    @Inject
    lateinit var connectivityManager: ConnectivityManager
    val patientStatusLiveData = MutableLiveData<Resource<PatientStatusResponse>>()
    //The below id represent backend generated ID from patient details response, its not a member generated ID
    var patientId:String? = null

    fun getPatientStatusDetails(patientDetails: PatientListRespModel, diagnosisType: String) {
        viewModelScope.launch(dispatcherIO) {
            patientStatusLiveData.postLoading()
            patientStatusLiveData.postValue(patientStatusRepository.getPatientStatusDetails(patientDetails, diagnosisType))
        }
    }
}