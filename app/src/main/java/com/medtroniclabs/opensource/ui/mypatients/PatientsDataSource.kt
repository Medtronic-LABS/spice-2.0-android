package com.medtroniclabs.opensource.ui.mypatients

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.medtroniclabs.opensource.common.DefinedParams.LIST_LIMIT
import com.medtroniclabs.opensource.common.DefinedParams.PAGE_INDEX
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.APIResponse
import com.medtroniclabs.opensource.model.MedicalReviewFilterModel
import com.medtroniclabs.opensource.model.PatientListRespModel
import com.medtroniclabs.opensource.model.PatientsDataModel
import com.medtroniclabs.opensource.model.SearchAndListResponse
import com.medtroniclabs.opensource.model.SortModel
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.ui.mypatients.repo.PatientRepository


class PatientsDataSource(
    private val apiHelper: ApiHelper,
    private val patientRepository: PatientRepository,
    private val searchText: String,
    private val filter:MedicalReviewFilterModel?,
    private val sort: SortModel?,
    private val isPatientListRequired: Boolean,
    private val origin: String?,
    private val getPatientsCount: (String) -> Unit
) : PagingSource<Int, PatientListRespModel>() {

    private var loadedCount: Long = 0
    private var totalCount = 0
    private var villages: List<Long>? = null
    private var districtId: Long? = null
    private var referencePatientId: String? = null
    private var isInitialData: Boolean = false
    override fun getRefreshKey(state: PagingState<Int, PatientListRespModel>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientListRespModel> {
        val pageIndex = params.key ?: PAGE_INDEX
        return try {
            if (villages.isNullOrEmpty()) {
                val userVillages = patientRepository.getUserVillages()
                if (userVillages.isNotEmpty())
                    villages = userVillages.map { it.id }
            }

            districtId = districtId ?: SecuredPreference.getDistrictId()

            val patientsDataModel = PatientsDataModel(
                skip = loadedCount,
                limit = LIST_LIMIT,
                villageIds = villages,
                referencePatientId = referencePatientId?.ifBlank { null },
                filter = filter,
                sort = sort,
                type = origin,
                siteId = SecuredPreference.getOrganizationFhirId(),
                countryId = SecuredPreference.getCountryId(),
                tenantId = SecuredPreference.getTenantId()
            )

            val response: APIResponse<SearchAndListResponse>? = if (isPatientListRequired && searchText.isBlank()) {
                apiHelper.getPatients(patientsDataModel)
            } else if(searchText.isNotBlank()) {
                apiHelper.patientSearch(
                    patientsDataModel.copy(
                        villageIds = null,
                        searchText = searchText.ifEmpty { null },
                        districtId = districtId
                    )
                )
            } else
                null

            /* Request construction - Ends */


            val patientList: List<PatientListRespModel> = response?.entity?.patientList ?: emptyList()
            referencePatientId = response?.entity?.referencePatientId
            if (!isInitialData) {
                if (searchText.isEmpty()) {
                    totalCount = response?.entity?.totalCount ?: 0
                    isInitialData = true
                } else {
                    totalCount += patientList.size
                }
                getPatientsCount(totalCount.toString())
            }
//            For Patient List Skip increment as 15
//            For Patient search Skip increment as patient list Size
            loadedCount += if (searchText.isEmpty()) LIST_LIMIT else patientList.size
            LoadResult.Page(
                data = patientList,
                prevKey = (pageIndex - 1).takeIf { pageIndex > PAGE_INDEX },
                nextKey = if (searchText.isEmpty()) {
                    (pageIndex + 1).takeIf { patientList.isNotEmpty()}
                } else (pageIndex + 1).takeIf { patientList.isNotEmpty() }
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}