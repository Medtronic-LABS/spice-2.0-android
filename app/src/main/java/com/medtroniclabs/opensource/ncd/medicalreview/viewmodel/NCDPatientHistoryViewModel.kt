package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.NCDPatientStatusRequest
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPatientHistoryViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository
) : BaseViewModel(dispatcherIO) {

    var patientStatusId: String? = null
    var id: String? = null
    val resultDiabetesHashMap = HashMap<String, Any>()
    val resultHypertensionHashMap = HashMap<String, Any>()
    var value: String? = null
    private val getSymptomListByTypeForNCD = MutableLiveData<Triple<String,String,Boolean>>()
    val createNCDPatientStatus = MutableLiveData<Resource<HashMap<String, Any>>>()
    var yearForDiabetes :String? = null
    var yearForHypertension :String? = null

    val getSymptomListByTypeForNCDLiveData = getSymptomListByTypeForNCD.switchMap {
        ncdMedicalReviewRepository.getNCDDiagnosisList(listOf(it.first), it.second, it.third)
    }

    fun getSymptoms(type: String, gender: String, isPregnant: Boolean) {
        getSymptomListByTypeForNCD.value = Triple(type, gender, isPregnant)
    }

    fun createNCDPatientStatus(request: NCDPatientStatusRequest) {
        viewModelScope.launch(dispatcherIO) {
            createNCDPatientStatus.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPatientHistoryCreationForNCD,
                isCompleted = true
            )
            createNCDPatientStatus.postValue(ncdMedicalReviewRepository.createNCDPatientStatus(request))
        }
    }
}