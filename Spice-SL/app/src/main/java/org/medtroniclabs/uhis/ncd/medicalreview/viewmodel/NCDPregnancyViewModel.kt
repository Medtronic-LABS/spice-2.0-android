package org.medtroniclabs.uhis.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.PregnancyDetailsModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.ncd.assessment.repo.NCDPregnancyRepo
import org.medtroniclabs.uhis.ncd.medicalreview.repo.NCDMedicalReviewRepository
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class NCDPregnancyViewModel @Inject constructor(
    private val ncdPregnancyRepo: NCDPregnancyRepo,
    private val ncdMedicalReviewRepository: NCDMedicalReviewRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseViewModel(dispatcherIO) {
    var id: String? = null
    var isPregnancyAncEnabledSite: Boolean = false

    val resultDiabetesHashMap = HashMap<String, Any>()
    val resultHypertensionHashMap = HashMap<String, Any>()
    val resultPregnantHashMap = HashMap<String, Any>()

    val ncdPregnancyCreateResponse = MutableLiveData<Resource<APIResponse<HashMap<String, Any>>>>()
    val ncdPregnancyDetailsResponse = MutableLiveData<Resource<PregnancyDetailsModel>>()

    private val getSymptomListByTypeForNCD = MutableLiveData<Triple<String, String, Boolean>>()
    var value: String? = null
    var yearForDiabetes: String? = null
    var yearForHypertension: String? = null

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
                isCompleted = true,
            )
            ncdPregnancyCreateResponse.postValue(ncdPregnancyRepo.ncdPregnancyCreate(requestModel))
        }
    }

    fun ncdPregnancyDetails(id: String) {
        viewModelScope.launch(dispatcherIO) {
            val request = HashMap<String, Any>().apply {
                put(DefinedParams.ID, id)
            }
            ncdPregnancyDetailsResponse.postLoading()
            ncdPregnancyDetailsResponse.postValue(
                ncdPregnancyRepo.ncdPregnancyDetails(request),
            )
        }
    }

    val getSymptomListByTypeForNCDLiveData = getSymptomListByTypeForNCD.switchMap {
        ncdMedicalReviewRepository.getNCDDiagnosisList(listOf(it.first), it.second, it.third)
    }

    fun getSymptoms(
        type: String,
        gender: String,
        isPregnant: Boolean,
    ) {
        getSymptomListByTypeForNCD.value = Triple(type, gender, isPregnant)
    }
}
