package com.medtroniclabs.opensource.ui.medicalreview.prescription

import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.EncounterDetails
import com.medtroniclabs.opensource.data.MedicationRequestObject
import com.medtroniclabs.opensource.data.MedicationResponse
import com.medtroniclabs.opensource.data.MedicationSearchRequest
import com.medtroniclabs.opensource.data.Prescription
import com.medtroniclabs.opensource.data.PrescriptionListRequest
import com.medtroniclabs.opensource.data.PrescriptionRequest
import com.medtroniclabs.opensource.data.RemovePrescriptionRequest
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.db.entity.FrequencyEntity
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.repo.MedicationRepository
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
class PrescriptionViewModel @Inject constructor(
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
    private val medicationRepository: MedicationRepository
) : ViewModel() {

    val medicationListLiveData = MutableLiveData<Resource<java.util.ArrayList<MedicationResponse>>>()

    val selectedMedicationLiveData = MutableLiveData<ArrayList<MedicationRequestObject>>()

    val createPrescriptionLiveData = MutableLiveData<Resource<Map<String, Any>>>()

    val prescriptionListLiveData = MutableLiveData<Resource<ArrayList<Prescription>>>()

    val discontinuedPrescriptionListLiveData = MutableLiveData<Resource<ArrayList<Prescription>>>()

    val removePrescriptionLiveData = MutableLiveData<Resource<Map<String, Any>>>()

    var patientId: String? = null

    val frequencyListLiveDate = MutableLiveData<Resource<List<FrequencyEntity>>>()

    fun searchMedicationByName(name: String) {
        viewModelScope.launch(dispatcherIO) {
            try {
                medicationListLiveData.postLoading()
                val request = MedicationSearchRequest(name)
                val response = medicationRepository.searchMedicationByName(request)
                response.data?.let {
                    medicationListLiveData.postSuccess(it)
                } ?: kotlin.run {
                    medicationListLiveData.postError()
                }
            } catch (e: Exception) {
                medicationListLiveData.postError()
            }
        }
    }

    fun updateMedicationList(
        medicationResponse: ArrayList<MedicationRequestObject>,
        reset: Boolean,
    ) {
        if (reset) {
            selectedMedicationLiveData.value?.clear()
        }
        val medicationList: ArrayList<MedicationRequestObject> =
            selectedMedicationLiveData.value ?: ArrayList()
        medicationList.addAll(medicationResponse)
        selectedMedicationLiveData.value = medicationList
    }

    fun getFrequencyList() {
        viewModelScope.launch(dispatcherIO) {
            frequencyListLiveDate.postValue(medicationRepository.getFrequencyList())
        }
    }

    fun getFrequencyMap(): ArrayList<Map<String, Any>> {
        val mapList = ArrayList<Map<String, Any>>()
        frequencyListLiveDate.value?.data?.forEach { data ->
            val map = HashMap<String, Any>()
            map[DefinedParams.NAME] = data.name
            map[DefinedParams.ID] = data.id
            map[DefinedParams.Frequency] = data.frequency ?: 1
            map[DefinedParams.Description] = data.description ?: ""
            map[DefinedParams.DisplayOrder] = data.displayOrder
            mapList.add(map)
        }

        return mapList
    }

    fun createPrescription(
        signature: Bitmap,
        filePath: File,
        list: ArrayList<MedicationRequestObject>,
        data: PatientListRespModel,
        encounterId: String?
    ) {
        viewModelScope.launch(dispatcherIO) {
            try {
                createPrescriptionLiveData.postLoading()
                filePath.mkdirs()
                val file = File(filePath, "${DefinedParams.SIGN_SUFFIX}.jpeg")
                if (file.exists()) file.delete()
                val out = FileOutputStream(file)
                signature.compress(Bitmap.CompressFormat.JPEG, 20, out)
                out.flush()
                out.close()
                val builder = MultipartBody.Builder()
                builder.setType(MultipartBody.FORM)
                builder.addFormDataPart(
                    "signature",
                    file.name,
                    file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                )
                val prescriptionList = ArrayList<Prescription>()
                list.forEach {
                    if (it.medicationResponse.prescribedDays != null) {
                        it.medicationResponse.name?.let { it1 ->
                            Prescription(
                                prescribedDays = it.medicationResponse.prescribedDays,
                                dosageDurationName = it.medicationResponse.dosageDurationName,
                                medicationName = it1,
                                medicationId = it.medicationResponse.id.toString(),
                                frequency = getMedicationFrequency(it),
                                prescribedSince = System.currentTimeMillis().convertToUtcDateTime(),
                                prescriptionId = it.medicationResponse.prescriptionId,
                                codeDetails = it.medicationResponse.codeDetails,
                                frequencyName = getMedicationFrequencyName(it)
                            )
                        }?.let { it2 ->
                            prescriptionList.add(
                                it2
                            )
                        }
                    }
                }
                val prescriptionRequest = PrescriptionRequest(
                    encounter = EncounterDetails(
                        id = encounterId,
                        patientReference = data.id,
                        patientId = data.patientId ?: "",
                        memberId = data.memberId ?: "", provenance = ProvanceDto()
                    ),
                    prescriptions = prescriptionList
                )
                val dataRequest = Gson().toJson(prescriptionRequest)
                builder.addFormDataPart("prescriptionRequest", dataRequest)
                val requestBody = builder.build()
                val response = medicationRepository.createPrescriptionRequest(requestBody)
                response.data?.let {
                    createPrescriptionLiveData.postSuccess(it)
                } ?: kotlin.run {
                    createPrescriptionLiveData.postError()
                }
            } catch (e: Exception) {
                createPrescriptionLiveData.postError()
            }
        }
    }






    fun constructMedicationRequestObjectList(
        list: java.util.ArrayList<Prescription>
    ): ArrayList<MedicationRequestObject> {
        val medicationRequestObjectList = ArrayList<MedicationRequestObject>()
        list.forEach { prescription ->
            val medicationRequestObject =
                MedicationRequestObject(constructMedicationRequestObject(prescription))
            medicationRequestObjectList.add(medicationRequestObject)
        }
        return medicationRequestObjectList
    }

    fun constructMedicationRequestObject(prescription: Prescription): MedicationResponse {
        val selectedFrequencyMap = getSelectedFrequencyMap(prescription.frequencyName)

        return MedicationResponse(
            id = prescription.medicationId.toLongOrNull(),
            name = prescription.medicationName,
            selectedMap = selectedFrequencyMap,
            prescribedDays = prescription.prescribedDays,
            prescriptionId = prescription.prescriptionId,
            prescribedSince = prescription.prescribedSince,
        )
    }

    private fun getSelectedFrequencyMap(frequency: String): HashMap<String, Any>? {
        val list = getFrequencyMap().filter { it[DefinedParams.NAME] == frequency }
        return if (list.isNotEmpty())
            HashMap(list[0])
        else
            null
    }

    private fun getMedicationFrequency(data: MedicationRequestObject): Int {
        return data.medicationResponse.selectedMap?.get(DefinedParams.Frequency) as? Int? ?: 0
    }

    private fun getMedicationFrequencyName(data: MedicationRequestObject): String {
        return data.medicationResponse.selectedMap?.get(DefinedParams.NAME) as? String? ?: ""
    }

    private fun getPrescriptionList(request: PrescriptionListRequest) {
        viewModelScope.launch(dispatcherIO) {
            if (request.isActive) {
                prescriptionListLiveData.postLoading()
            } else {
                discontinuedPrescriptionListLiveData.postLoading()
            }
            val response = medicationRepository.getPrescriptionList(request)
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

    fun removePrescription(prescriptionId: String, reason: String?) {
        viewModelScope.launch(dispatcherIO) {
            removePrescriptionLiveData.postLoading()
            val response = medicationRepository.removePrescription(
                RemovePrescriptionRequest(
                    prescriptionId, ProvanceDto(),
                    reason
                )
            )
            removePrescriptionLiveData.postSuccess(response.data)
        }
    }

    fun getPrescriptionList(data: PatientListRespModel, isDeleted: Boolean = true) {
        getPrescriptionList(
            PrescriptionListRequest(
                patientReference = data.id,
                memberReference = data.memberId,
                isDeleted
            )
        )
    }


}