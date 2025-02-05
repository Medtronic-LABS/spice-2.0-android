package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.data.PregnancyDetailsModel
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.assessment.repo.NCDPregnancyRepo
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPregnancyViewModel @Inject constructor(
    private val ncdPregnancyRepo: NCDPregnancyRepo,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {
    var id: String? = null
    var isPregnancyAncEnabledSite: Boolean = false

    val resultDiabetesHashMap = HashMap<String, Any>()
    val resultHypertensionHashMap = HashMap<String, Any>()
    val resultPregnantHashMap = HashMap<String, Any>()

    val ncdPregnancyCreateResponse = MutableLiveData<Resource<APIResponse<HashMap<String, Any>>>>()
    val ncdPregnancyDetailsResponse = MutableLiveData<Resource<PregnancyDetailsModel>>()

    private val getSymptomListByTypeForNCD = MutableLiveData<Triple<String,String,Boolean>>()
    var value: String? = null
    var yearForDiabetes :String? = null
    var yearForHypertension :String? = null

    var relatedPersonFhirId: String? = null

    var ncdPregnancyCreateModel: PregnancyDetailsModel = PregnancyDetailsModel()

    init {
        isPregnancyAncEnabledSite = SecuredPreference.isAncEnabled()
    }

    fun ncdPregnancyCreate(requestModel: PregnancyDetailsModel) {
        viewModelScope.launch(dispatcherIO) {
            ncdPregnancyCreateResponse.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPatientHistoryCreationForMaternalHealth,
                isCompleted = true
            )
            ncdPregnancyCreateResponse.postValue(ncdPregnancyRepo.ncdPregnancyCreate(requestModel))
        }
    }

    fun ncdPregnancyDetails(id: String) {
        viewModelScope.launch(dispatcherIO) {
            val request = HashMap<String, Any>().apply {
                put(DefinedParams.id, id)
            }
            ncdPregnancyDetailsResponse.postLoading()
            ncdPregnancyDetailsResponse.postValue(
                ncdPregnancyRepo.ncdPregnancyDetails(request)
            )
        }
    }

    val getSymptomListByTypeForNCDLiveData = getSymptomListByTypeForNCD.switchMap {
        ncdMedicalReviewRepository.getNCDDiagnosisList(listOf(it.first), it.second, it.third)
    }
    fun getSymptoms(type: String, gender: String, isPregnant: Boolean) {
        getSymptomListByTypeForNCD.value = Triple(type, gender, isPregnant)
    }
}