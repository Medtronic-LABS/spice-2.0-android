package com.medtroniclabs.opensource.ncd.followup.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.postSuccess
import com.medtroniclabs.opensource.common.DateUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.data.model.ChipViewItemModel
import com.medtroniclabs.opensource.db.entity.HealthFacilityEntity
import com.medtroniclabs.opensource.db.entity.NCDCallDetails
import com.medtroniclabs.opensource.db.entity.NCDFollowUp
import com.medtroniclabs.opensource.db.entity.VillageEntity
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.mappingkey.Screening
import com.medtroniclabs.opensource.ncd.data.CustomDate
import com.medtroniclabs.opensource.ncd.data.FollowUpUpdateRequest
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.data.RegisterCallResponse
import com.medtroniclabs.opensource.ncd.data.SortModelForFollowUp
import com.medtroniclabs.opensource.ncd.followup.NCDFollowUpUtils
import com.medtroniclabs.opensource.ncd.followup.adapter.NCDFollowUpDataSource
import com.medtroniclabs.opensource.ncd.followup.fragment.NCDFollowUpFilterEnum
import com.medtroniclabs.opensource.ncd.followup.repo.NCDFollowUpRepo
import com.medtroniclabs.opensource.ncd.medicalreview.repo.NCDMedicalReviewRepository
import com.medtroniclabs.opensource.ncd.screening.repo.ScreeningRepository
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.SingleLiveEvent
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.ui.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.inject.Inject

