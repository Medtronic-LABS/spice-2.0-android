package com.medtroniclabs.opensource.ncd.medicalreview.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.data.history.NCDMedicalReviewHistory
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.ReferralDetailRequest
import com.medtroniclabs.opensource.model.ReferredDate
import com.medtroniclabs.opensource.ncd.data.LifeStyleResponse
import com.medtroniclabs.opensource.ncd.data.LifeStyleRequest
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.network.resource.Resource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDMedicalReviewCMRViewModel @Inject constructor(
    private var ncdMedicalReviewRepo: NCDMedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher
) : ViewModel() {

    // prescription
    var prescriptionReferralDates = MutableLiveData<List<ReferredDate>>()
    val prescriptionTicketLiveData = MutableLiveData<Resource<HistoryEntity>>()
    var patientVisitId: String? = null

    // medical review History
    var medicalReferralDates = MutableLiveData<List<ReferredDate>>()
    val medicalReviewTicketLiveData = MutableLiveData<Resource<NCDMedicalReviewHistory>>()
    var medicalVisitId: String? = null

    //Investigation
    var investigationReferralDates = MutableLiveData<List<ReferredDate>>()
    val investigationTicketLiveData = MutableLiveData<Resource<HistoryEntity>>()
    var investigationVisitId: String? = null

    val lifeStyleResponse = MutableLiveData<Resource<ArrayList<LifeStyleResponse>>>()
    fun getPrescriptionHistory(patientId: String? = null, patientVisitId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            prescriptionTicketLiveData.postLoading()
            prescriptionTicketLiveData.postValue(
                ncdMedicalReviewRepo.getPrescription(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        patientVisitId = patientVisitId,
                    )
                )
            )
        }
    }

    fun getMedicalReviewHistory(patientId: String? = null, medicalVisitId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            medicalReviewTicketLiveData.postLoading()
            medicalReviewTicketLiveData.postValue(
                ncdMedicalReviewRepo.getNCDMedicalReviewHistory(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        patientVisitId = medicalVisitId,
                        requestFrom = null
                    )
                )
            )
        }
    }


    fun getInvestigationHistory(patientReference: String? = null, investigationVisitId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            investigationTicketLiveData.postLoading()
            investigationTicketLiveData.postValue(
                ncdMedicalReviewRepo.getNCDInvestigation(
                    ReferralDetailRequest(
                        patientReference = patientReference,
                        patientVisitId = investigationVisitId,
                    )
                )
            )
        }
    }

    fun getNcdLifeStyleDetails(request: LifeStyleRequest) {
        viewModelScope.launch(dispatcherIO) {
            lifeStyleResponse.postLoading()
            val response = ncdMedicalReviewRepo.getNcdLifeStyleDetails(request)
            lifeStyleResponse.postValue(response)
        }
    }
}

