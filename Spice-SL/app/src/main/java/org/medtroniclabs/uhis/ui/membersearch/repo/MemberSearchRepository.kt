package org.medtroniclabs.uhis.ui.membersearch.repo

import androidx.lifecycle.asFlow
import kotlinx.coroutines.flow.first
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.data.model.PatientDataModel
import org.medtroniclabs.uhis.data.model.PatientListResModel
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.data.offlinesync.model.SavedMemberDetails
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.repo.HouseholdMemberRepository
import org.medtroniclabs.uhis.repo.OfflineSyncRepository
import org.medtroniclabs.uhis.ui.patient.LIST_LIMIT
import javax.inject.Inject

/**
 * Data access for member search: local Room members and remote patient search.
 */
class MemberSearchRepository @Inject constructor(
    private val householdMemberRepository: HouseholdMemberRepository,
    private val apiHelper: ApiHelper,
    private val offlineSyncRepository: OfflineSyncRepository,
) {
    /**
     * Loads local members matching the given search and filter criteria.
     * Waits for Room to emit the first query result.
     */
    suspend fun getLocalMembers(
        searchInput: String?,
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        allowNullHousehold: Boolean = false,
        qrCode: String? = null,
    ): List<HouseholdMemberWithTb> =
        householdMemberRepository
            .getServiceMembers(
                searchInput = searchInput,
                filterBySs = filterBySs,
                filterBySubVillages = filterBySubVillages,
                staticFilter = ServiceStaticFilter.ALL_MEMBERS,
                allowNullHousehold = allowNullHousehold,
                qrCode = qrCode,
            ).asFlow()
            .first()

    /**
     * Searches patients on the remote API for the given text query.
     */
    suspend fun searchRemotePatients(
        searchInput: String?,
        skip: Int,
        isSiteBasedSearch: Boolean,
        qrValue: String?,
    ): RemotePatientSearchResult {
        val request =
            PatientDataModel(
                skip = skip,
                limit = LIST_LIMIT,
                tenantId = SecuredPreference.getTenantId(),
                searchText = searchInput,
                isSearchUserOrgPatient = isSiteBasedSearch,
                searchQRValue = qrValue,
            )
        val response = apiHelper.searchPatientById(request)
        return RemotePatientSearchResult(
            patients = response.entityList ?: arrayListOf(),
            totalCount = response.totalCount ?: 0,
        )
    }

    /** Builds the set of FHIR/patient ids from local results for remote deduplication. */
    fun extractLocalFhirIds(members: List<HouseholdMemberWithTb>): Set<String> =
        members
            .flatMap { member ->
                listOfNotNull(
                    member.fhirId?.takeIf { it.isNotBlank() },
                    member.patientId?.takeIf { it.isNotBlank() },
                )
            }.toSet()

    suspend fun fetchAndSaveRemoteMemberDetails(memberId: String): SavedMemberDetails? = offlineSyncRepository.fetchAndSaveMemberDetails(memberId)
}

/** Paginated remote patient search response. */
data class RemotePatientSearchResult(
    val patients: List<PatientListResModel>,
    val totalCount: Int,
)