@HiltViewModel
class NCDFollowUpViewModel @Inject constructor(
    private val apiHelper: ApiHelper,
    private val ncdFollowUpRepo: NCDFollowUpRepo,
    private var roomHelper: RoomHelper,
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private var ncdMedicalReviewRepo: NCDMedicalReviewRepository,
    private val screeningRepository: ScreeningRepository
) : BaseViewModel(dispatcherIO) {
    var spanCount: Int = DefinedParams.span_count_1
    var searchText = ""
    var type = ""
    var sortModel : SortModelForFollowUp? = null
    var customDate: CustomDate? = null
    var dateRange: String? = null
    var totalPatientCount = MutableLiveData<Int?>()
    var filterCount = MutableLiveData<Int>()
    var sortCount = MutableLiveData<Int>()
    var filterSet = MutableLiveData<Boolean>()
    var getPatientRegisterResponse = SingleLiveEvent<Resource<RegisterCallResponse>>()
    var statusUpdateResponse = SingleLiveEvent<Resource<HashMap<String, Any>>>()
    var remainingAttempts = listOf<ChipViewItemModel>()
    var selectedPatient: PatientFollowUpEntity? = null
    val callResultHashMap = HashMap<String, Any>()
    val patientStatusHashMap = HashMap<String, Any>()
    val unSuccessfulHashMap = HashMap<String, Any>()

    val patientsDataSource =
        Pager(config = PagingConfig(pageSize = DefinedParams.LIST_LIMIT), pagingSourceFactory = {
            NCDFollowUpDataSource(
                apiHelper = apiHelper,
                ncdFollowUpRepo = ncdFollowUpRepo,
                roomHelper = roomHelper,
                searchText = searchText,
                sortModel = sortModel,
                customDate = if (dateRange == NCDFollowUpFilterEnum.CUSTOMISE.title) customDate else null,
                dateRange = dateRange,
                type = type,
                remainingAttempts = remainingAttempts.mapNotNull { it.id }
            ) { getPatientsCount ->
                totalPatientCount.postValue(getPatientsCount)
            }
        }).flow


    fun getPatientCallRegister() {
        viewModelScope.launch(dispatcherIO) {
            getPatientRegisterResponse.postLoading()
            getPatientRegisterResponse.postValue(ncdFollowUpRepo.getPatientCallRegister())
        }
    }


    fun updatePatientCallRegister(request: FollowUpUpdateRequest) {
        viewModelScope.launch(dispatcherIO) {
            if (request.isInitiated) {
                setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDCallInitialed+" "+ request.type.takeIf { !it.isNullOrBlank() },
                    isCompleted = true
                )
            }
            statusUpdateResponse.postLoading()
            statusUpdateResponse.postValue(ncdFollowUpRepo.updatePatientCallRegister(request))
        }
    }

    fun filterLiveData() {
        setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.NCDFollowUpFilter + " " + type.takeIf { it.isNotBlank() },
            isCompleted = true
        )
        filterSet.postValue(true)
        val count = listOfNotNull(
            dateRange.takeIf { !it.isNullOrBlank() },
            remainingAttempts.takeIf { !it.isNullOrEmpty() }
        ).size

        if (count > 0) {
            filterCount.postValue(count)
        } else {
            filterCount.postValue(0)
        }
    }


    // Follow up Offline
    var selectedFollowUpPatient: NCDFollowUp? = null
    var searchTextOffline: String = ""
    fun searchLiveDataForOffline(text: String) {
        this.searchTextOffline = text
        searchTextOfflineLiveData.value = true
    }

    private val searchTextOfflineLiveData = MutableLiveData<Boolean>()
    val getFollowUpData: LiveData<List<NCDFollowUp>> = searchTextOfflineLiveData.switchMap {
        ncdFollowUpRepo.getNCDFollowUpData(
            typeOffline,
            searchTextOffline,
            getDateBasedOnChip(),
            sortTriple.first,
            sortTriple.second
        )
    }
    var typeOffline = ""
    var totalPatientCountOffline = MutableLiveData<Int>()

    val saveCallDetails = MutableLiveData<Resource<NCDCallDetails?>>()
    val updateCallLiveData = MutableLiveData<Resource<NCDFollowUp>>()
    val getInitialLiveData = MutableLiveData<Resource<NCDFollowUp?>>()

    var filterByVillage: List<ChipViewItemModel> = listOf()
    var filterByDateRange: List<ChipViewItemModel> = listOf()
    var data: NCDFollowUp? = null
    var selectedHealthFacilityId: Long? = null
    var selectedHealthFacilityName: String? = null

    fun filterFollowUpOfflineLiveData() {
        setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.NCDFollowUpFilter + " " + typeOffline.takeIf { it.isNotBlank() },
            isCompleted = true
        )
        searchTextOfflineLiveData.value = true
        val count = listOfNotNull(
            filterByDateRange.takeIf { !it.isNullOrEmpty() },
            filterByVillage.takeIf { !it.isNullOrEmpty() }
        ).size

        if (count > 0) {
            filterCount.postValue(count)
        } else {
            filterCount.postValue(0)
        }
    }

    fun insertNCDCallDetails(followUp: NCDCallDetails) {
        viewModelScope.launch {
            try {
                saveCallDetails.postLoading()
                val updatedFollowUp = ncdFollowUpRepo.insertNCDCallDetails(followUp)
                saveCallDetails.postSuccess(updatedFollowUp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun updateInitial(value: NCDFollowUp) {
        viewModelScope.launch(dispatcherIO) {
            try {
                updateCallLiveData.postLoading()
                val updatedFollowUp = ncdFollowUpRepo.updatedCallInitiatedCall(value)
                setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDCallInitialed+" "+ value.type.takeIf { !it.isNullOrBlank() },
                    isCompleted = true
                )
                updateCallLiveData.postSuccess(updatedFollowUp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getInitial() {
        viewModelScope.launch(dispatcherIO) {
            try {
                getInitialLiveData.postLoading()
                val updatedFollowUp = ncdFollowUpRepo.getNCDInitiatedCallFollowUp()
                getInitialLiveData.postSuccess(updatedFollowUp)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    var villageListResponse = MutableLiveData<Resource<List<VillageEntity>>>()
    fun getAllVillagesName() {
        viewModelScope.launch(dispatcherIO) {
            villageListResponse.postLoading()
            villageListResponse.postValue(ncdFollowUpRepo.getAllVillagesName())
        }
    }

    private fun getDateBasedOnChip(): Pair<Long?, Long?>? {
        var result: Pair<Long?, Long?>? = null

        val currentDay = DateUtils.getCurrentDayMonthYear()
        val startOfDay =
            LocalDateTime.of(currentDay.third, currentDay.second, currentDay.first, 0, 0, 0)
        val endOfDay =
            LocalDateTime.of(currentDay.third, currentDay.second, currentDay.first, 23, 59, 59)

        val startMillis = startOfDay.toInstant(ZoneOffset.UTC).toEpochMilli()
        val endMillis = endOfDay.toInstant(ZoneOffset.UTC).toEpochMilli()
        val oneDayMillis = 24 * 60 * 60 * 1000

        if (filterByDateRange.isEmpty()) {
            return null
        }
        for (chip in filterByDateRange) {
            result = when (chip.name.lowercase()) {
                NCDFollowUpUtils.today -> Pair(startMillis, endMillis)
                Screening.Yesterday.lowercase() -> Pair(
                    startMillis - oneDayMillis,
                    endMillis - oneDayMillis
                )

                NCDFollowUpUtils.customise -> {
                    // Example: Provide a default custom date range or retrieve it from somewhere
                    customDate?.let {
                        Pair(
                            DateUtils.convertToTimestampWithoutZone(it.startDate, true),
                            DateUtils.convertToTimestampWithoutZone(it.endDate, false)
                        )
                    }
                }

                else -> null
            }
        }
        return result
    }

    private var sortTriple: Pair<Boolean?, String?> = Pair(null, null)

    fun sortTriple() {
        setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.NCDFollowUpSort + " " + typeOffline.takeIf { it.isNotBlank() },
            isCompleted = true
        )
        val (triple, list) = when (typeOffline) {
            NCDFollowUpUtils.SCREENED -> sortModel?.isScreeningDueDate to null
            NCDFollowUpUtils.Assessment_Type -> sortModel?.isAssessmentDueDate to null
            NCDFollowUpUtils.Defaulters_Type -> sortModel?.isMedicalReviewDueDate to null
            NCDFollowUpUtils.LTFU_Type -> {
                val isAssessmentDue = sortModel?.isAssessmentDueDate == true
                val value =
                    if (isAssessmentDue) DefinedParams.Assessment else if (sortModel?.isMedicalReviewDueDate == true)
                        NCDFollowUpUtils.medical_review else null
                isAssessmentDue to value
            }

            else -> null to null
        }
        sortTriple = Pair(triple, list)
        val count = listOf(
            sortModel?.isScreeningDueDate,
            sortModel?.isAssessmentDueDate,
            sortModel?.isMedicalReviewDueDate
        ).count { it == true }
        sortCount.postValue(count)
        searchTextOfflineLiveData.value = true
    }

    val getFollowUpReasonList = MutableLiveData<List<ShortageReasonEntity>>()
    fun getFollowUpReasonList() {
        viewModelScope.launch(dispatcherIO) {
            val deleteList =
                ncdMedicalReviewRepo.getNCDShortageReason(NCDFollowUpUtils.REASON_CONSTANT)
            val list = ArrayList(deleteList)
            if (list.isNotEmpty()) {
                val itemIndex =
                    list.indexOfFirst { it.name.contains(DefinedParams.Other, ignoreCase = true) }
                if (itemIndex >= 0 && (itemIndex + 1) != list.size) {
                    val item = list.removeAt(itemIndex)
                    list.add(item)
                }
            }
            getFollowUpReasonList.postValue(list)
        }
    }

    private var getSites = MutableLiveData<Boolean>()
    val getSitesLiveData: LiveData<List<HealthFacilityEntity>> =
        getSites.switchMap {
            screeningRepository.getUserHealthFacilityEntity()
        }

    fun getSites(isTrigger: Boolean) {
        getSites.value = isTrigger
    }
}