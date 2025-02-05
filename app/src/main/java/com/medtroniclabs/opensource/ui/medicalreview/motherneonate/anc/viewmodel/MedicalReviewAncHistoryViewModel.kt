package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.MotherNeonateAncSummaryModel
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MedicalReviewAncHistoryViewModel @Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {
    val motherNeonateAncSummary = MutableLiveData<Resource<MotherNeonateAncSummaryModel>>()

    fun getMedicalReviewAncHistory(id: String?,fhirId: String?) {
        viewModelScope.launch(dispatcherIO) {
            motherNeonateAncSummary.postLoading()
            motherNeonateAncSummary.postValue(
                motherNeonateANCRepo.fetchSummaryDetails(
                    MotherNeonateAncRequest(id, previousHistory = true, patientReference = fhirId)
                )
            )
        }
    }
}