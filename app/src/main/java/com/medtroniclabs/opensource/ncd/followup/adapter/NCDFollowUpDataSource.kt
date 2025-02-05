package com.medtroniclabs.opensource.ncd.followup.adapter

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.medtroniclabs.opensource.common.DefinedParams.LIST_LIMIT
import com.medtroniclabs.opensource.common.DefinedParams.PAGE_INDEX
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.ncd.data.CustomDate
import com.medtroniclabs.opensource.ncd.data.FollowUpRequest
import com.medtroniclabs.opensource.ncd.data.PatientFollowUpEntity
import com.medtroniclabs.opensource.ncd.data.SortModelForFollowUp
import com.medtroniclabs.opensource.ncd.followup.repo.NCDFollowUpRepo
import com.medtroniclabs.opensource.network.ApiHelper

class NCDFollowUpDataSource(
    private val ncdFollowUpRepo: NCDFollowUpRepo,
    private var apiHelper: ApiHelper,
    private var roomHelper: RoomHelper,
    private val searchText: String,
    private val sortModel: SortModelForFollowUp?,
    private val customDate: CustomDate?,
    private val dateRange: String?,
    private val type: String,
    private val remainingAttempts :List<Long>?,
    private val getPatientsCount: (Int) -> Unit
) : PagingSource<Int, PatientFollowUpEntity>() {

    private var loadedCount: Long = 0
    private var totalCount = 0
    private var isInitialData: Boolean = false
    override fun getRefreshKey(state: PagingState<Int, PatientFollowUpEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientFollowUpEntity> {
        val pageIndex = params.key ?: PAGE_INDEX
        return try {
            val request = FollowUpRequest(
                skip = loadedCount,
                limit = LIST_LIMIT,
                type = type,
                dateRange = dateRange?.takeIf { it.isNotBlank() },
                customDate = customDate,
                searchText = searchText.takeIf { it.isNotBlank() },
                remainingAttempts = remainingAttempts?.takeIf { it.isNotEmpty() && it.any { attempt -> attempt != null } },
                siteId = SecuredPreference.getString(SecuredPreference.EnvironmentKey.IS_DEFAULT_SITE_ID.name)
            )
            val response: APIResponse<List<PatientFollowUpEntity>> =
                apiHelper.ncdFollowUpList(request)
            val patientList = response.entityList.orEmpty()

            if (!isInitialData) {
                if (searchText.isEmpty()) {
                    totalCount = response.totalCount ?: 0
                    isInitialData = true
                } else {
                    totalCount += patientList.size
                }
                getPatientsCount(totalCount)
            }
            loadedCount += if (searchText.isEmpty()) LIST_LIMIT else patientList.size
            LoadResult.Page(
                data = patientList,
                prevKey = (pageIndex - 1).takeIf { pageIndex > PAGE_INDEX },
                nextKey = if (searchText.isEmpty()) {
                    (pageIndex + 1).takeIf { patientList.isNotEmpty() }
                } else (pageIndex + 1).takeIf { patientList.isNotEmpty() }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}