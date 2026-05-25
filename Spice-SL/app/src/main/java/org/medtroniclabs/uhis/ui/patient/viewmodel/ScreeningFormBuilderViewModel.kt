package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.StringConverter
import org.medtroniclabs.uhis.data.LocalSpinnerResponse
import org.medtroniclabs.uhis.data.registration.PatientModel
import org.medtroniclabs.uhis.data.registration.RiskClassificationModel
import org.medtroniclabs.uhis.data.registration.SiteEnum
import org.medtroniclabs.uhis.db.entity.ScreeningEntity
import org.medtroniclabs.uhis.db.entity.SiteEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams.SKIP_DUPLICATE_VALIDATION
import org.medtroniclabs.uhis.formgeneration.model.FormResponse
import org.medtroniclabs.uhis.ncd.data.SiteDetails
import org.medtroniclabs.uhis.ncd.screening.repo.ScreeningRepository
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import java.lang.reflect.Type
import javax.inject.Inject

@HiltViewModel
class ScreeningFormBuilderViewModel @Inject constructor(
    private val screeningRepository: ScreeningRepository,
    private val onBoardingRepo: OnBoardingRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) :
    ViewModel() {
        var formResponseLiveData = MutableLiveData<Resource<FormResponse>>()
        var formResponseListLiveData = MutableLiveData<Resource<ArrayList<Pair<String, String>>>>()
        var screeningLiveDate = MutableLiveData<Resource<Boolean>>()
        var screeningSaveResponse = MutableLiveData<Resource<Triple<Long, Long?, Long?>>>()
        var duplicationNudgeResponse = MutableLiveData<Resource<Pair<String, PatientModel?>>>()
        var screeningEntity = MutableLiveData<Resource<ScreeningEntity>>()
        var list = ArrayList<RiskClassificationModel>()
        var siteDetail: SiteDetails? = null
        var screeningEntityRowId: Long? = null
        var patientTrackId: Long? = null
        var foregroundOnlyLocationServiceBound = false
        var accountSiteListLiveData = MutableLiveData<Resource<ArrayList<SiteEntity>>>()

        var mentalHealthQuestions = MutableLiveData<Resource<HashMap<String, LocalSpinnerResponse>>>()
        var isReferredForFurtherAssessment = MutableLiveData(false)
        var localDataCacheResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var unionListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var usersListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var ssListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var upazilaResponse = MutableLiveData<Resource<Pair<Boolean, LocalSpinnerResponse>>>()
        var villageListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var upazilaListResponse = MutableLiveData<Resource<LocalSpinnerResponse>>()
        var summaryMap: Map<String, Any>? = null
        var referredSite: String? = null
        var ccSites = MutableLiveData<ArrayList<SiteEntity>?>()
        var screeningLogRegId: Long? = null
        var isEyeScreening: Boolean = false
        var isCataractScreening: Boolean = false
        var isNationalIdGenerated: Boolean = false
        var isFromDirectEnrollment = false

        @Inject
        lateinit var connectivityManager: ConnectivityManager

        val validateSessionLiveData = MutableLiveData<Resource<ResponseBody>>()

        fun fetchWorkFlow(formType: String) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    formResponseLiveData.postLoading()
                    val response = onBoardingRepo.getFormData(formType)
                    formResponseLiveData.postSuccess(response.data)
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

        private var requestJsonForScreeningAllowToDuplicate: HashMap<String, Any>? = null
        private var screeningEntityAllowToDuplicate: ScreeningEntity? = null
        private var trackIdAllowToDuplicate: Long? = null
        private var villageSeqIdFunctionAllowToDuplicate: Pair<Long?, Long?>? = null

        fun savePatientScreeningInformation(
            context: Context,
            screeningRawString: String,
            generalDetail: String,
            isReferred: Boolean,
            uploadStatus: Boolean,
            isRecursion: Boolean,
            trackId: Long? = null,
            villageSeqId: Pair<Long?, Long?>? = null,
        ) {
            viewModelScope.launch(dispatcherIO) {
                screeningSaveResponse.postLoading()
                var screeningEntityRawString = screeningRawString

                screeningEntityRawString = CommonUtils.addValuesInJSON(
                    screeningEntityRawString,
                    DefinedParams.IS_GENERATED_NATIONAL_ID,
                    isNationalIdGenerated,
                    DefinedParams.BIO_DATA,
                )
                // If it is direct Eye care i need to send the ScreeningType
                if (isEyeScreening) {
                    screeningEntityRawString = CommonUtils.addValuesInJSON(
                        screeningEntityRawString,
                        DefinedParams.SCREENING_TYPE,
                        DefinedParams.EYE_CARE_SCREENING,
                    )
                }
                if (isCataractScreening) {
                    screeningEntityRawString = CommonUtils.addValuesInJSON(
                        screeningEntityRawString,
                        DefinedParams.SCREENING_TYPE,
                        DefinedParams.CATARACT,
                    )
                }

                try {
                    val screeningEntity = ScreeningEntity(
                        screeningDetails = screeningEntityRawString,
                        generalDetails = generalDetail,
                        userId = SecuredPreference.getUserId().toString(),
                        isReferred = isReferred,
                        uploadStatus = uploadStatus,
                    )
                    if (!isRecursion && connectivityManager.isNetworkAvailable()) {
                        val requestJson =
                            CommonUtils.parseRequest(generalDetail, screeningEntityRawString, false)
                        if (requestJson != null) {
                            val request = StringConverter.getJsonObject(Gson().toJson(requestJson))
                            val response = onBoardingRepo.createScreeningLog(request)

                            if (response.isSuccessful) {
                                screeningLogRegId = response.body()?.entity?.id
                                savePatientScreeningInformation(
                                    context,
                                    screeningEntityRawString,
                                    generalDetail,
                                    isReferred = isReferred,
                                    uploadStatus = true,
                                    isRecursion = true,
                                    trackId = response.body()?.entity?.patientTrackId,
                                    villageSeqId,
                                )
                            } else if (response.code() == AppConstants.CONFLICT_ERROR_CODE) {
                                requestJsonForScreeningAllowToDuplicate = requestJson
                                trackIdAllowToDuplicate = trackId
                                screeningEntityAllowToDuplicate = screeningEntity
                                villageSeqIdFunctionAllowToDuplicate = villageSeqId
                                val entity = StringConverter.getFormattedData(
                                    context,
                                    response.errorBody(),
                                    false,
                                )
                                duplicationNudgeResponse.postSuccess(entity)
                            } else {
                                savePatientScreeningInformation(
                                    context,
                                    screeningEntityRawString,
                                    generalDetail,
                                    isReferred = isReferred,
                                    uploadStatus = false,
                                    isRecursion = true,
                                    trackId = trackId,
                                    villageSeqId,
                                )
                            }
                        }
                    } else {
                        val rowId = screeningRepository.savePatientScreeningInformation(screeningEntity).id
                        screeningEntityRowId = rowId
                        patientTrackId = trackId

                        screeningSaveResponse.postSuccess(
                            Triple(
                                rowId,
                                villageSeqId?.first,
                                villageSeqId?.second,
                            ),
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    screeningSaveResponse.postError()
                }
            }
        }

        fun duplicateAllow() {
            viewModelScope.launch(dispatcherIO) {
                screeningSaveResponse.postLoading()
                try {
                    // Safely add skipvalidation flag
                    requestJsonForScreeningAllowToDuplicate?.apply {
                        put(SKIP_DUPLICATE_VALIDATION, true)
                    }
                    // Convert to JSON and create request
                    val requestJson = Gson().toJson(requestJsonForScreeningAllowToDuplicate)
                    val request = StringConverter.getJsonObject(requestJson)
                    val response = onBoardingRepo.createScreeningLog(request)

                    if (response.isSuccessful && screeningEntityAllowToDuplicate != null) {
                        screeningEntityAllowToDuplicate?.let { entity ->
                            val rowId = screeningRepository
                                .savePatientScreeningInformation(
                                    entity.apply {
                                        this.uploadStatus = true
                                    },
                                ).id
                            screeningEntityRowId = rowId
                            patientTrackId = trackIdAllowToDuplicate

                            screeningSaveResponse.postSuccess(
                                Triple(
                                    rowId,
                                    villageSeqIdFunctionAllowToDuplicate?.first,
                                    villageSeqIdFunctionAllowToDuplicate?.second,
                                ),
                            )
                        }
                    } else {
                        screeningSaveResponse.postError()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    screeningSaveResponse.postError()
                }
            }
        }

//    fun updatePatientScreeningInformation(
//        id: Long,
//        generalDetail: String,
//        referredSiteEntity: ReferredSiteEntity?
//    ) {
//        viewModelScope.launch(dispatcherIO) {
//            screeningLiveDate.postLoading()
//            try {
//                screeningRepository.updateGeneralDetailsById(id, generalDetail)
//                referredSiteEntity?.let { entity ->
//                    screeningRepository.createOfflineReferredSite(entity)
//                }
//                screeningLiveDate.postValue(Resource(ResourceState.SUCCESS, true))
//            } catch (e: Exception) {
//                screeningLiveDate.postValue(Resource(ResourceState.SUCCESS, false))
//            }
//        }
//    }

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

        fun getSavedSiteDetail(): String? =
            try {
                Gson().toJson(siteDetail)
            } catch (e: Exception) {
                null
            }

        fun getScreeningEntity(rowId: Long) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    screeningEntity.postLoading()
                    val entity = screeningRepository.getScreeningRecordById(rowId)
                    screeningEntity.postSuccess(entity)
                } catch (e: Exception) {
                    screeningEntity.postError()
                }
            }
        }

        fun fetchAccountSiteList(isLevel1: Boolean = false) {
            viewModelScope.launch(dispatcherIO) {
                accountSiteListLiveData.postLoading()
                try {
                    val accountSiteList =
                        if (isLevel1) {
                            SecuredPreference.getUserId().let {
                                screeningRepository.getAccountSiteListByLevel(
                                    it,
                                    SiteEnum.SITE_LEVEL_1.level,
                                )
                            }
                        } else {
                            SecuredPreference.getUserId().let { screeningRepository.getAccountSiteList(it) }
                        }
                    val siteList = Resource(ResourceState.SUCCESS, ArrayList(accountSiteList))
                    accountSiteListLiveData.postValue(siteList)
                } catch (e: Exception) {
                    accountSiteListLiveData.postValue(Resource(ResourceState.ERROR))
                }
            }
        }

//    fun fetchMentalHealthQuestions(id: String, type: String) {
//        viewModelScope.launch(dispatcherIO) {
//            var mhResponse = mentalHealthQuestions.value?.data
//            mentalHealthQuestions.postLoading()
//            try {
//                when (type) {
//                    DefinedParams.PC_BELOW18, DefinedParams.PC_OVER18 -> {
//                        val pcBelow18 =
//                            onBoardingRepository.getMHQuestionsByType(type = DefinedParams.PC_BELOW18)
//                        val pcAbove18 =
//                            onBoardingRepository.getMHQuestionsByType(type = DefinedParams.PC_OVER18)
//                        if (mhResponse == null)
//                            mhResponse = HashMap()
//
//                        mhResponse[DefinedParams.PC_BELOW18] =
//                            LocalSpinnerResponse(
//                                tag = DefinedParams.PISBelow18,
//                                response = pcBelow18
//                            )
//                        mhResponse[DefinedParams.PC_OVER18] =
//                            LocalSpinnerResponse(
//                                tag = DefinedParams.PISAbove18,
//                                response = pcAbove18
//                            )
//                    }
//
//                    else -> {
//                        val questions = onBoardingRepository.getMHQuestionsByType(type = type)
//                        mhResponse = HashMap()
//                        mhResponse[type] = LocalSpinnerResponse(tag = id, response = questions)
//                    }
//                }
//
//                mentalHealthQuestions.postValue(Resource(ResourceState.SUCCESS, mhResponse))
//            } catch (e: Exception) {
//                mentalHealthQuestions.postValue(Resource(ResourceState.ERROR))
//            }
//        }
//    }

        fun updateSiteDetail() {
//        SecuredPreference.getSelectedSiteEntity()?.apply {
//            siteDetail = SiteDetails(
//                siteId = this.id,
//                siteName = this.name,
//                referredSiteId = this.id,
//                tenantId = this.tenantId,
//                category = if (SecuredPreference.getSelectedSiteEntity()?.role == RoleConstant.HEALTH_SCREENER) TranslatedStaticStrings.Community else TranslatedStaticStrings.Facility
//            )
//        }
        }

        fun getLevel6Sites() {
            viewModelScope.launch(dispatcherIO) {
                try {
                    val accountSiteList = SecuredPreference.getUserId()?.let {
                        if (CommonUtils.isHealthScreener()) {
                            onBoardingRepo.getSiteList(true, it)
                        } else {
                            screeningRepository.getAccountSiteListByLevel(
                                it,
                                SiteEnum.SITE_LEVEL_6.level,
                            )
                        }
                    }
                    val list = accountSiteList?.let { ArrayList(it) }
                    ccSites.postValue(
                        list?.ifEmpty { null },
                    )
                } catch (e: Exception) {
                    // error block
                }
            }
        }

        fun setUpazilaSiteDetails() {
            // Only for BD
//        val site = SecuredPreference.getSelectedSiteEntity()
//        accountSiteListLiveData.value?.data?.let { siteList ->
//            if (siteList.isNotEmpty()) {
//                siteList[0].apply {
//                    siteDetail = SiteDetails(
//                        siteId = this.id,
//                        siteName = this.name,
//                        referredSiteId = site?.id ?: MedicalReviewConstant.DefaultSelectID,
//                        tenantId = this.tenantId,
//                        category = if (site?.role == RoleConstant.HEALTH_SCREENER) TranslatedStaticStrings.Community else TranslatedStaticStrings.Facility
//                    )
//                }
//            }
//        }
        }

        fun updateMySequenceCode(
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

        fun fetchLevel1Site(postValue: Boolean) {
            viewModelScope.launch(dispatcherIO) {
                try {
                    val response = onBoardingRepo.getUpazilaList()
                    upazilaResponse.postValue(
                        Resource(
                            ResourceState.SUCCESS,
                            Pair(postValue, LocalSpinnerResponse(DefinedParams.UPAZILA, response)),
                        ),
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        // Validate Session On Screening Page
        fun validateSessionOnScreening() {
            if (connectivityManager.isNetworkAvailable()) {
                viewModelScope.launch(dispatcherIO) {
                    try {
                        validateSessionLiveData.postLoading()
                        val session = onBoardingRepo.validateSession()
                        validateSessionLiveData.postSuccess(session.body())
                    } catch (e: Exception) {
                        validateSessionLiveData.postError()
                    }
                }
            }
        }
    }
