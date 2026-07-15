package org.medtroniclabs.uhis.ui.membersearch.viewmodel

import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.common.DefinedParams.LIST_LIMIT
import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.offlinesync.model.SavedMemberDetails
import org.medtroniclabs.uhis.db.entity.SubVillageEntity
import org.medtroniclabs.uhis.db.entity.VillageEntity
import org.medtroniclabs.uhis.di.IoDispatcher
import org.medtroniclabs.uhis.model.household.HouseHoldFilterUiData
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.network.resource.Resource
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.network.utils.ConnectivityManager
import org.medtroniclabs.uhis.repo.HouseHoldRepository
import org.medtroniclabs.uhis.ui.BaseFilterViewModel
import org.medtroniclabs.uhis.ui.boarding.repo.MetaRepository
import org.medtroniclabs.uhis.ui.membersearch.datasource.MemberSearchDataSource
import org.medtroniclabs.uhis.ui.membersearch.model.MemberSearchListItem
import org.medtroniclabs.uhis.ui.membersearch.model.MemberSearchParams
import org.medtroniclabs.uhis.ui.membersearch.repo.MemberSearchRepository
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.measureTimedValue

/**
 * ViewModel for FO/PO member search.
 *
 * Coordinates debounced text search, FO/PO filters, and hybrid local + remote paging.
 */
