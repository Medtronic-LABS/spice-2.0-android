package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import android.text.SpannableStringBuilder
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.text.color
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.ErrorResponse
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.registration.PatientCreateResponse
import org.medtroniclabs.uhis.data.registration.PatientModel
import org.medtroniclabs.uhis.data.registration.QRCodeRequest
import org.medtroniclabs.uhis.data.registration.QRCodeResponse
import org.medtroniclabs.uhis.data.registration.RiskClassificationModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.FormGenerator
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.model.FormLayout
import org.medtroniclabs.uhis.mappingkey.Screening
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.network.utils.DoesNetworkHaveInternet
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class EnrollmentFormBuilderViewModel @Inject constructor(
    private val onBoardingRepo: OnBoardingRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) :
    ViewModel() {
        @Inject
        lateinit var connectivityManager: ConnectivityManager
        var formResponseLiveData = MutableLiveData<Resource<List<FormLayout>>>()
        var formResponseListLiveData = MutableLiveData<Resource<ArrayList<Pair<String, String>>>>()
        var duplicationNudgeResponse = MutableLiveData<Resource<Pair<String, PatientModel?>>>()
        var enrollPatientLiveData = MutableLiveData<Resource<PatientCreateResponse>>()
        var groupedEnrollmentHashMap = HashMap<String, Any>()
        var patientTrackId: Long? = null

        var assessmentRequired: Boolean = true
        var list = ArrayList<RiskClassificationModel>()

        var unionCacheResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var villageCacheResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var mentalHealthQuestions = MutableLiveData<Resource<HashMap<String, LocalSpinnerResponse>>>()
        var programListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var patientInitial: String = ""
        var isConfirmDiagnosis: Boolean = false
        var isFromDirectEnrollment = false
        var screeningId: Long? = null
        var qrCodeValidationResult = MutableLiveData<Resource<QRCodeResponse>>()
        val patientVisitIDResponse = MutableLiveData<Resource<Long>>()
        var isNationalIdGenerated: Boolean = false
        var nationalId: String = "-1"

        fun fetchWorkFlow(formType: String) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    formResponseLiveData.postLoading()

                    val formLayout = onBoardingRepo
                        .getFormData(formType)
                        .data
                        ?.formLayout

                    formResponseLiveData.postSuccess(formLayout)
                } catch (e: Exception) {
                    formResponseLiveData.postError(e.message)
                }
            }
        }

        fun fetchWorkFlow(
            formTypeOne: String,
            formTypeTwo: String,
        ) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    formResponseListLiveData.postLoading()
                    val formResponseList = onBoardingRepo.getFormBasedOnType(formTypeOne, formTypeTwo)
                    val list = ArrayList<Pair<String, String>>()
                    formResponseList?.forEach {
                        list.add(Pair(it.formType, it.formInput))
                    }
                    formResponseListLiveData.postSuccess(list)
                } catch (e: Exception) {
                    formResponseListLiveData.postError(e.message)
                }
            }
        }

        fun getRiskEntityList() {
            viewModelScope.launch(dispatcherIO) {
                val resultOne = onBoardingRepo.riskFactorListing()
                val baseType: Type = object : TypeToken<ArrayList<RiskClassificationModel>>() {}.type
                if (resultOne.isNotEmpty()) {
                    val resultList = Gson().fromJson<ArrayList<RiskClassificationModel>>(
                        resultOne[0].nonLabEntity,
                        baseType,
                    )
                    list.clear()
                    list.addAll(resultList)
                }
            }
        }

        fun enrollPatient(
            context: Context,
            requestJson: String,
            maxSequence: Long? = null,
            villageId: Long? = null,
            patientTrackerId: Long? = null,
        ) {
            var request = CommonUtils.addValuesInJSON(requestJson, DefinedParams.IS_GENERATED_NATIONAL_ID, isNationalIdGenerated, DefinedParams.BIO_DATA)
            val rootJson: JsonObject = StringConverter.getJsonObject(request)

            rootJson.let {
                viewModelScope.launch(dispatcherIO) {
                    try {
                        if (connectivityManager.isNetworkAvailable()) {
                            enrollPatientLiveData.postLoading()
                            if ((patientTrackerId ?: 0) > 0) {
                                var enrollmentReq = it
                                StringConverter.convertStringToMap(request)?.let { map ->
                                    val reqMap = HashMap(map)
                                    reqMap[DefinedParams.PATIENT_TRACK_ID] = patientTrackerId
                                    StringConverter
                                        .convertGivenMapToString(reqMap)
                                        ?.let { reqStr ->
                                            enrollmentReq =
                                                StringConverter.getJsonObject(reqStr)
                                        }
                                }

                                proceedToCreatePatient(
                                    context,
                                    maxSequence,
                                    villageId,
                                    enrollmentReq,
                                )
                            } else {
                                val validateResponse = onBoardingRepo.validatePatient(it)
                                if (validateResponse.isSuccessful) {
                                    if (validateResponse.body()?.status == true) {
                                        proceedToCreatePatient(
                                            context,
                                            maxSequence,
                                            villageId,
                                            it,
                                        )
                                    } else {
                                        enrollPatientLiveData.postError()
                                    }
                                } else if (validateResponse.code() == AppConstants.CONFLICT_ERROR_CODE) {
                                    val entity = StringConverter.getFormattedData(
                                        context,
                                        validateResponse.errorBody(),
                                        true,
                                    )
                                    duplicationNudgeResponse.postSuccess(entity)
                                } else {
                                    enrollPatientLiveData.postError(
                                        StringConverter.getErrorMessage(
                                            validateResponse.errorBody(),
                                        ),
                                    )
                                }
                            }
                        } else {
                            enrollPatientLiveData.postError(context.getString(R.string.no_internet_error))
                        }
                    } catch (e: Exception) {
                        enrollPatientLiveData.postError()
                    }
                }
            }
        }

        private suspend fun proceedToCreatePatient(
            context: Context,
            maxSequence: Long? = null,
            villageId: Long? = null,
            enrollmentReq: JsonObject,
        ) {
            val response = onBoardingRepo.createPatient(enrollmentReq)
            if (response.isSuccessful) {
                if (response.body()?.status == true) {
                    if (villageId != null && maxSequence != null) {
                        updateMySequenceCode(villageId, maxSequence)
                    }
                    enrollPatientLiveData.postSuccess(response.body()?.entity)
                } else {
                    enrollPatientLiveData.postError()
                }
            } else if (response.code() == AppConstants.CONFLICT_ERROR_CODE) {
                val entity = StringConverter.getFormattedData(
                    context,
                    response.errorBody(),
                    true,
                )
                duplicationNudgeResponse.postSuccess(entity)
            } else {
                enrollPatientLiveData.postError(
                    StringConverter.getErrorMessage(
                        response.errorBody(),
                    ),
                )
            }
        }

        fun loadDataCacheByType(
            type: String,
            tag: String,
            selectedParent: Long?,
            restrictDefaultsFor: ArrayList<String>? = null,
        ) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    when (type) {
                        DefinedParams.CHIEF_DOM -> {
                            programListResponse.postLoading()
                            val response = onBoardingRepo.getAllChiefDoms()
                            programListResponse.postValue(
                                Resource(
                                    ResourceState.SUCCESS,
                                    LocalSpinnerResponse(DefinedParams.CHIEF_DOM, response),
                                ),
                            )
                        }

                        DefinedParams.VILLAGE -> {
                            unionCacheResponse.postLoading()
                            selectedParent?.let {
                                val response = onBoardingRepo.getVillageList(it)
                                if (response.isNotEmpty()) {
                                    unionCacheResponse.postValue(
                                        Resource(
                                            ResourceState.SUCCESS,
                                            LocalSpinnerResponse(tag, response),
                                        ),
                                    )
                                }
                            }
                        }

                        DefinedParams.SUB_VILLAGE -> {
                            villageCacheResponse.postLoading()
                            if (selectedParent != null) {
                                val villages = onBoardingRepo.getSubVillageByVillageId(selectedParent)
                                villageCacheResponse.postValue(
                                    Resource(
                                        ResourceState.SUCCESS,
                                        LocalSpinnerResponse(tag, villages),
                                    ),
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    programListResponse.postError()
                }
            }
        }

//    fun fetchMentalHealthQuestions(id: String, type: String) {
//        viewModelScope.launch(dispatcherIO) {
//            var mhResponse = mentalHealthQuestions.value?.data
//            mentalHealthQuestions.postLoading()
//            try {
//                if (CommonUtils.isAfrica()) {
//                    val phq4Questions = onBoardingRepo.getMHQuestionsByType(type = DefinedParams.PHQ4)
//                    val phq9Questions = onBoardingRepo.getMHQuestionsByType(type = DefinedParams.PHQ9)
//                    val gad7Questions = onBoardingRepo.getMHQuestionsByType(type = DefinedParams.GAD7)
//                    if (mhResponse == null)
//                        mhResponse = HashMap()
//
//                    mhResponse[DefinedParams.PHQ4] =
//                        LocalSpinnerResponse(
//                            tag = DefinedParams.PHQ4_Mental_Health,
//                            response = phq4Questions
//                        )
//                    mhResponse[DefinedParams.PHQ9] =
//                        LocalSpinnerResponse(
//                            tag = DefinedParams.PHQ9_Mental_Health,
//                            response = phq9Questions
//                        )
//                    mhResponse[DefinedParams.GAD7] =
//                        LocalSpinnerResponse(
//                            tag = DefinedParams.GAD7_Mental_Health,
//                            response = gad7Questions
//                        )
//                } else {
//                    val questions = onBoardingRepo.getMHQuestionsByType(type = type)
//                    mhResponse = HashMap()
//                    mhResponse[type] = LocalSpinnerResponse(tag = id, response = questions)
//                }
//
//                mentalHealthQuestions.postValue(Resource(ResourceState.SUCCESS, mhResponse))
//            } catch (e: Exception) {
//                mentalHealthQuestions.postValue(Resource(ResourceState.ERROR))
//            }
//        }
//    }

        fun validateQRCode(
            context: Context,
            qrCodeValue: String,
        ) {
            try {
                if (DoesNetworkHaveInternet.hasInternetConnection(context)) {
                    viewModelScope.launch {
                        qrCodeValidationResult.postLoading()
                        val response = onBoardingRepo.validateQRCodeValidation(QRCodeRequest(qrCodeValue))
                        if (response.isSuccessful) {
                            response.body()?.let { qrCodeResponse ->
                                if (qrCodeResponse.status) {
                                    qrCodeResponse.qrCode = qrCodeValue
                                    qrCodeValidationResult.postSuccess(qrCodeResponse)
                                } else {
                                    qrCodeValidationResult.postError(qrCodeResponse.message)
                                }
                            } ?: kotlin.run {
                                qrCodeValidationResult.postError(getErrorMessage(response.errorBody()))
                            }
                        } else {
                            qrCodeValidationResult.postError(getErrorMessage(response.errorBody()))
                        }
                    }
                } else {
                    qrCodeValidationResult.postError(context.getString(R.string.no_internet_error))
                }
            } catch (_: Exception) {
                // Exception - Catch block
            }
        }

        fun getErrorMessage(errorBody: ResponseBody?): String? {
            if (errorBody == null) {
                return null
            }
            return try {
                val errorResponse =
                    Gson().fromJson(errorBody.string(), ErrorResponse::class.java)
                errorResponse?.message
            } catch (e: Exception) {
                null
            }
        }

        private fun updateMySequenceCode(
            villageId: Long,
            newSequenceCode: Long,
        ) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    onBoardingRepo.updateSequenceCode(villageId, newSequenceCode)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        fun createPatientVisit(
            context: Context,
            request: MedicalReviewBaseRequest,
        ) {
            if (connectivityManager.isNetworkAvailable()) {
                patientVisitIDResponse.postLoading()
                viewModelScope.launch(dispatcherIO) {
                    try {
                        val response = onBoardingRepo.createPatientVisit(request)
                        if (response.isSuccessful) {
                            val res = response.body()
                            if (res?.status == true && res.entity != null) {
                                patientVisitIDResponse.postSuccess(res.entity.id)
                            }
                        } else {
                            patientVisitIDResponse.postError()
                        }
                    } catch (e: Exception) {
                        patientVisitIDResponse.postError()
                    }
                }
            } else {
                patientVisitIDResponse.postError(context.getString(R.string.no_internet_error))
            }
        }

        fun renderBMIValue(
            context: Context,
            formGenerator: FormGenerator,
            resultHashMap: HashMap<String, Any>,
        ) {
            val bmiView = formGenerator.getViewByTag(Screening.BMI) as? AppCompatTextView
            bmiView?.let { view ->
                if (!resultHashMap.containsKey(Screening.Weight) || !resultHashMap.containsKey(Screening.Height)) {
                    view.text = context.getString(R.string.hyphen_symbol)
                    formGenerator.removeIfContains(Screening.BMI)
                } else {
                    if (resultHashMap.containsKey(Screening.Weight) &&
                        resultHashMap.containsKey(
                            Screening.Height,
                        )
                    ) {
                        val weight = resultHashMap[Screening.Weight] as? Double
                        val height = resultHashMap[Screening.Height] as? Double

                        if (weight == null || height == null) {
                            view.text = context.getString(R.string.hyphen_symbol)
                        } else {
                            val bmi = CommonUtils.getBMIForNcd(height, weight)
                            CommonUtils
                                .getBMIInformation(context, bmi?.toDoubleOrNull())
                                ?.let { info ->
                                    bmi?.toDoubleOrNull()?.let {
                                        resultHashMap[Screening.BMI] = it
                                    }
                                    resultHashMap[Screening.BMI_CATEGORY] = info.first

                                    val bmiWithInfoSpannableStringBuilder = if (bmi == null) {
                                        context.getString(R.string.hyphen_symbol)
                                    } else {
                                        SpannableStringBuilder()
                                            .append(bmi)
                                            .color(context.getColor(info.second)) {
                                                append(" (${info.first})")
                                            }
                                    }
                                    view.text = bmiWithInfoSpannableStringBuilder
                                }
                        }
                    }
                }
            }
        }
    }
