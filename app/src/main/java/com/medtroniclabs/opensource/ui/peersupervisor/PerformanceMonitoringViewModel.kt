package com.medtroniclabs.opensource.ui.peersupervisor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.medtroniclabs.opensource.appextensions.postLoading
import com.medtroniclabs.opensource.appextensions.toString
import com.medtroniclabs.opensource.common.DateUtils.DATE_FORMAT_yyyyMMdd
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.LIST_LIMIT
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.PerformanceFilterModel
import com.medtroniclabs.opensource.data.performance.CHWPerformanceMonitoring
import com.medtroniclabs.opensource.data.performance.CheckBoxSpinnerData
import com.medtroniclabs.opensource.data.performance.ChwVillageFilterModel
import com.medtroniclabs.opensource.data.performance.FilterPreference
import com.medtroniclabs.opensource.data.performance.PerformanceReportRequest
import com.medtroniclabs.opensource.data.performance.Preference
import com.medtroniclabs.opensource.di.IoDispatcher
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.resource.Resource
import com.medtroniclabs.opensource.network.utils.ConnectivityManager
import com.medtroniclabs.opensource.repo.PerformanceMonitoringRepository
import com.medtroniclabs.opensource.ui.BaseViewModel
import com.medtroniclabs.opensource.ui.peersupervisor.adapter.PerformanceReportDataSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.util.Calendar
import javax.inject.Inject

private const val INT_SENTINEL: Int = Int.MIN_VALUE
private const val LONG_SENTINEL: Long = Long.MIN_VALUE
private const val STRING_SENTINEL: String = "STRING_SENTINEL"

