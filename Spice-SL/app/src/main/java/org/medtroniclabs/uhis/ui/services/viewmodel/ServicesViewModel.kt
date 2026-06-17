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
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.model.services.ServicesSearchFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.ui.BaseFilterViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.measureTimedValue

@HiltViewModel
class ServicesViewModel @Inject constructor(
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
    private val memberRepository: HouseholdMemberRepository,
    private val houseHoldRepository: HouseHoldRepository,
    override val metaRepository: MetaRepository,
) : BaseFilterViewModel(dispatcherIO, metaRepository) {
    /**
     * UI payload for Services list screen.
     *
     * Keeps the currently visible member list and all static-filter counters together,
     * derived from the same dynamic filter state.
     */
    data class FilteredMembersUiData(
        val members: List<HouseholdMemberWithTb>,
        val counts: Map<ServiceStaticFilter, Int>,
    )

    var filterUiData = MutableLiveData<Resource<HouseHoldFilterUiData>>()

    /**
     * Live data holding filter data
     */
    private val filterLiveData = MutableLiveData<ServicesSearchFilter>()

    /**
     * Boolean holding whether user role is FO/PO
     */
    var isFoPo = CommonUtils.isFoOrPo()
        private set

    /**
     * Static filters enabled for the flow/user
     */
    private val staticFilters = mutableListOf<ServiceStaticFilter>()

    /**
     * Single stream for filtered list + counts.
     *
     * Recomputes whenever [filterLiveData] changes so chips/tabs and list remain consistent.
     */
    val filteredMembersLiveData: LiveData<Resource<FilteredMembersUiData>> =
        filterLiveData.switchMap { filter ->
            liveData(dispatcherIO) {
                var ssFilter = filter.filterBySs.map { it.id!! }
                if (isFoPo && filter.filterSk != -1L && ssFilter.isEmpty()) {
                    val uiSsData = filterUiData.value
                        ?.data
                        ?.ssList
                        ?.map { it.id }
                    ssFilter = uiSsData ?: emptyList()
                }
                emit(
                    Resource<FilteredMembersUiData>(
                        state = ResourceState.LOADING,
                    ),
                )
                val restrictExternalToSkCreator = CommonUtils.isSk()
                val counts = measureTimedValue {
                    memberRepository.getServiceMemberCounts(
                        filters = staticFilters.toList(),
                        searchInput = filter.searchInput,
                        filterBySs = ssFilter,
                        filterBySubVillages = filter.filterBySubVillages.map { it.id!! },
                        allowNullHousehold = isFoPo,
                        qrCode = filter.qrCode,
                        restrictExternalToSkCreator = restrictExternalToSkCreator,
                    )
                }
                Timber.tag("bug_n_bug").d("Time taken for count in seconds : " + counts.duration.inWholeSeconds)
                val members = measureTimedValue {
                    memberRepository
                        .getServiceMembers(
                            filter.searchInput,
                            ssFilter,
                            filter.filterBySubVillages.map { it.id!! },
                            filter.staticFilter,
                            allowNullHousehold = isFoPo,
                            qrCode = filter.qrCode,
                            restrictExternalToSkCreator = restrictExternalToSkCreator,
                        )
                }
                Timber.tag("bug_n_bug").d("Time taken for filtered data in seconds : " + members.duration.inWholeSeconds)
                emitSource(
                    members.value.map { memberList ->
                        Resource(
                            state = ResourceState.SUCCESS,
                            FilteredMembersUiData(
                                members = memberList,
                                counts = counts.value,
                            ),
                        )
                    },
                )
            }
        }

    init {
        observeSearch {
            setFilterLiveData(search = it)
        }
    }

    /**
     * Populates [staticFilters] with the member-type options shown in the services dropdown.
     *
     * - FO/PO: all members, NCD, cataract, and eye screening
     * - SK: all members, NCD and eye screening, plus RMNCH cohort filters
     * - Other roles: all members plus RMNCH and related cohort filters
     *
     * When [isExternalMember] is true, no filters are added because the screen is scoped
     * to external members only and the dropdown is hidden.
     */
    fun initializeAllowedDropdown(isExternalMember: Boolean = false) {
        if (!isExternalMember) {
            staticFilters.add(ServiceStaticFilter.ALL_MEMBERS)
            if (!isFoPo) {
                staticFilters.add(ServiceStaticFilter.EXTERNAL_MEMBERS)
                staticFilters.add(ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS)
                staticFilters.add(ServiceStaticFilter.PREGNANT_WOMEN)
                staticFilters.add(ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN)
                staticFilters.add(ServiceStaticFilter.HIGH_RISK_PREGNANT_WOMEN)
                staticFilters.add(ServiceStaticFilter.FAMILY_PLANNING_COUNSELLING_ELIGIBLE)
                staticFilters.add(ServiceStaticFilter.POSTNATAL_CARE_MOTHERS)
                staticFilters.add(ServiceStaticFilter.EXPECTED_DELIVERIES)
                staticFilters.add(ServiceStaticFilter.PENDING_DELIVERIES)
            }
            if (isFoPo || CommonUtils.isSk()) {
                addScreeningServiceFilters(includeCataract = isFoPo)
            }
        }
    }

    private fun addScreeningServiceFilters(includeCataract: Boolean) {
        staticFilters.add(ServiceStaticFilter.NCD_SERVICES)
        if (includeCataract) {
            staticFilters.add(ServiceStaticFilter.CATARACT_SCREENING)
        }
        staticFilters.add(ServiceStaticFilter.EYE_SCREENING)
    }

    /**
     * Applies the initial filter once so [filteredMembersLiveData] is not recomputed twice
     * (e.g. default [ServiceStaticFilter.ALL_MEMBERS] then external-members override).
     */
    fun initializeFilter(
        isExternalMember: Boolean = false,
        ssFilter: List<ChipViewItemModel> = emptyList(),
        subVillagesFilter: List<ChipViewItemModel> = emptyList(),
        staticFilter: ServiceStaticFilter? = null,
    ) {
        val initialStaticFilter = when {
            isExternalMember -> ServiceStaticFilter.EXTERNAL_MEMBERS
            staticFilter != null -> staticFilter
            else -> ServiceStaticFilter.ALL_MEMBERS
        }
        filterLiveData.value = ServicesSearchFilter(
            filterBySs = ssFilter,
            filterBySubVillages = subVillagesFilter,
            staticFilter = initialStaticFilter,
        )
    }

    fun setFilterLiveData(
        search: String? = null,
        ssFilter: List<ChipViewItemModel>? = null,
        subVillagesFilter: List<ChipViewItemModel>? = null,
        staticFilter: ServiceStaticFilter? = null,
        filterSk: Long? = null,
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
        filterSk?.let {
            filter.filterSk = filterSk
        }

        filter.qrCode = null

        filterLiveData.postValue(filter)
    }

    fun getFilterLiveData(): LiveData<ServicesSearchFilter> = filterLiveData

    private var lastLoadedFoPoKormiId: Long? = null

    fun getFilterUiData() {
        viewModelScope.launch(dispatcherIO) {
            lastLoadedFoPoKormiId = null
            filterUiData.postLoading()
            val initial = houseHoldRepository.getHouseHoldFilterUiDataForServiceRecipient()
            filterUiData.postValue(initial)
            if (initial.state != ResourceState.SUCCESS) return@launch

            val kormiId = filterLiveData.value?.filterSk ?: return@launch
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

    fun getStaticFilters() = staticFilters

    fun filterMemberListByQr(qrCodeString: String) {
        val filter = ServicesSearchFilter().apply {
            qrCode = qrCodeString
        }
        filterLiveData.postValue(filter)
    }
}
