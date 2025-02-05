package com.medtroniclabs.opensource.ncd.followup.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NCDFollowUpScreenedViewModel @Inject constructor() : ViewModel() {
    var triggerGetStatus = MutableLiveData<Boolean>()
    fun triggerGetStatus() {
        triggerGetStatus.postValue(true)
    }

    var searchLiveData = MutableLiveData<Boolean>()
    fun searchLiveData() {
        searchLiveData.postValue(true)
    }
}