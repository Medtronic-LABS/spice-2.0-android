package com.medtroniclabs.opensource.ui.medicalreview.undertwomonths.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.medicalreview.CreateUnderTwoMonthsResponse
import com.medtroniclabs.opensource.model.medicalreview.SummaryDetails
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.repo.UnderTwoMonthsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UnderTwoMonthsTreatmentSummaryViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private var repository : UnderTwoMonthsRepository
) : ViewModel() {
    val summaryDetailsLiveData = MutableLiveData<Resource<SummaryDetails>>()
    var nextVisitDate: String? = null
    var selectedPatientStatus: String? = null
    val checkSubmitBtn = MutableLiveData<Boolean>()
    val summaryMetaListItems = MutableLiveData<Resource<List<MedicalReviewMetaItems>>>()

    fun getUnderTwoMonthsSummaryDetails(request: CreateUnderTwoMonthsResponse) {
        summaryDetailsLiveData.postValue(Resource(state = ResourceState.SUCCESS, data = null))// Clear and post loading state
        viewModelScope.launch(dispatcherIO) {
                  summaryDetailsLiveData.postLoading()
                  summaryDetailsLiveData.postValue(
                      repository.getMedicalReviewForUnderTwoMonths(
                          request
                      )
                  )

          }
    }
    fun getSummaryListMetaItems(type: String) {
        viewModelScope.launch(dispatcherIO) {
            summaryMetaListItems.postLoading()
            summaryMetaListItems.postValue(repository.getSummaryDetailMetaItems(type))
        }
    }

    fun setSubmitBtn() {
        checkSubmitBtn.value = true
    }
}

