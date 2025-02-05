package com.medtroniclabs.opensource.ui.referralhistory.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.data.PncChildMedicalReview
import com.medtroniclabs.opensource.data.history.HistoryEntity
import com.medtroniclabs.opensource.data.history.MedicalReviewHistory
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.ReferralData
import com.medtroniclabs.opensource.model.ReferralDetailRequest
import com.medtroniclabs.opensource.model.ReferredDate
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewTypeEnums
import com.medtroniclabs.opensource.ui.referralhistory.repo.ReferralHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReferralHistoryViewModel @Inject constructor(
    private val referralHistoryRepository: ReferralHistoryRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    @Inject
    lateinit var connectivityManager: ConnectivityManager
    val referralTicketLiveData = MutableLiveData<Resource<ReferralData>>()
    val prescriptionTicketLiveData = MutableLiveData<Resource<HistoryEntity>>()
    val investigationTicketLiveData = MutableLiveData<Resource<HistoryEntity>>()
    val medicalReviewTicketLiveData = MutableLiveData<Resource<MedicalReviewHistory>>()
    val medicalReviewTicketLiveDataPNC = MutableLiveData<Resource<PncChildMedicalReview>>()
    var referralDates = MutableLiveData<List<ReferredDate>>()
    var prescriptionReferralDates = MutableLiveData<List<ReferredDate>>()
    var investigationReferralDates = MutableLiveData<List<ReferredDate>>()
    var medicalReferralDates = MutableLiveData<List<ReferredDate>>()
    var ticketId: String? = null
    var prescriptionTicketId: String? = null
    var investigationTicketId: String? = null
    var medicalTicketId: String? = null
    var patientReference:String?=null


    fun getReferralTicket(patientId: String? = null, ticketId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            referralTicketLiveData.postLoading()
            referralTicketLiveData.postValue(
                referralHistoryRepository.getReferralTicket(
                    ReferralDetailRequest(
                        patientId = patientId,
                        ticketId = ticketId,
                        type = MedicalReviewTypeEnums.medicalReview.name
                    )
                )
            )
        }
    }

    fun getPrescriptionHistory(patientId: String? = null, prescriptionTicketId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            prescriptionTicketLiveData.postLoading()
            prescriptionTicketLiveData.postValue(
                referralHistoryRepository.getPrescription(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        encounterId = prescriptionTicketId,
                    )
                )
            )
        }
    }

    fun getMedicalReviewHistory(patientId: String? = null, medicalTicketId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            medicalReviewTicketLiveData.postLoading()
            medicalReviewTicketLiveData.postValue(
                referralHistoryRepository.getMedicalReviewHistory(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        encounterId = medicalTicketId,
                    )
                )
            )
        }
    }fun getMedicalReviewHistoryPNC(patientId: String? = null, medicalTicketId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            medicalReviewTicketLiveDataPNC.postLoading()
            medicalReviewTicketLiveDataPNC.postValue(
                referralHistoryRepository.getMedicalReviewHistoryPNC(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        encounterId = medicalTicketId,
                    )
                )
            )
        }
    }

    fun getInvestigationHistory(patientId: String? = null, investigationTicketId: String? = null) {
        viewModelScope.launch(dispatcherIO) {
            investigationTicketLiveData.postLoading()
            investigationTicketLiveData.postValue(
                referralHistoryRepository.getInvestigation(
                    ReferralDetailRequest(
                        patientReference = patientId,
                        encounterId = investigationTicketId,
                    )
                )
            )
        }
    }
}