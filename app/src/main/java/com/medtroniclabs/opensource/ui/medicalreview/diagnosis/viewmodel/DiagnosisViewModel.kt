package com.medtroniclabs.opensource.ui.medicalreview.diagnosis.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.DiagnosisDiseaseModel
import com.medtroniclabs.opensource.data.DiagnosisSaveUpdateRequest
import com.medtroniclabs.opensource.data.DiseaseCategoryItems
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.DiagnosisRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DiagnosisViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: DiagnosisRepository
) : ViewModel() {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    val diagnosisMetaList = MutableLiveData<Resource<List<DiseaseCategoryItems>>>()
    val diagnosisDetailsList = MutableLiveData<Resource<ArrayList<DiagnosisDiseaseModel>>>()
    val diagnosisSaveUpdateResponse = MutableLiveData<Resource<ArrayList<DiagnosisDiseaseModel>>>()
    var viewDiagnosis: Boolean = true
    var diagnosisType:String = ""

    fun getDiagnosisMetaList(diagnosisType: String) {
        viewModelScope.launch(dispatcherIO) {
            repository.getDiagnosisList(diagnosisMetaList, diagnosisType)
        }
    }

    fun diagnosisCreate(request: DiagnosisSaveUpdateRequest) {
            viewModelScope.launch(dispatcherIO) {
                diagnosisSaveUpdateResponse.postLoading()
                diagnosisSaveUpdateResponse.postValue(repository.saveUpdateDiagnosis(request))
            }
    }

    fun getDiagnosisDetails(request: CreateUnderTwoMonthsResponse) {
        if (connectivityManager.isNetworkAvailable()) {
            viewModelScope.launch(dispatcherIO) {
                diagnosisDetailsList.postLoading()
                diagnosisDetailsList.postValue(repository.getDiagnosisDetails(request))
            }
        }
    }
}