package org.medtroniclabs.uhis.ui.patient

import androidx.paging.PagingSource
import androidx.paging.PagingState
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.FilterEnum
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.APIResponse
import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.model.PatientDataModel
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.network.ApiHelper

private const val PAGE_INDEX = 0
const val LIST_LIMIT = 15

class PatientsDataSource(
    private val isSiteBasedSearch: Boolean,
    private val searchModel: PatientDataModel,
    private val apiHelper: ApiHelper,
    private val getPatientsCount: GetPatientsCount,
    private val origin: String,
    private val isPsychologist: Boolean?,
    private val followUpType: String? = null,
    isFilteredUnion: Boolean,
) : PagingSource<Int, PatientListResModel>() {
    private var loadedCount = 0
    private var totalCount = 0

    override fun getRefreshKey(state: PagingState<Int, PatientListResModel>): Int? =
        state.anchorPosition?.let { anchorPosition ->
            state.closestPageToPosition(anchorPosition)?.prevKey?.plus(1)
                ?: state.closestPageToPosition(anchorPosition)?.nextKey?.minus(1)
        }

    private fun getTenantId(): Long? = SecuredPreference.getTenantId()

    private fun setQRCode(): String? = null

    private fun setQRValue(): String? = searchModel.searchQRValue

    private fun defaultRequest(): PatientDataModel {
        val filteredUnion = searchModel.patientFilter?.filterUnionId ?: 0

        return PatientDataModel(
            skip = loadedCount,
            limit = LIST_LIMIT,
            tenantId = getTenantId(),
            operatingUnitId = searchModel.operatingUnitId,
            accountId = searchModel.accountId,
            isLabtestReferred = searchModel.isLabtestReferred,
            isMedicationPrescribed = searchModel.isMedicationPrescribed,
            patientSort = searchModel.patientSort,
            patientFilter = searchModel.patientFilter,
            searchQRValue = setQRValue(),
            qrCode = setQRCode(),
            counsellorId = searchModel.counsellorId,
            isParaCounsellingDisabled = searchModel.isParaCounsellingDisabled,
            unionId = if (filteredUnion > 0) listOf(filteredUnion) else searchModel.unionId,
            userId = searchModel.userId,
            isPsychologist = isPsychologist,
            prescribedSiteId = searchModel.prescribedSiteId,
            followUpType = followUpType,
            callStatus = searchModel.patientFilter?.callStatus,
            patientType = searchModel.patientFilter?.patientType,
        )
    }

    private fun isTelesupportMenuSupported(): Boolean =
        CommonUtils.isHealthEducator() ||
            CommonUtils.isParamedic() ||
            CommonUtils.isMedicalDoctor() ||
            CommonUtils.isNurse() ||
            CommonUtils.isProvider() ||
            CommonUtils.isHealthScreener() ||
            CommonUtils.isPhysicianPrescriber() ||
            CommonUtils.isCHCP()

    private fun searchStatusForOrigin(patientStatusFromFilter: String?): String? =
        when {
            origin.equals(UIConstants.ENROLLMENT_UNIQUE_ID, ignoreCase = true) ->
                patientStatusFromFilter ?: FilterEnum.NOT_ENROLLED.name
            isDispenseOrigin() ->
                patientStatusFromFilter ?: FilterEnum.ENROLLED.name
            else -> null
        }

    private fun clearsPatientStatusFromFilter(): Boolean = origin.equals(UIConstants.ENROLLMENT_UNIQUE_ID, ignoreCase = true) || isDispenseOrigin()

    /** Dispense search (text/QR) must flag medicine dispense; scoped to origin, not role. */
    private fun isDispenseOrigin(): Boolean = origin.equals(UIConstants.DISPENSE, ignoreCase = true)

    private fun medicineDispenseForSearch(): Boolean? = if (isDispenseOrigin()) true else null

    private fun FilterModel?.withoutDefaultPcFilterOnly(): FilterModel? {
        if (this == null) return null
        return if (copy(isDefaultPcFilter = false) == FilterModel(isDefaultPcFilter = false)) {
            null
        } else {
            this
        }
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, PatientListResModel> {
        val pageIndex = params.key ?: PAGE_INDEX
        return try {
            val response: APIResponse<ArrayList<PatientListResModel>>
            /*
             * Please make sure you are confident before making any changes
             * on the following request construction.
             * Request construction - Starts
             */
            val isQrSearch = !searchModel.searchQRValue.isNullOrBlank()
            val isFollowUp = origin == UIConstants.FOLLOW_UP
            val isExactSearch = !searchModel.searchText.isNullOrBlank()

            response = when {
                isQrSearch -> {
                    val request = defaultRequest().copy(
                        isSearchUserOrgPatient = isSiteBasedSearch,
                        patientSort = null,
                        patientFilter = null,
                        status = searchStatusForOrigin(searchModel.patientFilter?.patientStatus),
                        medicineDispense = medicineDispenseForSearch(),
                    )
                    when {
                        isFollowUp -> apiHelper.patientFollowUpList(request)
                        else -> apiHelper.searchPatientById(request)
                    }
                }

                isFollowUp -> {
                    val isTeleSupport = isTelesupportMenuSupported()
                    val modifiedPatientFilter = if (isTeleSupport) {
                        FilterModel(
                            countyId = searchModel.patientFilter?.countyId,
                            subCountyId = searchModel.patientFilter?.subCountyId,
                            accountIds = searchModel.patientFilter?.accountIds ?: searchModel.patientFilter?.allAccountIds,
                            diagnosisType = searchModel.patientFilter?.diagnosisType,
                            patientType = searchModel.patientFilter?.patientType,
                            cvdRisk = searchModel.patientFilter?.cvdRisk,
                            riskStatus = searchModel.patientFilter?.riskStatus,
                        )
                    } else {
                        null
                    }

                    val payLoad = defaultRequest().copy(
                        searchText = searchModel.searchText,
                        firstName = searchModel.firstName,
                        lastName = searchModel.lastName,
                        phoneNumber = searchModel.phoneNumber,
                        isSearchUserOrgPatient = isSiteBasedSearch,
                        patientFilter = modifiedPatientFilter,
                        diagnosis = searchModel.patientFilter?.diagnosis,
                        dateRange = searchModel.patientFilter?.dateRange,
                        customDate = searchModel.patientFilter?.customDate,
                        villageId = searchModel.patientFilter?.subVillageId,
                        referredSite = searchModel.patientFilter?.referredSite,
                        remainingAttempts = searchModel.patientFilter?.remainingAttempts,
                    )
                    when (followUpType) {
                        // DefinedParams.MEDICAL_REVIEW_FOLLOW_UP -> apiHelper.medicalReviewFollowUpList(payLoad)
                        // DefinedParams.ASSESSMENT_FOLLOW_UP -> apiHelper.assessmentFollowUpList(payLoad)
                        else -> apiHelper.patientFollowUpList(payLoad)
                    }
                }

                isExactSearch -> {
                    val request = defaultRequest()

                    val exactSearch = request.copy(
                        searchText = searchModel.searchText,
                        isSearchUserOrgPatient = isSiteBasedSearch,
                        status = searchStatusForOrigin(request.patientFilter?.patientStatus),
                        medicineDispense = medicineDispenseForSearch(),
                        patientFilter = if (clearsPatientStatusFromFilter()) {
                            null
                        } else {
                            request.patientFilter
                                ?.copy(patientStatus = null)
                                .withoutDefaultPcFilterOnly()
                        },
                    )

                    apiHelper.searchPatientById(exactSearch)
                }

                else -> {
                    val request = defaultRequest()
                    request.patientFilter = request.patientFilter?.copy(patientStatus = null)
                    apiHelper.patientsList(request)
                }
            }
            // Request construction - Ends
            val patientList: ArrayList<PatientListResModel> = response.entityList ?: ArrayList()
            response.totalCount?.let { count ->
                totalCount = count
            }
            if (loadedCount == 0) {
                getPatientsCount.patientsCount(totalCount.toString())
            }
            loadedCount += patientList.size
            LoadResult.Page(
                data = patientList,
                prevKey = if (pageIndex > PAGE_INDEX) pageIndex - 1 else null,
                nextKey = if (loadedCount < totalCount) pageIndex + 1 else null,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }
}