@HiltViewModel
class PerformanceMonitoringViewModel @Inject constructor(
    @IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val performanceMonitoringRepo: PerformanceMonitoringRepository,
    private val apiHelper: ApiHelper
) : BaseViewModel(dispatcherIO) {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    val userFilterPreferenceLiveData = MutableLiveData<Resource<FilterPreference>>()
    val saveUserFilterPreferenceLiveData = MutableLiveData<Resource<FilterPreference>>()
    var userPreference: Preference? = null

    val filterChwListLiveData = MutableLiveData<Resource<List<ChwVillageFilterModel>>>()
    val chwFilterListLiveData = MutableLiveData<List<CheckBoxSpinnerData>>()
    val villageFilterListLiveData = MutableLiveData<List<CheckBoxSpinnerData>>()

    val anyFilterChanged = MutableLiveData<Boolean>()

    val filterModel: PerformanceFilterModel = PerformanceFilterModel()

    private val _refreshTrigger = MutableLiveData<PerformanceReportRequest>()
    val dataFlow: LiveData<PagingData<CHWPerformanceMonitoring>> = _refreshTrigger.switchMap { request ->
        Pager(
            config = PagingConfig(
                pageSize = LIST_LIMIT,
                enablePlaceholders = false
            ),
            pagingSourceFactory = { PerformanceReportDataSource(apiHelper, request) }
        ).flow.cachedIn(viewModelScope).asLiveData()
    }

    init {
        getUserFilterPreference()
        anyFilterChanged.value = false
    }

    private fun getUserFilterPreference() {
        viewModelScope.launch(dispatcherIO) {
            userFilterPreferenceLiveData.postLoading()
            val request = FilterPreference(userId = SecuredPreference.getUserId().toString(), preference = null)
            userFilterPreferenceLiveData.postValue(performanceMonitoringRepo.getUserFilterPreference(request))

            getLinkedChwDetails()
        }
    }

    fun saveFilterPreference(fromDate: String, toDate: String, userIds : List<Long>, villageIds: List<Long>) {
        viewModelScope.launch(dispatcherIO) {
            saveUserFilterPreferenceLiveData.postLoading()
            val request = FilterPreference(
                userId = SecuredPreference.getUserId().toString(),
                preference = Preference(
                    fromDate = fromDate,
                    toDate = toDate,
                    userIds = userIds,
                    villageIds = villageIds
                )
            )
            saveUserFilterPreferenceLiveData.postValue(performanceMonitoringRepo.saveUserFilterPreference(request))
        }
    }

    fun refreshData() {

    }

    fun getLinkedChwDetails() {
        viewModelScope.launch(dispatcherIO) {
            filterChwListLiveData.postLoading()
            filterChwListLiveData.postValue(performanceMonitoringRepo.getLinkedChwDetails())
        }
    }

    fun updateUserPreference(preference: Preference) {
        userPreference = preference
    }

    fun initPagination(preference: Preference) {
        val isUnlocked = (preference.fromDate.isNullOrEmpty() && preference.toDate.isNullOrEmpty())

        val fromToDate = preference.getFromToDate()
        val fromDate = fromToDate.first.toString(DATE_FORMAT_yyyyMMdd)
        val toDate = fromToDate.second.toString(DATE_FORMAT_yyyyMMdd)
        val userIds = if (isUnlocked) null else preference.userIds
        val villageIds = if (isUnlocked) null else preference.villageIds

        _refreshTrigger.postValue(
            PerformanceReportRequest(
                userIds = userIds,
                villageIds = villageIds,
                fromDate = fromDate,
                toDate = toDate
            )
        )

    }

    fun updatePaginationWithNewFilter(
        fromDate: String,
        toDate: String,
        userIds: List<Long>,
        villageIds: List<Long>,
        shouldSave: Boolean
    ) {
        if (shouldSave)
            saveFilterPreference(fromDate, toDate, userIds, villageIds)

        _refreshTrigger.postValue(
            PerformanceReportRequest(
                userIds = userIds,
                villageIds = villageIds,
                fromDate = fromDate,
                toDate = toDate
            )
        )
    }

    fun updateChwFilterListLiveData() {
        val selectedIds = hashSetOf<Long>()
        val isUnlocked = (userPreference?.fromDate.isNullOrEmpty() && userPreference?.toDate.isNullOrEmpty())
        userPreference?.userIds?.forEach {
            selectedIds.add(it)
        }

        val allCHWs = filterChwListLiveData.value?.data

        val chwList = mutableListOf<CheckBoxSpinnerData>()
        allCHWs?.forEach { item ->
            val isSelected = if (isUnlocked) true else selectedIds.contains(item.id)
            chwList.add(CheckBoxSpinnerData(item.id, "${item.firstName} ${item.lastName}", isSelected))
        }

        chwFilterListLiveData.postValue(chwList);
    }

    fun updateVillageListLiveData(selectedCHWs: List<CheckBoxSpinnerData>, shouldSelectAll: Boolean = false) {
        val selectedIds = hashSetOf<Long>()
        val isUnlocked = (userPreference?.fromDate.isNullOrEmpty() && userPreference?.toDate.isNullOrEmpty())
        if (!shouldSelectAll){
            userPreference?.villageIds?.forEach {
                selectedIds.add(it.toLong())
            }
        }

        val allCHWs = filterChwListLiveData.value?.data

        val villageList = mutableListOf<CheckBoxSpinnerData>()
        selectedCHWs.forEach { selectedChw ->
            val villages = allCHWs?.find { it.id == selectedChw.id }?.villages
            villages?.forEach { village ->
                val isSelected = if (shouldSelectAll) {
                    true
                } else {
                    if (isUnlocked) {
                        true
                    } else {
                        selectedIds.contains(village.id)
                    }
                }
                villageList.add(
                    CheckBoxSpinnerData(
                        village.id,
                        village.name,
                        isSelected,
                        village.userId
                    )
                )
            }
        }

        villageFilterListLiveData.postValue(villageList)
    }

    fun getAllCHWAsSelected() : List<CheckBoxSpinnerData> {
        val allCHWs = filterChwListLiveData.value?.data

        val chwList = mutableListOf<CheckBoxSpinnerData>()
        allCHWs?.forEach { item ->
            chwList.add(CheckBoxSpinnerData(item.id, "${item.firstName} ${item.lastName}", true))
        }

        return chwList
    }

    fun getAllVillagesAsSelected(): List<CheckBoxSpinnerData> {
        val allCHWs = filterChwListLiveData.value?.data

        val villageList = mutableListOf<CheckBoxSpinnerData>()
        allCHWs?.forEach { selectedChw ->
            selectedChw.villages.forEach { village ->
                villageList.add(CheckBoxSpinnerData(village.id, village.name, true, village.userId))
            }
        }

        return villageList
    }

    fun updateFilter(
        year: Int? = INT_SENTINEL,
        month: Int? = INT_SENTINEL,
        startDate: Long? = LONG_SENTINEL,
        endDate: Long? = LONG_SENTINEL,
        fromDate: String? = STRING_SENTINEL,
        toDate: String? = STRING_SENTINEL
    ) {
        if (year != INT_SENTINEL)
            filterModel.year = year

        if (month != INT_SENTINEL)
            filterModel.month = month

        if (startDate != LONG_SENTINEL)
            filterModel.startDate = startDate

        if (endDate != LONG_SENTINEL)
            filterModel.endDate = endDate

        if (fromDate != STRING_SENTINEL)
            filterModel.fromDate = fromDate

        if (toDate != STRING_SENTINEL)
            filterModel.toDate = toDate
    }

    fun getYearList(): ArrayList<Map<String, Any>> {
        val list = ArrayList<Map<String, Any>>()
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        val startYear = 2024 // Since sl first release going by 2024

        for (year in currentYear downTo startYear) {
            val map = HashMap<String, Any>()
            map[DefinedParams.NAME] = year.toString()
            map[DefinedParams.ID] = year
            list.add(map)
        }
        return list
    }

    fun getMonthList(): ArrayList<Map<String, Any>> {

        val list = ArrayList<Map<String, Any>>()
        val monthNames: Array<String> = DateFormatSymbols().months
        val monthList = monthNames.take(12)
        monthList.forEachIndexed { index, month ->
            val map = HashMap<String, Any>()
            map[DefinedParams.NAME] = month
            map[DefinedParams.ID] = index
            list.add(map)
        }
        return list
    }
}