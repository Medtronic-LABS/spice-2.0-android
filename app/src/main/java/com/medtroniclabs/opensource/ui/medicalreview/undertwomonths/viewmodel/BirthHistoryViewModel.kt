package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.BirthHistoryRequest
import com.medtroniclabs.opensource.data.BirthHistoryResponse
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.UnderTwoMonthsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BirthHistoryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: UnderTwoMonthsRepository
) : ViewModel() {
    val birthHistoryLiveData = MutableLiveData<Resource<BirthHistoryResponse>>()
    val lowBirthWeight=2.0

    fun getBirthHistoryDetails(patientId: String?, memberId: String?) {
        val birthHistoryRequest =
            BirthHistoryRequest(memberId = memberId, motherPatientId = patientId)
        viewModelScope.launch(dispatcherIO) {
            birthHistoryLiveData.postLoading()
            birthHistoryLiveData.postValue(
                repository.getBirthHistoryDetailsUnderTwoMonths(birthHistoryRequest)
            )
        }
    }
}