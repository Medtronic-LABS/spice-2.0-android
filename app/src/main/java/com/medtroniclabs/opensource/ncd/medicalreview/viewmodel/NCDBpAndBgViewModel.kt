package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.assessment.repo.BloodPressureRepo
import com.medtroniclabs.opensource.ncd.assessment.repo.GlucoseRepo
import com.medtroniclabs.opensource.ncd.data.BPBGListModel
import com.medtroniclabs.opensource.ncd.data.BPLogList
import com.medtroniclabs.opensource.ncd.data.GraphModel
import com.medtroniclabs.opensource.network.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDBpAndBgViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val bloodPressureRepo: BloodPressureRepo,
    private val glucoseRepo: GlucoseRepo,
) : ViewModel() {
    var totalBPTotalCount: Int? = null
    var selectedBGDropDown = MutableLiveData<Int>()
    var latestBpLogResponse: BPLogList? = null
    var totalBGCount: Int = 0
    var totalBPCount: Int = 0
    var bpLogListResponseLiveData = MutableLiveData<Resource<BPBGListModel>>()
    var glucoseLogListResponseLiveData = MutableLiveData<Resource<BPBGListModel>>()
    val onBPValueSelectedObserver = MutableLiveData<GraphModel>()
    val onBGValueSelectedObserver = MutableLiveData<GraphModel>()
    fun bpLogList(request: BPBGListModel) {
        viewModelScope.launch(dispatcherIO) {
            bpLogListResponseLiveData.postLoading()
            bpLogListResponseLiveData.postValue(bloodPressureRepo.bpLogList(request))
        }
    }

    fun glucoseLogList(request: BPBGListModel,forward: Boolean? = null) {
        viewModelScope.launch(dispatcherIO) {
            glucoseLogListResponseLiveData.postLoading()
            glucoseLogListResponseLiveData.postValue(glucoseRepo.glucoseLogList(request))
        }
    }
}