package com.medtroniclabs.opensource.ui.medicalreview.abovefiveyears

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ClinicalNotesViewModel @Inject constructor() : ViewModel() {
    var enteredClinicalNotes = ""
    var isMotherPnc: Boolean = false

    private val _submitButtonStateLiveData = MutableLiveData<Boolean>()
    val submitButtonStateLiveData: LiveData<Boolean>
        get() = _submitButtonStateLiveData


    fun handleSubmitButtonState() {
        _submitButtonStateLiveData.value = true
    }

}