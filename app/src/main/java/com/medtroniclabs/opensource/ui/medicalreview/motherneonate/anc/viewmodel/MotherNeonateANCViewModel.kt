package com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.viewmodel

import android.location.Location
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.data.model.MedicalReviewEncounter
import com.medtroniclabs.opensource.data.model.MotherNeonateAncRequest
import com.medtroniclabs.opensource.data.model.PatientEncounterResponse
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.MedicalReviewSummaryRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.motherneonate.anc.repo.MotherNeonateANCRepo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MotherNeonateANCViewModel @Inject constructor(
    private val motherNeonateANCRepo: MotherNeonateANCRepo,
    private val summaryRepository: MedicalReviewSummaryRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseViewModel(dispatcherIO) {
    val motherNeonateMetaResponse = MutableLiveData<Resource<Boolean>>()
    val motherNeonateCreateResponse = MutableLiveData<Resource<PatientEncounterResponse>>()
    var motherNeonateAncRequest: MotherNeonateAncRequest = MotherNeonateAncRequest()
    val summaryCreateResponse = MutableLiveData<Resource<HashMap<String,Any>>>()
    var ancVisit: Int = -1
    var patientId: String? = null
    var memberId: String? = null
    var lastLocation: Location? = null


    fun getMotherNeoNateAncStaticData() {
        viewModelScope.launch(dispatcherIO) {
            motherNeonateANCRepo.getMotherNeoNateAncStaticData(motherNeonateMetaResponse)
        }
    }

    fun createMotherNeonate(encounterId: String?, patientHouseholdId: String?,memberId: String?) {
        viewModelScope.launch(dispatcherIO) {
            try {
                motherNeonateAncRequest.apply {
                    encounter =
                        createMedicalReviewEncounter(encounterId, patientHouseholdId, memberId)
                }
                motherNeonateCreateResponse.postLoading()
                motherNeonateCreateResponse.postValue(
                    motherNeonateANCRepo.saveMotherNeonateAnc(
                        motherNeonateAncRequest
                    )
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun createMedicalReviewEncounter(
        encounterId: String?,
        patientHouseholdId: String?,
        memberId: String?
    ): MedicalReviewEncounter {
        val currentTime = DateUtils.getCurrentDateAndTime(DateUtils.DATE_FORMAT_yyyyMMddHHmmssZZZZZ)
        return MedicalReviewEncounter(
            id = encounterId,
            patientId = this@MotherNeonateANCViewModel.patientId,
            provenance = ProvanceDto(),
            memberId = memberId,
            latitude = lastLocation?.latitude ?: 0.0,
            longitude = lastLocation?.longitude ?: 0.0,
            startTime = currentTime,
            endTime = currentTime,
            householdId = patientHouseholdId,
            referred = true,
            visitNumber = ancVisit
        )
    }

    fun motherNeonateSummaryCreate(
        referralTicketType: String,
        memberId: String?,
        submitCreateId: String?,
        householdId: String?,
        patientReference: String?,
        nextVisitDate: String,
        patientStatus: String?,
        villageId: String?,
        patientId: String?,
        assessmentName: String
    ) {
        viewModelScope.launch(dispatcherIO) {
            summaryCreateResponse.postLoading()
            if (patientId != null && memberId != null && patientStatus != null  && villageId != null && patientReference != null && submitCreateId != null) {
                val response = summaryRepository.createSummarySubmit(
                    patientId = patientId,
                    patientReference = patientReference,
                    memberId = memberId,
                    id = submitCreateId,
                    patientStatus = patientStatus,
                    nextVisitDate = nextVisitDate,
                    referralTicketType = referralTicketType,
                    assessmentName = assessmentName,
                    householdId = householdId,
                    villageId = villageId
                )
                summaryCreateResponse.postValue(response)
            }
        }
    }

    fun getSubmitCreateId(): String? {
        return motherNeonateCreateResponse.value?.data?.encounterId
    }

    fun getPatientReference(): String? {
        return motherNeonateCreateResponse.value?.data?.patientReference
    }
}
