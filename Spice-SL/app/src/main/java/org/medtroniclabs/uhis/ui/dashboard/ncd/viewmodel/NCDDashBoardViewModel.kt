package org.medtroniclabs.uhis.ui.dashboard.ncd.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.CustomDateModel
import org.medtroniclabs.uhis.data.NCDUserDashboardRequest
import org.medtroniclabs.uhis.data.NCDUserDashboardResponse
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.ui.BaseFilterViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import org.medtroniclabs.uhis.ui.dashboard.ncd.repository.DashboardLocalRepository
import javax.inject.Inject

@HiltViewModel
class NCDDashBoardViewModel @Inject constructor(
    override val metaRepository: MetaRepository,
    private val dashboardLocalRepository: DashboardLocalRepository,
    private val houseHoldRepository: HouseHoldRepository,
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseFilterViewModel(dispatcherIO, metaRepository) {
    var userDashboardDetails = MutableLiveData<Resource<NCDUserDashboardResponse>>()
    val menuListLiveData = MutableLiveData<List<String>?>()
    val clinicalWorkflowNamesLowerLiveData = MutableLiveData<Set<String>>()
    val filterUiData = MutableLiveData<Resource<HouseHoldFilterUiData>>()
    private val filterLiveData = MutableLiveData(DashboardSearchFilter())

    val isFoPo = CommonUtils.isFoOrPo()

    fun getUserDashboardDetails(request: NCDUserDashboardRequest) {
        viewModelScope.launch(dispatcherIO) {
            userDashboardDetails.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDDashBoardCount,
                isCompleted = true,
            )
            val local = dashboardLocalRepository.getLocalDashboardDetails(request)
            userDashboardDetails.postValue(Resource(state = ResourceState.SUCCESS, data = local))
        }
    }

    fun fetchDashboardForDateRange(customDate: CustomDateModel) {
        viewModelScope.launch(dispatcherIO) {
            val filter = filterLiveData.value ?: DashboardSearchFilter()
            val ssIds = resolveEffectiveSsIds(filter)
            val subVillageIds = filter.filterBySubVillages.mapNotNull { it.id }
            getUserDashboardDetails(
                NCDUserDashboardRequest(
                    customDate = customDate,
                    userId = SecuredPreference.getUserFhirId(),
                    filterBySs = ssIds.ifEmpty { null },
                    filterBySubVillages = subVillageIds.ifEmpty { null },
                ),
            )
        }
    }

    private suspend fun resolveEffectiveSsIds(filter: DashboardSearchFilter): List<Long> {
        val fromChips = filter.filterBySs.mapNotNull { it.id }
        if (fromChips.isNotEmpty()) return fromChips
        if (!isFoPo || filter.filterSk == -1L) return emptyList()

        val cached = filterUiData.value
            ?.data
            ?.ssList
            ?.map { it.id }
            .orEmpty()
        if (cached.isNotEmpty()) return cached

        val result = houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(filter.filterSk)
        if (result.state == ResourceState.SUCCESS) {
            filterUiData.postValue(result)
            return result.data
                ?.ssList
                ?.map { it.id }
                .orEmpty()
        }
        return emptyList()
    }

    fun getMenus() {
        viewModelScope.launch(dispatcherIO) {
            val response = metaRepository.getMenu()
            val menus = response.data?.map { it.menuId }
            menuListLiveData.postValue(menus)
        }
    }

    fun loadDashboardClinicalWorkflowGate() {
        viewModelScope.launch(dispatcherIO) {
            clinicalWorkflowNamesLowerLiveData.postValue(
                metaRepository.getClinicalWorkflowWorkflowNamesLower(),
            )
        }
    }

    fun setFilterLiveData(
        ssFilter: List<ChipViewItemModel>? = null,
        subVillagesFilter: List<ChipViewItemModel>? = null,
        filterSk: Long? = null,
    ) {
        val filter = filterLiveData.value ?: DashboardSearchFilter()
        ssFilter?.let { filter.filterBySs = it }
        subVillagesFilter?.let { filter.filterBySubVillages = it }
        filterSk?.let { filter.filterSk = it }
        filterLiveData.value = filter
    }

    fun getFilterLiveData(): MutableLiveData<DashboardSearchFilter> = filterLiveData

    private var lastLoadedFoPoKormiId: Long? = null

    fun getFilterUiData() {
        viewModelScope.launch(dispatcherIO) {
            lastLoadedFoPoKormiId = null
            filterUiData.postLoading()
            val initial = houseHoldRepository.getHouseHoldFilterUiDataForServiceRecipient()
            filterUiData.postValue(initial)
            if (initial.state != ResourceState.SUCCESS) return@launch

            val kormiId = filterLiveData.value?.filterSk ?: return@launch
            if (kormiId == -1L) return@launch
            lastLoadedFoPoKormiId = kormiId
            filterUiData.postValue(houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(kormiId))
        }
    }

    fun loadSsListForSelectedKormi(kormiId: Long) {
        if (lastLoadedFoPoKormiId == kormiId) {
            val current = filterUiData.value?.data
            if (current?.ssList?.isNotEmpty() == true) return
        }
        lastLoadedFoPoKormiId = kormiId
        viewModelScope.launch(dispatcherIO) {
            filterUiData.postLoading()
            filterUiData.postValue(houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(kormiId))
        }
    }
}

data class DashboardSearchFilter(
    var filterBySs: List<ChipViewItemModel> = listOf(),
    var filterBySubVillages: List<ChipViewItemModel> = listOf(),
    var filterSk: Long = -1L,
)
