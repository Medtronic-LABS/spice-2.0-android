package org.medtroniclabs.uhis.ui.patient.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.postError
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.appextensions.postSuccess
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.FilterEnum
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.model.FollowUpPatientDetailsResponse
import org.medtroniclabs.uhis.data.model.MedicalReviewBaseRequest
import org.medtroniclabs.uhis.data.model.PatientDataModel
import org.medtroniclabs.uhis.data.model.PatientDetails
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.model.RegisterCallResponse
import org.medtroniclabs.uhis.data.model.ResponseDivisionDistrictUpazilas
import org.medtroniclabs.uhis.data.model.SiteRoleResponse
import org.medtroniclabs.uhis.data.model.SortModel
import org.medtroniclabs.uhis.data.model.UpdatePatientCallRegister
import org.medtroniclabs.uhis.data.servicerecipient.PatientHistoryData
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.formgeneration.config.DefinedParams
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.MedicalReviewRepository
import org.medtroniclabs.uhis.repo.OnBoardingRepository
import org.medtroniclabs.uhis.ui.patient.GetPatientsCount
import org.medtroniclabs.uhis.ui.patient.LIST_LIMIT
import org.medtroniclabs.uhis.ui.patient.PatientsDataSource
import org.medtroniclabs.uhis.ui.patient.UIConstants
import javax.inject.Inject

