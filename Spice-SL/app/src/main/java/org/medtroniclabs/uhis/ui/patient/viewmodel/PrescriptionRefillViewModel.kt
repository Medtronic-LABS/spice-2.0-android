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
import org.medtroniclabs.uhis.common.DefinedParams.TYPE_REFILL
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.ShortageReasonEntity
import org.medtroniclabs.uhis.data.registration.FillMedicineResponse
import org.medtroniclabs.uhis.data.registration.FillPrescription
import org.medtroniclabs.uhis.data.registration.FillPrescriptionListResponse
import org.medtroniclabs.uhis.data.registration.FillPrescriptionRequest
import org.medtroniclabs.uhis.data.registration.FillPrescriptionUpdateRequest
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.registration.PrescriptionRefillHistoryResponse
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.TYPE_DELETE
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import javax.inject.Inject
import kotlin.compareTo

@HiltViewModel
class PrescriptionRefillViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    var patientTrackId: Long? = null
    var patientVisitId: Long? = null
    var lastRefillVisitId: String? = null
    var tenantId: Long? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    val patientRefillMedicationList =
        MutableLiveData<Resource<ArrayList<FillPrescriptionListResponse>>>()

    val fillPrescriptionUpdateRequest = MutableLiveData<Resource<ArrayList<FillMedicineResponse>>>()

    val shortageReasonList = MutableLiveData<Resource<List<ShortageReasonEntity>>>()

    val patientRefillHistoryList =
        MutableLiveData<Resource<ArrayList<PrescriptionRefillHistoryResponse>>>()

    val transferReasonList = MutableLiveData<List<ShortageReasonEntity>>()
    val deleteReasonList = MutableLiveData<List<ShortageReasonEntity>>()

    var shortageReasonMap: ArrayList<FillMedicineResponse>? = null

    fun getPatientRefillMedicationList(request: FillPrescriptionRequest) {
        viewModelScope.launch(dispatcherIO) {
            try {
                patientRefillMedicationList.postLoading()
                val response = medicalReviewRepo.getPatientFillPrescriptionList(request)
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        patientRefillMedicationList.postSuccess(res.entityList ?: ArrayList())
                    } else {
                        patientRefillMedicationList.postError()
                    }
                } else {
                    patientRefillMedicationList.postError()
                }
            } catch (e: Exception) {
                patientRefillMedicationList.postError()
            }
        }
    }

    fun fillPrescriptionUpdate(
        context: Context,
        patientTrackId: Long,
        tenantId: Long,
        patientVisitId: Long,
        prescriptions: ArrayList<FillPrescription>,
    ) {
        viewModelScope.launch(dispatcherIO) {
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    val request = FillPrescriptionUpdateRequest(
                        patientTrackId,
                        tenantId,
                        patientVisitId,
                        prescriptions,
                    )
                    fillPrescriptionUpdateRequest.postLoading()
                    val response = medicalReviewRepo.fillPrescriptionUpdate(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            fillPrescriptionUpdateRequest.postSuccess(res.entityList)
                        } else {
                            fillPrescriptionUpdateRequest.postError()
                        }
                    } else {
                        fillPrescriptionUpdateRequest.postError(
                            StringConverter.getErrorMessage(
                                response.errorBody(),
                            ),
                        )
                    }
                } else {
                    fillPrescriptionUpdateRequest.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                fillPrescriptionUpdateRequest.postError()
            }
        }
    }

    fun getFillPrescriptionList(): ArrayList<FillPrescription> {
        val list: ArrayList<FillPrescription> = ArrayList()
        val refillList: List<FillPrescriptionListResponse>? = patientRefillMedicationList.value?.data?.filter { it.prescriptionFilledDays != 0 }
        refillList?.forEach { data ->
            data.prescriptionFilledDays?.let { filledDays ->
                list.add(
                    FillPrescription(
                        data.id,
                        data.prescription,
                        filledDays,
                        data.reason,
                        data.otherReasonDetail,
                        data.instructionModified ?: data.instructionNote,
                        data.instructionUpdated,
                        productNumber = data.productNumber,
                        dosageFrequencyName = data.dosageFrequencyName,
                        medicationName = data.medicationName,
                    ),
                )
            }
        }
        return list
    }

    fun getShortageReasonList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                shortageReasonList.postLoading()
                shortageReasonList.postSuccess(medicalReviewRepo.getShortageReasonList(TYPE_REFILL))
            } catch (e: Exception) {
                shortageReasonList.postError()
            }
        }
    }

    fun getPrescriptionRefillHistory(
        context: Context,
        request: PatientPrescriptionModel,
    ) {
        viewModelScope.launch(dispatcherIO) {
            try {
                if (connectivityManager.isNetworkAvailable()) {
                    patientRefillHistoryList.postLoading()
                    val response = medicalReviewRepo.getPrescriptionRefillHistory(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            patientRefillHistoryList.postSuccess(res.entityList ?: ArrayList())
                        } else {
                            patientRefillHistoryList.postError()
                        }
                    } else {
                        patientRefillHistoryList.postError()
                    }
                } else {
                    patientRefillHistoryList.postError(context.getString(R.string.no_internet_error))
                }
            } catch (e: Exception) {
                patientRefillHistoryList.postError()
            }
        }
    }

    fun getDeleteReasonList() {
        viewModelScope.launch(dispatcherIO) {
            val deleteList = medicalReviewRepo.getShortageReasonList(TYPE_DELETE)
            val list = ArrayList(deleteList)
            if (list.isNotEmpty()) {
                val itemIndex =
                    list.indexOfFirst { it.name.contains(DefinedParams.OTHER, ignoreCase = true) }
                if (itemIndex >= 0 && (itemIndex + 1) != list.size) {
                    val item = list.removeAt(itemIndex)
                    list.add(item)
                }
            }
            deleteReasonList.postValue(list)
        }
    }
}
