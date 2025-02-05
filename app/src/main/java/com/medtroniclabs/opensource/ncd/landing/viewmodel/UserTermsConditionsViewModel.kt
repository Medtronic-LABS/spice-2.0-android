package com.medtroniclabs.opensource.ncd.landing.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.TermsAndConditionsModel
import com.medtroniclabs.opensource.ncd.screening.repo.ScreeningRepository
import com.medtroniclabs.opensource.network.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserTermsConditionsViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) :
    ViewModel() {

    var updateTermsAndConditionsStatusLiveData =
        MutableLiveData<Resource<TermsAndConditionsModel>>()

    private val userTermsConditionsConsentLiveData = MutableLiveData<String>()
    val consentEntityLiveData: LiveData<String> =
        userTermsConditionsConsentLiveData.switchMap {
            screeningRepository.fetchConsentForm(it)
        }

    fun getConsentForm(formType: String) {
        userTermsConditionsConsentLiveData.value = formType
    }

    fun updateTermsAndConditionsStatus(isAccepted: Boolean) {
        val request = TermsAndConditionsModel(isTermsAndConditionsAccepted = isAccepted)
        viewModelScope.launch(dispatcherIO) {
            updateTermsAndConditionsStatusLiveData.postLoading()
            updateTermsAndConditionsStatusLiveData.postValue(
                screeningRepository.updateTermsAndConditionsStatus(
                    request
                )
            )
        }
    }
}