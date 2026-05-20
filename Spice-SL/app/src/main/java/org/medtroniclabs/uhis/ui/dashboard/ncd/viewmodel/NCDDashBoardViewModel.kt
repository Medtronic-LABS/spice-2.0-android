package org.medtroniclabs.uhis.ui.dashboard.ncd.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.data.NCDUserDashboardRequest
import org.medtroniclabs.uhis.data.NCDUserDashboardResponse
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseFilterViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import org.medtroniclabs.uhis.ui.dashboard.ncd.repository.DashboardLocalRepository
import javax.inject.Inject

@HiltViewModel
class NCDDashBoardViewModel @Inject constructor(
    override val metaRepository: MetaRepository,
    private val dashboardLocalRepository: DashboardLocalRepository,
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseFilterViewModel(dispatcherIO, metaRepository) {
    var userDashboardDetails = MutableLiveData<Resource<NCDUserDashboardResponse>>()
    val menuListLiveData = MutableLiveData<List<String>?>()
    val clinicalWorkflowNamesLowerLiveData = MutableLiveData<Set<String>>()
    private val filterLiveData = MutableLiveData(DashboardSearchFilter())

    fun getUserDashboardDetails(request: NCDUserDashboardRequest) {
        viewModelScope.launch(dispatcherIO) {
            userDashboardDetails.postLoading()
            setAnalyticsData(
                UserDetail.startDateTime,
                eventName = AnalyticsDefinedParams.NCDDashBoardCount,
                isCompleted = true,
            )
            // Use local dashboard counts from MemberAssessmentHistory instead of API
            val local = dashboardLocalRepository.getLocalDashboardDetails(request)
            userDashboardDetails.postValue(Resource(state = ResourceState.SUCCESS, data = local))
        }
    }

    fun getMenus() {
        viewModelScope.launch(dispatcherIO) {
            val response = metaRepository.getMenu()
            // Use menuId for reliable constant matching in dashboard card rendering
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
    ) {
        val filter = filterLiveData.value ?: DashboardSearchFilter()
        ssFilter?.let { filter.filterBySs = it }
        subVillagesFilter?.let { filter.filterBySubVillages = it }
        filterLiveData.value = filter
    }

    fun getFilterLiveData(): MutableLiveData<DashboardSearchFilter> = filterLiveData
}

data class DashboardSearchFilter(
    var filterBySs: List<ChipViewItemModel> = listOf(),
    var filterBySubVillages: List<ChipViewItemModel> = listOf(),
)