@HiltViewModel
class MemberSearchViewModel @Inject constructor(
    private val memberSearchRepository: MemberSearchRepository,
    private val houseHoldRepository: HouseHoldRepository,
    override val metaRepository: MetaRepository,
    @param:IoDispatcher override var dispatcherIO: CoroutineDispatcher,
) : BaseFilterViewModel(dispatcherIO, metaRepository) {
    @Inject
    lateinit var connectivityManager: ConnectivityManager

    private val _searchParams = MutableStateFlow(MemberSearchParams())

    /** Current search and filter state. */
    val searchParams: StateFlow<MemberSearchParams> = _searchParams.asStateFlow()

    private val _filterUiData = MutableStateFlow<Resource<HouseHoldFilterUiData>>(Resource(ResourceState.SUCCESS))

    /** SK/SS filter sheet data. */
    val filterUiData: StateFlow<Resource<HouseHoldFilterUiData>> = _filterUiData.asStateFlow()

    private val _subVillages = MutableStateFlow<List<ChipViewItemModel>>(emptyList())

    /** Sub-villages for the currently selected SS chips (FO/PO) or Union (CHCP). */
    val subVillages: StateFlow<List<ChipViewItemModel>> = _subVillages.asStateFlow()

    private val _unions = MutableStateFlow<List<ChipViewItemModel>>(emptyList())

    /** Union (village) list for the CHCP filter sheet. */
    val unions: StateFlow<List<ChipViewItemModel>> = _unions.asStateFlow()

    private val _remoteMemberDetailsState = MutableStateFlow(Resource<SavedMemberDetails>(ResourceState.SUCCESS))

    /** Remote member fetch + persist state for navigation from search results. */
    val remoteMemberDetailsState: StateFlow<Resource<SavedMemberDetails>> = _remoteMemberDetailsState.asStateFlow()

    /**
     * Static filters enabled for the flow/user
     */
    private val staticFilters = mutableListOf<ServiceStaticFilter>()

    private val _staticFilterCounts = MutableStateFlow<Map<ServiceStaticFilter, Int>>(emptyMap())

    /**
     * Per-filter member counts for the member-type spinner.
     */
    val staticFilterCounts: StateFlow<Map<ServiceStaticFilter, Int>> = _staticFilterCounts.asStateFlow()

    /**
     * Hybrid local + remote member list; refreshes when [searchParams] changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    val membersFlow: Flow<PagingData<MemberSearchListItem>> =
        searchParams
            .flatMapLatest { params ->
                Pager(
                    config = PagingConfig(pageSize = LIST_LIMIT),
                    pagingSourceFactory = {
                        MemberSearchDataSource(
                            params = params.withOnlineState(),
                            memberSearchRepository = memberSearchRepository,
                        )
                    },
                ).flow
            }

    init {
        initializeAllowedDropdown()
        observeSearch { query ->
            updateFilter(search = query)
        }
        viewModelScope.launch(dispatcherIO) {
            searchParams.collectLatest { params ->
                refreshStaticFilterCounts(params)
            }
        }
    }

    /**
     * Populates [staticFilters] with FO/PO member-type options shown in the dropdown.
     */
    private fun initializeAllowedDropdown() {
        staticFilters.add(ServiceStaticFilter.ALL_MEMBERS)
        staticFilters.add(ServiceStaticFilter.NCD_SERVICES)
        staticFilters.add(ServiceStaticFilter.CATARACT_SCREENING)
        staticFilters.add(ServiceStaticFilter.EYE_SCREENING)
    }

    /**
     * Merges partial filter updates into [searchParams] and triggers a list refresh.
     */
    fun updateFilter(
        staticFilter: ServiceStaticFilter? = null,
        search: String? = null,
        ssFilter: List<ChipViewItemModel>? = null,
        subVillagesFilter: List<ChipViewItemModel>? = null,
        filterSk: Long? = null,
    ) {
        val current = _searchParams.value
        val newFilterSk = filterSk ?: current.filterSk
        val newSsFilter = ssFilter ?: current.filterBySs
        val skScopeSsIds = when {
            filterSk != null && filterSk == -1L -> emptyList()
            newFilterSk != -1L && newSsFilter.isEmpty() ->
                _filterUiData.value.data
                    ?.ssList
                    ?.map { it.id } ?: current.skScopeSsIds

            else -> current.skScopeSsIds
        }
        _searchParams.value = current.copy(
            staticFilter = staticFilter ?: current.staticFilter,
            searchInput = search ?: current.searchInput,
            qrCode = null,
            filterBySs = newSsFilter,
            filterBySubVillages = subVillagesFilter ?: current.filterBySubVillages,
            filterSk = newFilterSk,
            skScopeSsIds = skScopeSsIds,
        )
    }

    private suspend fun refreshStaticFilterCounts(params: MemberSearchParams) {
        if (staticFilters.isEmpty()) return
        val counts = measureTimedValue {
            memberSearchRepository.getStaticFilterCounts(
                filters = staticFilters.toList(),
                searchInput = params.searchInput.orEmpty(),
                filterBySs = params.effectiveSsIds,
                filterBySubVillages = params.effectiveSubVillageIds,
                allowNullHousehold = params.allowNullHousehold,
                qrCode = params.qrCode,
            )
        }
        Timber.tag("bug_n_bug").d("Time taken for count in seconds : " + counts.duration.inWholeSeconds)
        _staticFilterCounts.value = counts.value
    }

    private var lastLoadedFoPoKormiId: Long? = null

    /**
     * Loads SK list and restores previously applied SK filter if present.
     */
    fun getFilterUiData() {
        viewModelScope.launch(dispatcherIO) {
            lastLoadedFoPoKormiId = null
            _filterUiData.value = Resource(ResourceState.LOADING, _filterUiData.value.data)
            val initial = houseHoldRepository.getHouseHoldFilterUiDataForServiceRecipient()
            _filterUiData.value = initial
            if (initial.state != ResourceState.SUCCESS) return@launch

            val kormiId = _searchParams.value.filterSk
            if (kormiId == -1L) return@launch
            lastLoadedFoPoKormiId = kormiId
            loadSkFilterData(kormiId)
        }
    }

    /**
     * Loads SS list for the selected Shasthya Kormi.
     */
    fun loadSsListForSelectedKormi(kormiId: Long) {
        if (lastLoadedFoPoKormiId == kormiId) {
            val current = _filterUiData.value.data
            if (current?.ssList?.isNotEmpty() == true) return
        }
        lastLoadedFoPoKormiId = kormiId
        viewModelScope.launch(dispatcherIO) {
            _filterUiData.value = Resource(ResourceState.LOADING, _filterUiData.value.data)
            loadSkFilterData(kormiId)
        }
    }

    /**
     * Loads sub-villages for the selected SS chips.
     */
    fun onSsSelected(tags: List<ChipViewItemModel>) {
        viewModelScope.launch(dispatcherIO) {
            _subVillages.value =
                metaRepository
                    .getSubVillagesByShasthyaShebikaIds(tags.mapNotNull { it.id })
                    .map { ChipViewItemModel(id = it.id, name = it.name) }
        }
    }

    /**
     * CHCP filter: loads the Union (village) list scoped to the logged-in user.
     */
    fun loadChcpUnions() {
        viewModelScope.launch(dispatcherIO) {
            val response = houseHoldRepository.getUserVillages("").data?.response as? List<*>
            _unions.value =
                response
                    ?.filterIsInstance<VillageEntity>()
                    ?.map { ChipViewItemModel(id = it.id, name = it.name) }
                    .orEmpty()
        }
    }

    /**
     * CHCP filter: loads sub-villages for the selected Union.
     */
    fun onUnionSelected(villageId: Long) {
        viewModelScope.launch(dispatcherIO) {
            val response = houseHoldRepository.getSubVillagesByVillageId(villageId).data?.response as? List<*>
            _subVillages.value =
                response
                    ?.filterIsInstance<SubVillageEntity>()
                    ?.map { ChipViewItemModel(id = it.id, name = it.name) }
                    .orEmpty()
        }
    }

    /**
     * CHCP filter: clears sub-villages when the Union selection is removed.
     */
    fun clearChcpSubVillages() {
        _subVillages.value = emptyList()
    }

    private suspend fun loadSkFilterData(kormiId: Long) {
        val result = houseHoldRepository.getHouseHoldFilterUiDataForShasthyaKormi(kormiId)
        _filterUiData.value = result
        if (result.state == ResourceState.SUCCESS) {
            syncSkScopeSsIds(result.data)
        }
    }

    private fun syncSkScopeSsIds(data: HouseHoldFilterUiData?) {
        val ssIds = data?.ssList?.map { it.id } ?: emptyList()
        val current = _searchParams.value
        if (current.filterSk != -1L && current.skScopeSsIds != ssIds) {
            _searchParams.value = current.copy(skScopeSsIds = ssIds)
        }
    }

    private fun MemberSearchParams.withOnlineState(): MemberSearchParams = copy(isOnline = connectivityManager.isNetworkAvailable())

    fun fetchRemoteMemberDetails(patient: PatientListResModel) {
        val memberId = patient.memberReference ?: run {
            _remoteMemberDetailsState.value = Resource(ResourceState.ERROR)
            return
        }
        viewModelScope.launch(dispatcherIO) {
            _remoteMemberDetailsState.value = Resource(ResourceState.LOADING)
            val result = memberSearchRepository.fetchAndSaveRemoteMemberDetails(memberId)
            _remoteMemberDetailsState.value = if (result != null) {
                Resource(ResourceState.SUCCESS, result)
            } else {
                Resource(ResourceState.ERROR)
            }
        }
    }

    fun clearRemoteMemberDetailsState() {
        _remoteMemberDetailsState.value = Resource(ResourceState.SUCCESS)
    }

    fun filterMemberListByQr(qrCode: String) {
        _searchParams.value = MemberSearchParams(qrCode = qrCode)
    }
}
