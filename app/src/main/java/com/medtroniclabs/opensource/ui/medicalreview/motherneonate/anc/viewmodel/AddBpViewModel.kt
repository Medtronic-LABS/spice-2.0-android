package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.model.BpAndWeightRequestModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddBpViewModel @Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {
    val saveBloodPressure = MutableLiveData<Resource<HashMap<String, Any>>>()
    var lastLocation: Location? = null

    fun saveBloodPressure(bpAndWeightRequestModel: BpAndWeightRequestModel) {
        viewModelScope.launch(dispatcherIO) {
            saveBloodPressure.postLoading()
            saveBloodPressure.postValue(
                motherNeonateANCRepo.createBloodPressure(
                    bpAndWeightRequestModel
                )
            )
        }
    }
}