package com.medtroniclabs.opensource.ui.medicalreview.pharmacist.viewModel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DefinedParams.TYPE_REFILL
import com.medtroniclabs.opensource.data.DispensePrescriptionRequest
import com.medtroniclabs.opensource.data.DispensePrescriptionResponse
import com.medtroniclabs.opensource.data.DispenseUpdatePrescriptionRequest
import com.medtroniclabs.opensource.data.EncounterDetails
import com.medtroniclabs.opensource.data.DispenseUpdateRequest
import com.medtroniclabs.opensource.data.DispenseUpdateResponse
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.medicalreview.pharmacist.repo.NCDPharmacistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NCDPharmacistViewModel @Inject constructor(
    private var nCDPharmacistRepository: NCDPharmacistRepository,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher
) : BaseViewModel(dispatcherIO) {

    var patientVisitId: String? = null
    var memberId: String? = null
    var lastRefillVisitId: String? = null
    var patientReference: String? = null


    val prescriptionDispenseLiveData =
        MutableLiveData<Resource<ArrayList<DispensePrescriptionResponse>>>()
    val prescriptionDispenseHistoryLiveData =
        MutableLiveData<Resource<ArrayList<DispensePrescriptionResponse>>>()
    val updatePrescriptionLiveData = MutableLiveData<Resource<DispenseUpdateResponse>>()
    val shortageReasonList = MutableLiveData<Resource<List<ShortageReasonEntity>>>()

    fun getPrescriptionDispenseList(request: DispenseUpdateRequest) {
        viewModelScope.launch(dispatcherIO) {
            prescriptionDispenseLiveData.postLoading()
            prescriptionDispenseLiveData.postValue(nCDPharmacistRepository.getPrescriptionDispenseList(request))
        }
    }

    fun getDispensePrescriptionHistory(request: DispenseUpdateRequest) {
        viewModelScope.launch(dispatcherIO) {
            prescriptionDispenseHistoryLiveData.postLoading()
            prescriptionDispenseHistoryLiveData.postValue(
                nCDPharmacistRepository.getDispensePrescriptionHistory(request)
            )
        }
    }

    fun getShortageReasonList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                shortageReasonList.postLoading()
                shortageReasonList.postSuccess(
                    nCDPharmacistRepository.getShortageReasonList(
                        TYPE_REFILL
                    )
                )
            } catch (e: Exception) {
                shortageReasonList.postError()
            }
        }
    }

    fun updateDispensePrescription(
        memberId: String? = null,
        patientReference: String? = null,
        patientVisitId: String? =null,
        request: List<DispenseUpdatePrescriptionRequest>
    ) {
        viewModelScope.launch(dispatcherIO) {
            val prescriptionRequest = DispensePrescriptionRequest(
                encounter = EncounterDetails(
                    memberId = memberId,
                    patientReference = patientReference,
                    patientVisitId = patientVisitId,
                    provenance = ProvanceDto()
                ),
                prescriptions = request
            )
            updatePrescriptionLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPrescriptionUpdated,
                isCompleted = true
            )
            updatePrescriptionLiveData.postValue(
                nCDPharmacistRepository.updateDispensePrescription(
                    prescriptionRequest
                )
            )
        }
    }

}