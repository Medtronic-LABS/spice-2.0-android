package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.data.registration.PatientDetailsModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.PatientDetailRequest
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import javax.inject.Inject

@HiltViewModel
class NurseBioDataViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) :
    ViewModel() {
        @Inject
        lateinit var connectivityManager: ConnectivityManager

        val patientDetailsResponse = MutableLiveData<Resource<PatientDetailsModel>>()
        var isSummary: Boolean = false

        fun getPatientDetails(
            context: Context,
            request: PatientDetailsModel,
        ) {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    patientDetailsResponse.postLoading()
                    try {
                        val response = medicalReviewRepo.getPatientDetails(PatientDetailRequest(patientId = null))
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true) {
                                patientDetailsResponse.postSuccess(res.entity)
                            } else {
                                patientDetailsResponse.postError()
                            }
                        } else {
                            patientDetailsResponse.postError()
                        }
                    } catch (e: Exception) {
                        patientDetailsResponse.postError()
                    }
                }
            } else {
                patientDetailsResponse.setError(context.getString(R.string.no_internet_error))
            }
        }
    }
