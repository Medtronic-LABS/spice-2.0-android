package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.MultipartBody
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.DosageFrequency
import org.medtroniclabs.uhis.data.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.PrescriptionListRequest
import org.medtroniclabs.uhis.data.ResponseDataModel
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.data.UpdateMedicationModel
import org.medtroniclabs.uhis.data.registration.PatientPrescriptionHistoryResponse
import org.medtroniclabs.uhis.data.registration.PrescriptionModel
import org.medtroniclabs.uhis.data.registration.PrescriptionPredictionResponse
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.model.ReferralDetailRequest
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class PrescriptionCopyViewModel @Inject constructor(
    private val medicalReviewRepo: MedicalReviewRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel() {
    val prescriptionListLiveDate = MutableLiveData<Resource<ArrayList<PrescriptionModel>>>()
    val disContinuedPrescriptionListLiveDate =
        MutableLiveData<Resource<ArrayList<PrescriptionModel>>>()
    var patientTrackId: Long? = null
    var patientVisitId: Long? = null
    var tenantId: Long? = null
    var patientReference: String? = null
    var prescriptionUIModel: ArrayList<PrescriptionModel>? = null
    val removePrescriptionLiveDate = MutableLiveData<Resource<ResponseDataModel>>()
    var savePrescriptionList: ArrayList<UpdateMedicationModel>? = null
    val updatePrescriptionLiveDate = MutableLiveData<Resource<ResponseDataModel>>()
    val frequencyList = MutableLiveData<List<DosageFrequency>>()
    val medicationHistoryLiveData = MutableLiveData<Resource<PatientPrescriptionHistoryResponse>>()
    val reloadInstruction = MutableLiveData<Boolean>()
    val unitList = MutableLiveData<List<UnitMetricEntity>>()
    val prescriptionPredictionResponseLiveDate =
        MutableLiveData<Resource<PrescriptionPredictionResponse>>()

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    fun getPrescriptionList(isDiscontinuedMedicationList: Boolean) {
        prescriptionListLiveDate.postLoading()
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = medicalReviewRepo.getPrescriptionList(
                    PrescriptionListRequest(
                        patientReference = patientReference,
                        isActive = !isDiscontinuedMedicationList,
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    prescriptionListLiveDate.postSuccess(
                        NurseFhirMapper.mapPrescriptionList(response.body()?.entity),
                    )
                } else {
                    prescriptionListLiveDate.postError()
                }
            } catch (e: Exception) {
                prescriptionListLiveDate.postError()
            }
        }
    }

    fun getDiscountinuedPrescriptionList(isDiscontinuedMedicationList: Boolean) {
        disContinuedPrescriptionListLiveDate.postLoading()
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = medicalReviewRepo.getPrescriptionList(
                    PrescriptionListRequest(
                        patientReference = patientReference,
                        isActive = !isDiscontinuedMedicationList,
                    ),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    disContinuedPrescriptionListLiveDate.postSuccess(
                        NurseFhirMapper.mapPrescriptionList(response.body()?.entity),
                    )
                } else {
                    disContinuedPrescriptionListLiveDate.postError()
                }
            } catch (e: Exception) {
                disContinuedPrescriptionListLiveDate.postError()
            }
        }
    }

    fun removePrescription(
        context: Context,
        prescriptionId: Long,
        discontinuedReason: String?,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            removePrescriptionLiveDate.postLoading()
            viewModelScope.launch(dispatcherIO) {
                try {
                    val response = medicalReviewRepo.removePrescription(
                        PatientPrescriptionModel(
                            id = prescriptionId,
                            patientVisitId = patientVisitId,
                            tenantId = tenantId,
                            discontinuedReason = discontinuedReason,
                            patientTrackId = patientTrackId,
                        ),
                    )
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true) {
                            removePrescriptionLiveDate.postSuccess(res.entity)
                        } else {
                            removePrescriptionLiveDate.postError()
                        }
                    } else {
                        removePrescriptionLiveDate.postError()
                    }
                } catch (e: Exception) {
                    removePrescriptionLiveDate.postError()
                }
            }
        } else {
            removePrescriptionLiveDate.postError(context.getString(R.string.no_internet_error))
        }
    }

    fun prescriptionBD(
        context: Context,
        request: PatientPrescriptionModel,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            updatePrescriptionLiveDate.postLoading()
            viewModelScope.launch(dispatcherIO) {
                try {
                    val builder = MultipartBody.Builder()
                    builder.setType(MultipartBody.FORM)
                    val dataRequest = Gson().toJson(request)
                    builder.addFormDataPart("prescriptionRequest", dataRequest)
                    val requestBody = builder.build()
                    val response: Response<APIResponse<ResponseDataModel>> =
                        medicalReviewRepo.updatePrescription(requestBody)
                    parseUpdatePrescriptionResponse(response, request)
                } catch (e: Exception) {
                    updatePrescriptionLiveDate.postError()
                }
            }
        } else {
            updatePrescriptionLiveDate.postError(context.getString(R.string.no_internet_error))
        }
    }

    private fun parseUpdatePrescriptionResponse(
        response: Response<APIResponse<ResponseDataModel>>,
        request: PatientPrescriptionModel,
    ) {
        if (response.isSuccessful) {
            val res = response.body()
            if (res?.status == true) {
                updatePrescriptionLiveDate.postSuccess()
            } else {
                updatePrescriptionLiveDate.postError()
            }
        } else {
            updatePrescriptionLiveDate.postError()
        }
    }

    fun getFrequencyList() {
        viewModelScope.launch(dispatcherIO) {
            frequencyList.postValue(medicalReviewRepo.getDosageFrequencyList())
        }
    }

    fun getMedicationHistory(prescriptionId: Long?) {
        medicationHistoryLiveData.postLoading()
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = medicalReviewRepo.getPatientPrescriptionHistoryList(
                    ReferralDetailRequest(patientReference = patientReference),
                )
                if (response.isSuccessful && response.body()?.status == true) {
                    medicationHistoryLiveData.postSuccess(
                        NurseFhirMapper.mapPrescriptionHistory(response.body()?.entity),
                    )
                } else {
                    medicationHistoryLiveData.postError()
                }
            } catch (e: Exception) {
                medicationHistoryLiveData.postError()
            }
        }
    }

    fun getDosageUnitList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                unitList.postValue(medicalReviewRepo.getUnitList(DefinedParams.PRESCRIPTION))
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }
    }

    fun getPrescriptionPrediction(patientTrackId: Long) {
        viewModelScope.launch(dispatcherIO) {
            try {
                val map = HashMap<String, Any>()
                map.put(DefinedParams.PATIENT_TRACK_ID, patientTrackId)
                val response = medicalReviewRepo.getPrescriptionPrediction(map)
                if (response.isSuccessful) {
                    response.body()?.entity?.let { data ->
                        prescriptionPredictionResponseLiveDate.postSuccess(data = data)
                    } ?: kotlin.run {
                        prescriptionPredictionResponseLiveDate.postError()
                    }
                } else {
                    prescriptionPredictionResponseLiveDate.postError()
                }
            } catch (_: Exception) {
                prescriptionPredictionResponseLiveDate.postError()
            }
        }
    }
}