@HiltViewModel
class PatientListViewModel @Inject constructor(
    private val apiHelper: ApiHelper,
    private val medicalReviewRepo: MedicalReviewRepository,
    private val onBoardingRepo: OnBoardingRepository,
    @IoDispatcher private val dispatcherIO: CoroutineDispatcher,
) : ViewModel(), GetPatientsCount {
    // Origin
    var origin = ""

    // Patient list - Grid count
    var spanCount: Int = DefinedParams.SPAN_COUNT_1

    // Total patient count
    var totalPatientCount = MutableLiveData<String>()

    // Create visit response
    val patientVisitResponse = MutableLiveData<Resource<PatientDetails>>()

    // Search keys
    var searchQRValue: String? = null
    var searchPatientId = ""

    // Patient List (or) Search
    var isBangladesh = true
    val isOfflineEnabled = false

    // Sort and Filter
    var sort: SortModel? = null
    var filter: FilterModel? = null

    var isQRSearch = false

    var registerCallResponse = MutableLiveData<Resource<RegisterCallResponse>>()
    var getPatientRegisterResponse = MutableLiveData<Resource<RegisterCallResponse>>()
    var statusUpdateResponse = MutableLiveData<Resource<UpdatePatientCallRegister>>()
    var unionListResponse = MutableLiveData<Resource<List<VillageEntity>>>()
    var villageListResponse = MutableLiveData<Resource<List<SubVillageEntity>>>()
    var userUnionListResponse = MutableLiveData<List<Long>>()
    val searchParaCounselorResponse = MutableLiveData<Resource<ArrayList<SiteRoleResponse>>>()
    val patientDetailsFollowUp = MutableLiveData<Resource<FollowUpPatientDetailsResponse>>()

    val upazilaListLiveData = MutableLiveData<Resource<List<ResponseDivisionDistrictUpazilas>>>()

    val tcPatientDetailLiveData = MutableLiveData<Resource<List<PatientHistoryData>>>()

    var name: String? = null
    var age: Long? = null
    var gender: String? = null
    var mobile: String? = null
    var callType: String? = null
    var callId: Long? = null

    var followUpType: String = ""

    var isCallRegistered = MutableLiveData(false)

    var userTouchMade: Boolean = false

    // Add variables for call result dialog state
    var callResultStatus: String = ""
    var reason: String? = null
    var otherReason: String? = null
    var unsuccessfulReason: String? = null
    var villageResponse = MutableLiveData<VillageEntity>()
    var isWillingToVisit: Boolean? = null
    var isCalled: Boolean? = null

    var selectedNavTab: Int? = null
    var callStartTime: Long? = null
    var followUpPatientDetailsResponse: FollowUpPatientDetailsResponse? = null

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    var isFilteredUnion: Boolean = false

    private val refreshTrigger = MutableSharedFlow<Int>(replay = 1)
    private var triggerCount = 0

    @OptIn(ExperimentalCoroutinesApi::class)
    val patientsDataSource: Flow<PagingData<PatientListResModel>> = refreshTrigger.flatMapLatest {
        Pager(
            config = PagingConfig(pageSize = LIST_LIMIT, enablePlaceholders = false),
            initialKey = null,
            pagingSourceFactory = {
                PatientsDataSource(
                    isSiteBasedSearch = isSiteBasedSearch(),
                    searchModel = PatientDataModel(
                        searchId = searchPatientId,
                        operatingUnitId = null,
                        accountId = null,
                        isLabtestReferred = isLabTestReferred(),
                        isMedicationPrescribed = isMedicationPrescribed(),
                        patientSort = getSortBy(),
                        patientFilter = getFilterBy(),
                        searchQRValue = searchQRValue,
                        isParaCounsellingDisabled = null,
                        counsellorId = null,
                        unionId = null,
                        userId = null,
                        prescribedSiteId = null,
                    ),
                    apiHelper = apiHelper,
                    getPatientsCount = this,
                    origin = origin,
                    isPsychologist = null,
                    followUpType = null,
                    isFilteredUnion,
                )
            },
        ).flow.cachedIn(viewModelScope)
    }

    fun refreshPatients() {
        triggerCount++
        refreshTrigger.tryEmit(triggerCount)
    }

    override fun patientsCount(count: String) {
        totalPatientCount.postValue(count)
    }

    private fun isSiteBasedSearch(): Boolean = origin != UIConstants.ENROLLMENT_UNIQUE_ID

    private fun isLabTestReferred(): Boolean? =
        when (origin) {
            UIConstants.INVESTIGATION -> true
            else -> null
        }

    private fun isMedicationPrescribed(): Boolean? =
        when (origin) {
            UIConstants.PRESCRIPTION_UNIQUE_ID -> true
            else -> null
        }

    private fun getSortBy(): SortModel? {
        if (isQRSearch) {
            return null
        }
        return SortModel().apply {
            this.isCVDRisk = true
        }
    }

    private fun getFilterBy(): FilterModel? {
        if (isQRSearch) {
            return null
        }

        return if (filter != null) {
            filter
        } else {
            FilterModel(
                isDefaultPcFilter = true,
                patientStatus = FilterEnum.NOT_ENROLLED.name,
            )
        }
    }

    fun isPatientListRequired(): Boolean {
        var isListRequired = when (origin) {
            UIConstants.MY_PATIENTS_UNIQUE_ID,
            UIConstants.PRESCRIPTION_UNIQUE_ID,
            UIConstants.INVESTIGATION,
            UIConstants.LIFESTYLE,
            UIConstants.PSYCHOLOGICAL,
            UIConstants.FOLLOW_UP,
            -> true

            else -> false
        }
        if (origin == UIConstants.PRESCRIPTION_UNIQUE_ID && (CommonUtils.isCHCP() || CommonUtils.isNurse())) {
            isListRequired =
                false
        }
        return isListRequired
    }

    private val _dialEvent = MutableLiveData<Event<String>>()
    val dialEvent: LiveData<Event<String>> = _dialEvent

    fun triggerDial(phoneNumber: String) {
        _dialEvent.value = Event(phoneNumber)
    }

    open class Event<out T>(private val content: T) {
        private var hasBeenHandled = false

        fun getContentIfNotHandled(): T? =
            if (hasBeenHandled) {
                null
            } else {
                hasBeenHandled = true
                content
            }

        fun peekContent(): T = content
    }

    fun sortCount(): Int {
        var isSortApplied = false
        sort?.let { sort ->
            isSortApplied = sort.isRedRisk != null ||
                sort.isLatestAssessment != null ||
                sort.isLastReviewDate != null ||
                sort.isHighLowBp != null ||
                sort.isHighLowBg != null ||
                sort.isAssessmentDueDate != null ||
                sort.isScreeningDueDate != null ||
                sort.isUpdated != null ||
                sort.isCVDRisk != null ||
                sort.isDateRange != null ||
                sort.isNextReviewDate != null
        }
        return if (isSortApplied) 1 else 0
    }

    fun filterCount(): Int {
        val count = filter?.let {
            listOf(
                it.medicalReviewDate != null,
                it.isRedRiskPatient == true,
                it.patientStatus != null,
                it.cvdRiskLevel != null,
                it.riskStatus != null,
                it.assessmentDate != null,
                it.labTestReferredDate != null,
                it.medicationPrescribedDate != null,
                it.diagnosis != null,
                it.dateRange != null,
                it.registrationDate != null,
                it.customRegistrationDate != null,
                it.healthCondition != null,
                it.customOption == true,
                it.customRegistrationOption == true,
                !it.isDefaultPcFilter,
                it.sessionDate != null,
                it.villageId != null,
                it.subVillageId != null,
                it.referredSite != null,
                it.remainingAttempts != null,
                it.selectedParaCounselor != null,
                it.callStatus != null,
                it.patientType != null,
                it.countyId != null,
                it.subCountyId != null,
                it.countyId != null && it.subCountyId != null && !it.accountIds.isNullOrEmpty(),
                it.cvdRisk != null,
                it.diagnosisType != null,
            ).count { item -> item }
        } ?: 0
        return count
    }

    fun getTCPatientDetails(
        ctx: Context,
        patientTrackId: Long,
        callType: String? = null,
    ) {
        viewModelScope.launch(dispatcherIO) {
            tcPatientDetailLiveData.postLoading()
//            try {
//                val response =
//                    medicalReviewRepo.getTCPatientRecord(PatientDetailsFollowUp(patientTrackId = patientTrackId, callRegisterId = null))
//                if (response.isSuccessful && response.body()?.entity != null) {
//                    tcPatientDetailLiveData.postSuccess(getFormattedPatientHistory(response.body()?.entity))
//                    if (callType != null) {
//                        patientCallRegister(ctx, RegisterCallRequest(patientTrackId, callType))
//                    }
//                } else {
//                    tcPatientDetailLiveData.postError()
//                }
//
//            } catch (e: Exception) {
//                tcPatientDetailLiveData.postError(e.localizedMessage)
//            }

            tcPatientDetailLiveData.postError()
        }
    }

    fun fetchUnionOrAccountIdList() {
        viewModelScope.launch(dispatcherIO) {
//            if (CommonUtils.isHealthEducator()) {
//                medicalReviewRepo.getAssignedCountSubCountyAccountIds().let { list ->
//                    heAllAccountIds = list.map { it.accountId }.toSet()
//                }
//            }
//            userUnionListResponse.postValue(onBoardingRepo.getUserUnions().map { it._id })
        }
    }

    fun getPatientCallRegister(context: Context) {
        viewModelScope.launch(dispatcherIO) {
//            try {
//                if (connectivityManager.isNetworkAvailable()) {
//                    getPatientRegisterResponse.postLoading()
//                    val response = medicalReviewRepo.getPatientCallRegister()
//                    if (response.isSuccessful) {
//                        getPatientRegisterResponse.postSuccess(response.body()?.entity)
//                    } else {
//                        getPatientRegisterResponse.postError()
//                    }
//                } else {
//                    getPatientRegisterResponse.postError(context.getString(R.string.no_internet_error))
//                }
//            } catch (e: Exception) {
//                getPatientRegisterResponse.postError()
//            }

            getPatientRegisterResponse.postError()
        }
    }

    fun createPatientVisit(
        context: Context,
        request: MedicalReviewBaseRequest,
        initialReview: Boolean,
        patientID: Long,
        age: Int?,
    ) {
        if (connectivityManager.isNetworkAvailable()) {
            patientVisitResponse.postLoading()
            viewModelScope.launch(dispatcherIO) {
                try {
                    val response = medicalReviewRepo.createPatientVisit(request)
                    if (response.isSuccessful) {
                        val res = response.body()
                        if (res?.status == true && res.entity != null) {
                            patientVisitResponse.postSuccess(
                                PatientDetails(
                                    res.entity.id,
                                    initialReview,
                                    patientID,
                                    age,
                                ),
                            )
                        }
                    } else {
                        patientVisitResponse.postError()
                    }
                } catch (e: Exception) {
                    patientVisitResponse.postError()
                }
            }
        } else {
            patientVisitResponse.postError(context.getString(R.string.no_internet_error))
        }
    }

    fun getAllUnionList() {
        viewModelScope.launch(dispatcherIO) {
            val response = onBoardingRepo.getAllVillages()
            unionListResponse.postValue(
                Resource(
                    ResourceState.SUCCESS,
                    response,
                ),
            )
        }
    }

    fun fetchVillageList(unionId: Long) {
        viewModelScope.launch(dispatcherIO) {
            val villages = onBoardingRepo.getSubVillageByVillageId(unionId)
            villageListResponse.postValue(
                Resource(
                    ResourceState.SUCCESS,
                    villages,
                ),
            )
        }
    }
}
