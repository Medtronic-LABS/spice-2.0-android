package com.medtroniclabs.opensource.ui.mypatients.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.model.BpAndWeightResponse
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MotherNeonateBpWeightViewModel @Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {
    val getBloodPressure = MutableLiveData<Resource<BpAndWeightResponse>>()
    val getWeight = MutableLiveData<Resource<BpAndWeightResponse>>()
    fun fetchBloodPressure(motherNeonateAncRequest: MotherNeonateAncRequest) {
        viewModelScope.launch(dispatcherIO) {
            getBloodPressure.postLoading()
            getBloodPressure.postValue(
                motherNeonateANCRepo.fetchBloodPressure(
                    motherNeonateAncRequest
                )
            )
        }
    }

    fun fetchWeight(motherNeonateAncRequest: MotherNeonateAncRequest) {
        viewModelScope.launch(dispatcherIO) {
            getWeight.postLoading()
            getWeight.postValue(motherNeonateANCRepo.fetchWeight(motherNeonateAncRequest))
        }
    }

    fun getWeight(): Double? {
        return getWeight.value?.data?.weight
    }

    fun getBp(): BpAndWeightResponse? {
        return getBloodPressure.value?.data
    }

}