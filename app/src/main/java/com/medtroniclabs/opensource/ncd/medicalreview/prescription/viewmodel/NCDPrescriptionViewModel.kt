package com.medtroniclabs.opensource.ncd.medicalreview.prescription.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.DosageFrequency
import com.medtroniclabs.opensource.data.EncounterDetails
import com.medtroniclabs.opensource.data.MedicationResponse
import com.medtroniclabs.opensource.data.MedicationSearchRequest
import com.medtroniclabs.opensource.data.PatientPrescriptionModel
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.PrescriptionCreateRequest
import com.medtroniclabs.opensource.data.PrescriptionListRequest
import com.medtroniclabs.opensource.data.RemovePrescriptionRequest
import com.medtroniclabs.opensource.data.ResponseDataModel
import com.medtroniclabs.opensource.data.UnitMetricEntity
import com.medtroniclabs.opensource.data.UpdatePrescriptionModel
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.entity.DosageDurationEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.ncd.data.PredictionRequest
import com.medtroniclabs.opensource.ncd.data.PrescriptionNudgeResponse
import com.medtroniclabs.opensource.ncd.medicalreview.prescription.repo.NCDPrescriptionRepo
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class NCDPrescriptionViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val prescriptionRepository: NCDPrescriptionRepo
) : BaseViewModel(dispatcherIO) {

    val medicationListLiveData = MutableLiveData<Resource<ArrayList<MedicationResponse>>>()
    val prescriptionListLiveData = MutableLiveData<Resource<ArrayList<Prescription>>>()
    val unitList = MutableLiveData<List<UnitMetricEntity>>()
    val prescribedDaysList = MutableLiveData<List<DosageDurationEntity>>()
    val createPrescriptionLiveData = MutableLiveData<Resource<Map<String, Any>>>()
    var patient_visit_id: String? = null
    var memberReference: String? = null
    var patientReference: String? = null
    var enrollmentType: String? = null
    var identityValue: String? = null
    var selectedMedication: MedicationResponse? = null
    var prescriptionUIModel: ArrayList<MedicationResponse>? = null
    val reloadInstruction = MutableLiveData<Boolean>()
    val frequencyList = MutableLiveData<List<DosageFrequency>>()
    val discontinuedPrescriptionListLiveData = MutableLiveData<Resource<ArrayList<Prescription>>>()
    var savePrescriptionList: ArrayList<UpdatePrescriptionModel>? = null
    val updatePrescriptionLiveDate = MutableLiveData<Resource<ResponseDataModel>>()
    val removePrescriptionLiveData = MutableLiveData<Resource<Map<String, Any>>>()
    val medicationHistoryLiveData = MutableLiveData<Resource<ArrayList<Prescription>>>()
    val prescriptionPredictionResponseLiveDate =
        SingleLiveEvent<Resource<PrescriptionNudgeResponse>>()

    fun searchMedication(request: MedicationSearchRequest? = null) {
        viewModelScope.launch(dispatcherIO) {
            try {
                medicationListLiveData.postLoading()
                val response = request?.let { prescriptionRepository.searchMedication(request) }
                response?.data?.let {
                    medicationListLiveData.postSuccess(it)
                } ?: kotlin.run {
                    medicationListLiveData.postError()
                }
            } catch (e: Exception) {
                medicationListLiveData.postError()
            }
        }
    }

    fun getDosageFrequencyList() {
        viewModelScope.launch(dispatcherIO) {
            frequencyList.postValue(prescriptionRepository.getDosageFrequencyList())
        }
    }

    fun removePrescription(prescriptionId: String, reason: String?) {
        viewModelScope.launch(dispatcherIO) {
            removePrescriptionLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPrescriptionDelete,
                isCompleted = true
            )
            val response = prescriptionRepository.removePrescription(
                RemovePrescriptionRequest(
                    prescriptionId, ProvanceDto(),
                    reason,
                    requestFrom = DefinedParams.Africa
                )
            )
            removePrescriptionLiveData.postSuccess(response.data)
        }
    }

    fun getDosageUnitList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                unitList.postValue(prescriptionRepository.getUnitList(DefinedParams.PRESCRIPTION))
            } catch (_: Exception) {
                //Exception - Catch block
            }
        }
    }

    fun getPrescribedDays() {
        viewModelScope.launch(dispatcherIO) {
            try {
                prescribedDaysList.postValue(prescriptionRepository.getDosageDurations())
            } catch (_: Exception) {
                //Exception - Catch block
            }
        }
    }

    fun getPrescriptionList(request: PrescriptionListRequest) {
        viewModelScope.launch(dispatcherIO) {
            if (request.isActive) {
                prescriptionListLiveData.postLoading()
            } else {
                discontinuedPrescriptionListLiveData.postLoading()
            }
            val response = prescriptionRepository.getPrescriptionList(request)
            response.data?.let {
                if (request.isActive) {
                    prescriptionListLiveData.postSuccess(it)
                } else {
                    discontinuedPrescriptionListLiveData.postSuccess(it)
                }
            } ?: kotlin.run {
                if (request.isActive) {
                    prescriptionListLiveData.postError()
                } else {
                    discontinuedPrescriptionListLiveData.postError()
                }
            }
        }
    }


    fun createOrUpdatePrescription(
        signatureBitmap: Bitmap,
        filePath: File,
        request: PatientPrescriptionModel
    ) {
        updatePrescriptionLiveDate.postLoading()
        viewModelScope.launch(dispatcherIO) {
            try {
                filePath.mkdirs()
                val signature = "${request.patientVisitId}${DefinedParams.SIGN_SUFFIX}.jpeg"
                val file = File(filePath, signature)
                if (file.exists()) {
                    val result = file.delete()
                    if (result) {
                        uploadPrescription(
                            file = file,
                            signatureBitmap = signatureBitmap,
                            request = request,
                        )
                    } else {
                        updatePrescriptionLiveDate.postError()
                    }
                } else {
                    uploadPrescription(
                        file = file,
                        signatureBitmap = signatureBitmap,
                        request = request,
                    )
                }
            } catch (e: Exception) {
                updatePrescriptionLiveDate.postError()
            }
        }
    }

    fun getMedicationHistory(prescriptionId: String?) {
        medicationHistoryLiveData.postLoading()
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = prescriptionRepository.getPatientPrescriptionHistoryList(
                    RemovePrescriptionRequest(
                        prescriptionId = prescriptionId,
                        requestFrom = DefinedParams.Africa
                    )
                )
                response.data.let {
                    medicationHistoryLiveData.postSuccess(it)
                }
            } catch (e: Exception) {
                medicationHistoryLiveData.postError()
            }
        }
    }

    private suspend fun uploadPrescription(
        file: File,
        signatureBitmap: Bitmap,
        request: PatientPrescriptionModel,
    ) {
        val out = FileOutputStream(file)
        signatureBitmap.compress(Bitmap.CompressFormat.JPEG, 20, out)
        out.flush()
        out.close()
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)
        builder.addFormDataPart(
            "signature",
            file.name,
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
        )

        val prescriptionRequest = request.prescriptions?.let {
            PrescriptionCreateRequest(
                enrollmentType = request.enrollmentType,
                identityValue = request.identityValue,
                requestFrom = DefinedParams.Africa,
                encounter = EncounterDetails(
                    patientVisitId = patient_visit_id,
                    memberId = memberReference,
                    patientReference = patientReference,
                    provenance = ProvanceDto()
                ), prescriptions = it
            )
        }

        val dataRequest = Gson().toJson(prescriptionRequest)
        builder.addFormDataPart("prescriptionRequest", dataRequest)
        val requestBody = builder.build()
        setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.NCDPrescriptionCreation,
            isCompleted = true
        )
        val response = prescriptionRepository.createPrescriptionRequest(requestBody)
        response.data?.let {
            createPrescriptionLiveData.postSuccess(it)
        } ?: kotlin.run {
            createPrescriptionLiveData.postError()
        }
    }

     fun getPrescriptionPrediction() {
        viewModelScope.launch(dispatcherIO) {
            try {
                val response = prescriptionRepository.getNudgesList(PredictionRequest(memberId = memberReference))
                if (response.isSuccessful) {
                    val res = response.body()
                    if (res?.status == true) {
                        prescriptionPredictionResponseLiveDate.postSuccess(res.entity)
                    }
                }
            } catch (e: Exception) {
                //error Block
            }
        }
    }
}