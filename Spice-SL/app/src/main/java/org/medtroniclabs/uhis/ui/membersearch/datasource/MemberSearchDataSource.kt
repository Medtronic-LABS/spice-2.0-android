package org.medtroniclabs.uhis.ui.membersearch.datasource

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.ui.membersearch.model.MemberSearchListItem
import org.medtroniclabs.uhis.ui.membersearch.model.MemberSearchParams
import org.medtroniclabs.uhis.ui.membersearch.repo.MemberSearchRepository
import org.medtroniclabs.uhis.ui.membersearch.repo.RemotePatientSearchResult

private const val PAGE_INDEX = 0

/**
 * Hybrid paging source: page 0 loads local members and the first remote page when online;
 * subsequent pages load additional remote results when a search term is present.
 */
class MemberSearchDataSource(
    private val params: MemberSearchParams,
    private val memberSearchRepository: MemberSearchRepository,
) : PagingSource<Int, MemberSearchListItem>() {
    private var loadedCount = 0
    private var totalCount = 0
    private var localFhirIds: Set<String> = emptySet()

    override fun getRefreshKey(state: PagingState<Int, MemberSearchListItem>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, MemberSearchListItem> {
        val pageIndex = params.key ?: PAGE_INDEX
        return try {
            if (pageIndex == LOCAL_PAGE_KEY) {
                loadLocalPage()
            } else {
                loadRemotePage(pageIndex)
            }
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun loadLocalPage(): LoadResult<Int, MemberSearchListItem> {
        val localMembers = memberSearchRepository.getLocalMembers(
            searchInput = params.searchInput,
            filterBySs = params.effectiveSsIds,
            filterBySubVillages = params.effectiveSubVillageIds,
            allowNullHousehold = params.allowNullHousehold,
        )
        localFhirIds = memberSearchRepository.extractLocalFhirIds(localMembers)
        val localItems = localMembers.map { MemberSearchListItem.Local(it) }

        if (!shouldFetchRemote()) {
            return LoadResult.Page(
                data = localItems,
                prevKey = null,
                nextKey = null,
            )
        }

        val remoteItems = fetchFirstRemotePage()
        val combinedData = if (remoteItems != null) localItems + remoteItems else localItems
        val nextKey = if (remoteItems != null && loadedCount < totalCount) REMOTE_PAGE_START else null

        return LoadResult.Page(
            data = combinedData,
            prevKey = null,
            nextKey = nextKey,
        )
    }

    private suspend fun fetchFirstRemotePage(): List<MemberSearchListItem.Remote>? =
        try {
            val result = memberSearchRepository.searchRemotePatients(
                searchInput = params.searchInput,
                skip = 0,
                isSiteBasedSearch = params.isSiteBasedSearch,
            )
            totalCount = result.totalCount
            loadedCount = result.patients.size
            mapRemoteResults(result)
        } catch (_: Exception) {
            loadedCount = 0
            totalCount = 0
            null
        }

    private suspend fun loadRemotePage(pageIndex: Int): LoadResult<Int, MemberSearchListItem> {
        if (!shouldFetchRemote()) {
            return LoadResult.Page(
                data = emptyList(),
                prevKey = pageIndex - 1,
                nextKey = null,
            )
        }

        val result = memberSearchRepository.searchRemotePatients(
            searchInput = params.searchInput,
            skip = loadedCount,
            isSiteBasedSearch = params.isSiteBasedSearch,
        )
        totalCount = result.totalCount
        val remoteItems = mapRemoteResults(result)
        loadedCount += result.patients.size

        return LoadResult.Page(
            data = remoteItems,
            prevKey = if (pageIndex > REMOTE_PAGE_START) pageIndex - 1 else LOCAL_PAGE_KEY,
            nextKey = if (loadedCount < totalCount) pageIndex + 1 else null,
        )
    }

    private fun mapRemoteResults(result: RemotePatientSearchResult): List<MemberSearchListItem.Remote> =
        result.patients
            .filterNot { patient -> isDuplicate(patient) }
            .map { MemberSearchListItem.Remote(it) }

    private fun shouldFetchRemote(): Boolean = params.searchInput.isNotBlank() && params.isOnline

    private fun isDuplicate(patient: PatientListResModel): Boolean = localFhirIds.contains(patient.id?.toString())

    private companion object {
        const val LOCAL_PAGE_KEY = 0
        const val REMOTE_PAGE_START = 1
    }
}
