package org.medtroniclabs.uhis.ncd.registration.viewmodel

import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
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
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.model.RegistrationResponse
import org.medtroniclabs.uhis.data.offlinesync.model.ProvanceDto
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.ncd.registration.repo.RegistrationRepository
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.ui.BaseViewModel
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject

@HiltViewModel
class RegistrationFormViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var registrationRepository: RegistrationRepository,
) : BaseViewModel(dispatcherIO) {
    var registrationResponseLiveData = MutableLiveData<Resource<RegistrationResponse>>()
    var validatePatientResponseLiveDate =
        MutableLiveData<Resource<Pair<HashMap<String, Any>, List<FormLayout>?>>>()

    val countrySpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val districtSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val chiefdomSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val villageSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()
    val programsSpinnerLiveData = MutableLiveData<Resource<LocalSpinnerResponse>>()

    var isFromProceedEnrollment = false

    fun loadDataCacheByType(
        type: String,
        tag: String,
        selectedParent: Long?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            when (type) {
                DefinedParams.COUNTRY -> {
                    countrySpinnerLiveData.postLoading()
                    countrySpinnerLiveData.postValue(
                        registrationRepository.getCountries(tag),
                    )
                }

                DefinedParams.District -> {
                    selectedParent?.let { parent ->
                        districtSpinnerLiveData.postLoading()
                        districtSpinnerLiveData.postValue(
                            registrationRepository.getCounties(tag, parent),
                        )
                    }
                }

                DefinedParams.CHIEFDOM -> {
                    selectedParent?.let { parent ->
                        chiefdomSpinnerLiveData.postLoading()
                        chiefdomSpinnerLiveData.postValue(
                            registrationRepository.getSubCounties(tag, parent),
                        )
                    }
                }

                DefinedParams.VILLAGE -> {
                    selectedParent?.let { parent ->
                        villageSpinnerLiveData.postLoading()
                        villageSpinnerLiveData.postValue(
                            registrationRepository.getAllVillages(tag, parent),
                        )
                    }
                }

                DefinedParams.PROGRAM -> {
                    programsSpinnerLiveData.postLoading()
                    programsSpinnerLiveData.postValue(
                        registrationRepository.getAllPrograms(tag),
                    )
                }
            }
        }
    }

    fun registerPatient(
        context: Context,
        hashMap: HashMap<String, Any>,
        signature: ByteArray?,
    ) {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)

        hashMap.apply {
            put(DefinedParams.TENANT_ID, SecuredPreference.getTenantId())
            put(DefinedParams.HEALTH_FACILITY_ID, SecuredPreference.getOrganizationId())
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
                        file.asRequestBody("multipart/form-data".toMediaTypeOrNull()),
                    )
                }
            } else {
                registrationResponseLiveData.postError()
            }
        }

        viewModelScope.launch(dispatcherIO) {
            registrationResponseLiveData.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDRegistrationCreation,
                isCompleted = true,
            )
            registrationResponseLiveData.postValue(registrationRepository.registerPatient(builder.build()))
        }
    }

    fun validatePatient(
        resp: HashMap<String, Any>,
        serverData: List<FormLayout>?,
    ) {
        viewModelScope.launch(dispatcherIO) {
            validatePatientResponseLiveDate.postLoading()
            validatePatientResponseLiveDate.postValue(
                registrationRepository.validatePatient(resp, Pair(resp, serverData)),
            )
        }
    }
}
