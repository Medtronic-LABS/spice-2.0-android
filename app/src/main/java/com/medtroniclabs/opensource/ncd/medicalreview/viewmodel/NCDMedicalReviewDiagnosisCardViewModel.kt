package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetRequest
import com.medtroniclabs.opensource.ncd.data.NCDDiagnosisGetResponse
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDMedicalReviewDiagnosisCardViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : ViewModel() {
    val getConfirmDiagonsis = MutableLiveData<Resource<NCDDiagnosisGetResponse>>()
    fun getConfirmDiagonsis(request: NCDDiagnosisGetRequest) {
        viewModelScope.launch(dispatcherIO) {
            getConfirmDiagonsis.postLoading()
            getConfirmDiagonsis.postValue(ncdMedicalReviewRepository.getConfirmDiagonsis(request))
        }
    }
}