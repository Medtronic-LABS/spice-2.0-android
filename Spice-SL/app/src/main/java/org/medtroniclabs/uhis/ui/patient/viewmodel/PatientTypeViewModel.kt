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
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import javax.inject.Inject

@HiltViewModel
class PatientTypeViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    var patientTypeMap = MutableLiveData<Resource<Boolean>>()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    fun updatePatientType(
        context: Context,
        request: HashMap<String, Any>,
    ) {
        viewModelScope.launch(dispatcherIO) {
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    patientTypeMap.postLoading()
                    val response = medicalReviewRepo.updatePatientType(request)
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body?.status == true) {
                            patientTypeMap.postSuccess()
                        } else {
                            patientTypeMap.postError(body?.message)
                        }
                    } else {
                        patientTypeMap.postError(
                            StringConverter.getErrorMessage(
                                response.errorBody(),
                            ),
                        )
                    }
                } else {
                    patientTypeMap.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                patientTypeMap.postError()
            }
        }
    }
}
