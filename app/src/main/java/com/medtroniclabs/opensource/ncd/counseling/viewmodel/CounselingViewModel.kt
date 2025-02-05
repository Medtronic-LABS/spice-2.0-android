package com.medtroniclabs.opensource.ncd.counseling.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.RoleConstant
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.medicalreview.NCDMRUtil.PatientLifestyle
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ncd.data.NCDCounselingModel
import com.medtroniclabs.opensource.ncd.data.AssessmentResultModel
import com.medtroniclabs.opensource.ncd.counseling.repo.CounselingRepo
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CounselingViewModel @Inject constructor(
    private val counselingRepo: CounselingRepo,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {
    var patientReference: String? = null
    var memberReference: String? = null
    var encounterReference: String? = null

    //Lifestyle [Provider & Nutritionist]
    var lifestyles: List<String>? = null
    var cultureLifestyles: List<String>? = null
    var clinicianNote: String? = null
    var lifestyleAssessment: String? = null
    var otherNote: String? = null

    //Psychological [Provider & Counselor]
    var clinicianNotes: ArrayList<String>? = null
    var counselorAssessment: String? = null

    var nutritionist: Boolean? = null
    var counselor: Boolean? = null

    init {
        SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }?.let { userRole ->
            nutritionist = userRole.contains(RoleConstant.NUTRITIONIST)
            counselor = userRole.contains(RoleConstant.COUNSELOR)
        }
    }

    var createAssessmentLiveData =
        SingleLiveEvent<Resource<APIResponse<NCDCounselingModel>>>()
    var updateAssessmentLiveData =
        MutableLiveData<Resource<APIResponse<HashMap<String, Any>>>>()
    var assessmentListLiveData =
        MutableLiveData<Resource<APIResponse<ArrayList<NCDCounselingModel>>>>()
    var removeAssessmentLiveData =
        MutableLiveData<Resource<APIResponse<NCDCounselingModel>>>()

    fun createAssessment(request: NCDCounselingModel, lifestyle: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            createAssessmentLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = if (lifestyle) AnalyticsDefinedParams.NCDLifestyleManagementCreation else AnalyticsDefinedParams.NCDCounselorCreation,
                isCompleted = true
            )
            createAssessmentLiveData.postValue(counselingRepo.createAssessment(request, lifestyle))
        }
    }

    fun updateAssessment(request: AssessmentResultModel, lifestyle: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            updateAssessmentLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = if (lifestyle) AnalyticsDefinedParams.NCDLifestyleManagementUpdated else AnalyticsDefinedParams.NCDCounselorUpdated,
                isCompleted = true
            )
            updateAssessmentLiveData.postValue(counselingRepo.updateAssessment(request, lifestyle))
        }
    }

    fun getAssessmentList(request: NCDCounselingModel, lifestyle: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            assessmentListLiveData.postLoading()
            assessmentListLiveData.postValue(counselingRepo.getAssessmentList(request, lifestyle))
        }
    }

    fun removeAssessment(request: NCDCounselingModel, lifestyle: Boolean) {
        viewModelScope.launch(dispatcherIO) {
            removeAssessmentLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = if (lifestyle) AnalyticsDefinedParams.NCDLifestyleManagementDelete else AnalyticsDefinedParams.NCDCounselorDelete,
                isCompleted = true
            )
            removeAssessmentLiveData.postValue(counselingRepo.removeAssessment(request, lifestyle))
        }
    }

    private val getChip = MutableLiveData<Boolean>()
    fun getChips() {
        getChip.value = true
    }

    val getChipItems: LiveData<List<NCDMedicalReviewMetaEntity>> = getChip.switchMap {
        counselingRepo.getLifestyleAssessments(category = PatientLifestyle)
    }
}