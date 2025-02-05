package com.medtroniclabs.opensource.ncd.registration.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.registration.repo.RegistrationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import javax.inject.Inject

@HiltViewModel
class TermsAndConditionsViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val registrationRepository: RegistrationRepository
) : ViewModel() {
    var patientInitial = MutableLiveData<String?>()

    private val registrationConsentLiveData = MutableLiveData<String>()
    val consentEntityLiveData: LiveData<String> =
        registrationConsentLiveData.switchMap {
            registrationRepository.fetchConsentForm(it)
        }

    fun getConsentForm(formType: String) {
        registrationConsentLiveData.value = formType
    }
}