package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.data.model.CreateLabourDeliveryRequest
import com.medtroniclabs.opensource.data.model.LabourDeliverySummaryDetails
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.labourdelivery.repo.LabourDeliveryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LabourDeliverySummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository: LabourDeliveryRepository
) : ViewModel() {
    val summaryDetailsLiveData = MutableLiveData<Resource<CreateLabourDeliveryRequest>>()
    val summaryCreateResponse = MutableLiveData<Resource<HashMap<String, Any>>>()
    var nextFollowupDate: String? = null
    var neonatePatientStatus:String?=null

    fun getLabourDeliverySummaryDetails(
        motherId: String?,
        patientReference: String?,
        childPatientReference: String?,
        neonateId: String?
    ) {
        val request = LabourDeliverySummaryDetails(
            motherId = motherId,
            patientReference = patientReference,
            childPatientReference = childPatientReference,
            neonateId = neonateId
        )
        viewModelScope.launch(dispatcherIO) {
            summaryDetailsLiveData.postLoading()
            summaryDetailsLiveData.postValue(repository.getLabourDeliverySummaryDetails(request))
        }
    }

}