package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class NCDClinicalNotesViewModel @Inject constructor() : ViewModel() {
    var comments: String = ""
}