package com.medtroniclabs.opensource.ncd.registration.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postError
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.StringConverter
import com.medtroniclabs.opensource.data.LocalSpinnerResponse
import com.medtroniclabs.opensource.data.model.RegistrationResponse
import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.formgeneration.model.FormLayout
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.registration.repo.RegistrationRepository
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
class RegistrationFormViewModel  @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var registrationRepository: RegistrationRepository
) : BaseViewModel(dispatcherIO) {
    var registrationResponseLiveData = MutableLiveData<Resource<RegistrationResponse>>()
    var validatePatientResponseLiveDate =
        MutableLiveData<Resource<Pair<HashMap<String, Any>, List<FormLayout?>?>>>()

    val countrySpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val districtSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val chiefdomSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val villageSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val programsSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()

    var isFromProceedEnrollment = false

    fun loadDataCacheByType(type: String, tag: String, selectedParent: Long?) {
        viewModelScope.launch(dispatcherIO) {
            when (type) {
                DefinedParams.Country -> {
                    countrySpinnerLiveData.postLoading()
                    countrySpinnerLiveData.postValue(
                        registrationRepository.getCountries(tag)
                    )
                }

                DefinedParams.District -> {
                    selectedParent?.let { parent ->
                        districtSpinnerLiveData.postLoading()
                        districtSpinnerLiveData.postValue(
                            registrationRepository.getCounties(tag, parent)
                        )
                    }
                }

                DefinedParams.Chiefdom -> {
                    selectedParent?.let { parent ->
                        chiefdomSpinnerLiveData.postLoading()
                        chiefdomSpinnerLiveData.postValue(
                            registrationRepository.getSubCounties(tag, parent)
                        )
                    }
                }

                DefinedParams.Village -> {
                    selectedParent?.let { parent ->
                        villageSpinnerLiveData.postLoading()
                        villageSpinnerLiveData.postValue(
                            registrationRepository.getAllVillages(tag, parent)
                        )
                    }
                }

                DefinedParams.Program -> {
                    programsSpinnerLiveData.postLoading()
                    programsSpinnerLiveData.postValue(
                        registrationRepository.getAllPrograms(tag)
                    )
                }
            }
        }
    }

    fun registerPatient(
        context: Context,
        hashMap: HashMap<String, Any>,
        signature: ByteArray?
    ) {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)

        hashMap.apply {
            put(DefinedParams.TenantId, SecuredPreference.getTenantId())
            put(DefinedParams.HealthFacilityId, SecuredPreference.getOrganizationId())
            put(DefinedParams.HealthFacilityFhirId, SecuredPreference.getOrganizationFhirId())
            put(DefinedParams.Provenance, ProvanceDto())
        }
        StringConverter.convertGivenMapToString(hashMap)?.let { req ->
            builder.addFormDataPart("registrationRequest", req)
        }

        signature?.let { sign ->
            val signMap = CommonUtils.convertByteArrayToBitmap(sign)

            val identityValue = CommonUtils.getIdentityValue(hashMap)
            val fileName = "${identityValue}${Screening.RegistrationSignSuffix}.jpeg"

            val filePath = CommonUtils.getFilePath(identityValue, context)
            filePath.mkdirs()

            val file = File(filePath, fileName)

            val clearedExistingFile: Boolean = if (file.exists()) file.delete() else true

            if (clearedExistingFile && signMap != null) {
                val out = FileOutputStream(file)
                signMap.compress(Bitmap.CompressFormat.JPEG, 20, out)
                out.flush()
                out.close()
                file.let {
                    builder.addFormDataPart(
                        "signatureFile",
                        file.name,
                        file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    )
                }
            } else
                registrationResponseLiveData.postError()
        }

        viewModelScope.launch(dispatcherIO) {
            registrationResponseLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDRegistrationCreation,
                isCompleted = true
            )
            registrationResponseLiveData.postValue(registrationRepository.registerPatient(builder.build()))
        }
    }

    fun validatePatient(resp: HashMap<String, Any>, serverData: List<FormLayout?>?) {
        viewModelScope.launch(dispatcherIO) {
            validatePatientResponseLiveDate.postLoading()
            validatePatientResponseLiveDate.postValue(
                registrationRepository.validatePatient(resp, Pair(resp, serverData))
            )
        }
    }
}