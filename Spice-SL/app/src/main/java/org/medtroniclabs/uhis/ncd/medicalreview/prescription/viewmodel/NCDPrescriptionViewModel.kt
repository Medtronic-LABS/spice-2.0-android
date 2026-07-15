package org.medtroniclabs.uhis.ncd.medicalreview.prescription.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.data.DosageFrequency
import org.medtroniclabs.uhis.data.EncounterDetails
import org.medtroniclabs.uhis.data.MedicationResponse
import org.medtroniclabs.uhis.data.MedicationSearchRequest
import org.medtroniclabs.uhis.data.PatientPrescriptionModel
import org.medtroniclabs.uhis.data.Prescription
import org.medtroniclabs.uhis.data.PrescriptionCreateRequest
import org.medtroniclabs.uhis.data.PrescriptionListRequest
import org.medtroniclabs.uhis.data.RemovePrescriptionRequest
import org.medtroniclabs.uhis.data.ResponseDataModel
import org.medtroniclabs.uhis.data.UnitMetricEntity
import org.medtroniclabs.uhis.data.UpdatePrescriptionModel
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.db.entity.DosageDurationEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.ncd.data.PredictionRequest
import org.medtroniclabs.uhis.ncd.data.PrescriptionNudgeResponse
import org.medtroniclabs.uhis.ncd.medicalreview.prescription.repo.NCDPrescriptionRepo
import org.medtroniclabs.uhis.network.SingleLiveEvent
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class NCDPrescriptionViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val prescriptionRepository: NCDPrescriptionRepo,
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
    var prescriptionId: String? = null

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

    fun removePrescription(
        prescriptionId: String,
        reason: String?,
    ) {
        this.prescriptionId = prescriptionId
        viewModelScope.launch(dispatcherIO) {
            removePrescriptionLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDPrescriptionDelete,
                isCompleted = true,
            )
            val response = prescriptionRepository.removePrescription(
                RemovePrescriptionRequest(
                    prescriptionId,
                    ProvanceDto(),
                    reason,
                    requestFrom = DefinedParams.AFRICA,
                ),
            )
            removePrescriptionLiveData.postSuccess(response.data)
        }
    }

    fun getDosageUnitList() {
        viewModelScope.launch(dispatcherIO) {
            try {
                unitList.postValue(prescriptionRepository.getUnitList(DefinedParams.PRESCRIPTION))
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }
    }

    fun getPrescribedDays() {
        viewModelScope.launch(dispatcherIO) {
            try {
                prescribedDaysList.postValue(prescriptionRepository.getDosageDurations())
            } catch (_: Exception) {
                // Exception - Catch block
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
        request: PatientPrescriptionModel,
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
                        requestFrom = DefinedParams.AFRICA,
                    ),
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
            file.asRequestBody("multipart/form-data".toMediaTypeOrNull()),
        )

        val prescriptionRequest = request.prescriptions?.let {
            PrescriptionCreateRequest(
                enrollmentType = request.enrollmentType,
                identityValue = request.identityValue,
                requestFrom = DefinedParams.AFRICA,
                encounter = EncounterDetails(
                    patientVisitId = patient_visit_id,
                    memberId = memberReference,
                    patientReference = patientReference,
                    provenance = ProvanceDto(),
                ),
                prescriptions = it,
            )
        }

        val dataRequest = Gson().toJson(prescriptionRequest)
        builder.addFormDataPart("prescriptionRequest", dataRequest)
        val requestBody = builder.build()
        setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.NCDPrescriptionCreation,
            isCompleted = true,
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
                // error Block
            }
        }
    }
}
