package org.medtroniclabs.uhis.ui.services.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.appextensions.postLoading
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.model.services.ServiceMemberCounts
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.model.services.ServicesSearchFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.ui.BaseViewModel
import javax.inject.Inject

@HiltViewModel
class ServicesViewModel @Inject constructor(
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val memberRepository: HouseholdMemberRepository,
    private val houseHoldRepository: HouseHoldRepository,
) : BaseViewModel(dispatcherIO) {
    /**
     * UI payload for Services list screen.
     *
     * Keeps the currently visible member list and all static-filter counters together,
     * derived from the same dynamic filter state.
     */
    data class FilteredMembersUiData(
        val members: List<HouseholdMemberWithTb>,
        val counts: ServiceMemberCounts,
    )

    var filterUiData = MutableLiveData<Resource<HouseHoldFilterUiData>>()

    private val filterLiveData = MutableLiveData<ServicesSearchFilter>()

    /**
     * Single stream for filtered list + counts.
     *
     * Recomputes whenever [filterLiveData] changes so chips/tabs and list remain consistent.
     */
    val filteredMembersLiveData: LiveData<Resource<FilteredMembersUiData>> =
        filterLiveData.switchMap { filter ->
            liveData(dispatcherIO) {
                emit(
                    Resource<FilteredMembersUiData>(
                        state = ResourceState.LOADING,
                    ),
                )
                val allowNullHousehold = CommonUtils.isFoOrPo()
                val counts =
                    memberRepository.getAllServiceMemberCounts(
                        searchInput = filter.searchInput,
                        filterBySs = filter.filterBySs.map { it.id!! },
                        filterBySubVillages = filter.filterBySubVillages.map { it.id!! },
                        allowNullHousehold = allowNullHousehold,
                    )
                emitSource(
                    memberRepository
                        .getServiceMembers(
                            filter.searchInput,
                            filter.filterBySs.map { it.id!! },
                            filter.filterBySubVillages.map { it.id!! },
                            filter.staticFilter,
                            allowNullHousehold = allowNullHousehold,
                        ).map { members ->
                            Resource(
                                state = ResourceState.SUCCESS,
                                FilteredMembersUiData(
                                    members = members,
                                    counts = counts,
                                ),
                            )
                        },
                )
            }
        }

    init {
        filterLiveData.value = ServicesSearchFilter()
    }

    fun setFilterLiveData(
        search: String? = null,
        ssFilter: List<ChipViewItemModel>? = null,
        subVillagesFilter: List<ChipViewItemModel>? = null,
        staticFilter: ServiceStaticFilter? = null,
    ) {
        val filter = filterLiveData.value ?: ServicesSearchFilter()
        search?.let {
            filter.searchInput = search
        }
        ssFilter?.let {
            filter.filterBySs = ssFilter
        }
        subVillagesFilter?.let {
            filter.filterBySubVillages = subVillagesFilter
        }
        staticFilter?.let {
            filter.staticFilter = staticFilter
        }
        filterLiveData.postValue(filter)
    }

    fun getFilterLiveData(): LiveData<ServicesSearchFilter> = filterLiveData

    private var lastLoadedFoPoKormiId: Long? = null

    fun getFilterUiData() {
        viewModelScope.launch(dispatcherIO) {
            lastLoadedFoPoKormiId = null
            filterUiData.postLoading()
            val userId = SecuredPreference.getUserId()
            val initial = houseHoldRepository.getHouseHoldFilterUiDataForServiceRecipient(userId)
            filterUiData.postValue(initial)
            if (CommonUtils.isFoOrPo() &&
                initial.state == ResourceState.SUCCESS &&
                initial.data != null
            ) {
                val firstSsId = filterLiveData.value
                    ?.filterBySs
                    ?.firstOrNull()
                    ?.id
                if (firstSsId != null) {
                    val kormiId =
                        houseHoldRepository.getShasthyaKormiIdForShasthyaShebika(firstSsId)
                    if (kormiId != null) {
                        lastLoadedFoPoKormiId = kormiId
                        filterUiData.postValue(
                            houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(kormiId),
                        )
                    }
                }
            }
        }
    }

    fun loadFilterUiDataForSelectedKormi(kormiId: Long) {
        if (lastLoadedFoPoKormiId == kormiId) {
            val current = filterUiData.value?.data
            if (current?.ssList?.isNotEmpty() == true) return
        }
        lastLoadedFoPoKormiId = kormiId
        viewModelScope.launch(dispatcherIO) {
            setFilterLiveData(ssFilter = emptyList(), subVillagesFilter = emptyList())
            filterUiData.postLoading()
            filterUiData.postValue(
                houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(kormiId),
            )
        }
    }
}
