package org.medtroniclabs.uhis.db.dao

import android.database.Cursor
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.data.offlinesync.model.HHSignatureDetail
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHoldMember
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberStatus
import org.medtroniclabs.uhis.data.offlinesync.model.HouseholdMemberWithTb
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.entity.MemberServiceIconRow
import org.medtroniclabs.uhis.model.MemberDobGenderModel
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

@Dao
interface MemberDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(memberEntity: HouseholdMemberEntity): Long

    /**
     * Retrieves all national IDs that are not null and match the specified ID type.
     *
     * @param idType The type of ID to filter by (e.g., National ID).
     * @return A list of matching national IDs.
     */
    @Query("SELECT national_id FROM HouseHoldMember WHERE national_id IS NOT NULL AND id_type = :idType")
    suspend fun getAllNationalIds(idType: String): List<String>

    @Query("SELECT * FROM HouseHoldMember WHERE household_id = :houseHoldId")
    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): List<HouseholdMemberEntity>

    @Query(
        """
        SELECT
            hhm.*,
            td.diagnoses,
            (SELECT MAX(strftime('%s', visitDate) * 1000)
             FROM MemberAssessmentHistory mah
             WHERE mah.memberId = hhm.id) AS recent_service_date
        FROM householdmember AS hhm
        LEFT JOIN TreatmentDetailsEntity AS td ON hhm.fhir_id = td.memberId
        WHERE hhm.household_id = :houseHoldId
        """,
    )
    fun getAllHouseHoldMembersLiveDataRaw(houseHoldId: Long): LiveData<List<HouseholdMemberWithTb>>

    fun getAllHouseHoldMembersLiveData(houseHoldId: Long): LiveData<List<HouseholdMemberWithTb>> = attachAssessmentHistoryToMembers(getAllHouseHoldMembersLiveDataRaw(houseHoldId))

    @Query("SELECT * FROM HouseHoldMember WHERE household_id = :houseHoldId AND isActive =:aliveStatus")
    fun getAliveHouseHoldMembers(
        houseHoldId: Long,
        aliveStatus: Boolean,
    ): List<HouseholdMemberEntity>

    @Query("SELECT * FROM HouseHoldMember WHERE id = :memberId")
    suspend fun getMemberDetailsById(memberId: Long): HouseholdMemberEntity

    @Query("SELECT * FROM HouseHoldMember WHERE patient_id = :patientId")
    suspend fun getMemberDetailsByPatientId(patientId: String): HouseholdMemberEntity?

    @Query("SELECT * FROM HouseHoldMember WHERE motherReferenceId = :memberId ORDER BY fhir_id IS NULL, fhir_id ASC")
    suspend fun getMemberDetailsByParentId(memberId: String): List<HouseholdMemberEntity>

    @Query("SELECT id, fhir_id AS fhirId, '' AS signatureName FROM HouseHoldMember WHERE fhir_id IS NOT NULL")
    suspend fun getHHSignatureDetails(): List<HHSignatureDetail>

    @Query("SELECT COUNT(household_id) FROM HouseHoldMember WHERE household_id = :householdId")
    suspend fun getMemberCountPerHouseHold(householdId: Long): Int

    @Query("SELECT patient_id FROM HouseholdMember WHERE patient_id LIKE :patientIdStarts ORDER BY patient_id DESC LIMIT 1")
    suspend fun getLastPatientId(patientIdStarts: String): String?

    @Query("SELECT date_of_birth,gender FROM HouseHoldMember WHERE id = :memberId")
    suspend fun getDobAndGenderById(memberId: Long): MemberDobGenderModel

    @Query("SELECT hhm.*, hh.fhir_id as household_fhir_id, hh.village_id as village_id, ve.name as village_name, hh.sub_village_id as sub_village_id, sv.name as sub_village_name, CASE WHEN lhhm.memberId IS NOT NULL AND lhhm.syncStatus IN (:status) THEN 1 ELSE NULL END AS assignHousehold FROM HouseHoldMember AS hhm INNER JOIN Household as hh ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id LEFT JOIN SubVillageEntity AS sv ON hh.sub_village_id = sv.id LEFT JOIN LinkHouseholdMember AS lhhm ON lhhm.memberId = hhm.fhir_id WHERE hhm.id NOT IN (:memberIds) AND (hh.fhir_id IS NULL OR hhm.fhir_id IS NULL) AND hhm.household_id = :houseHoldId AND hhm.sync_status IN (:status)")
    suspend fun getAllUnSyncedHouseHoldMembers(
        houseHoldId: Long,
        memberIds: List<Long>,
        status: List<String> = listOf(
            OfflineSyncStatus.NotSynced.name,
            OfflineSyncStatus.NetworkError.name,
        ),
    ): List<HouseHoldMember>

    @Query("SELECT hhm.*, hh.fhir_id as household_fhir_id, COALESCE(hh.village_id, hhm.villageId) as village_id, ve.name as village_name, COALESCE(hh.sub_village_id, hhm.sub_village_id) as sub_village_id, sv.name as sub_village_name, CASE WHEN lhhm.memberId IS NOT NULL AND lhhm.syncStatus IN (:status) THEN 1 ELSE NULL END AS assignHousehold FROM HouseHoldMember AS hhm LEFT JOIN Household as hh ON hh.id = hhm.household_id LEFT JOIN VillageEntity AS ve ON ve.id = COALESCE(hh.village_id, hhm.villageId) LEFT JOIN SubVillageEntity AS sv ON sv.id = COALESCE(hh.sub_village_id, hhm.sub_village_id) LEFT JOIN LinkHouseholdMember AS lhhm ON lhhm.memberId = hhm.fhir_id WHERE hhm.id NOT IN (:memberIds) AND (hh.id IS NULL OR hh.fhir_id IS NOT NULL) AND hhm.sync_status IN (:status)")
    suspend fun getOtherHouseholdMembers(
        memberIds: List<String>,
        status: List<String> = listOf(
            OfflineSyncStatus.NotSynced.name,
            OfflineSyncStatus.NetworkError.name,
        ),
    ): List<HouseHoldMember>

    @Query("SELECT COUNT(id) FROM HouseholdMember where sync_status IN (:syncStatus)")
    suspend fun getUnSyncedCount(
        syncStatus: List<String> = listOf(
            OfflineSyncStatus.NotSynced.name,
            OfflineSyncStatus.NetworkError.name,
        ),
    ): Int

    @Query("SELECT id FROM HouseholdMember WHERE fhir_id =:fhirId")
    suspend fun getHouseholdMemberIdByFhirId(fhirId: String): Long?

    @Query("SELECT hhm.name, hhm.gender, hhm.date_of_birth AS dateOfBirth, hhm.patient_id AS patientId, CASE WHEN hh.sub_village_id IS NOT NULL THEN CAST(hh.sub_village_id AS TEXT) WHEN hhm.sub_village_id IS NOT NULL THEN CAST(hhm.sub_village_id AS TEXT) ELSE '0' END as subVillageId, CASE WHEN hh.village_id IS NOT NULL THEN CAST(hh.village_id AS TEXT) WHEN hhm.villageId IS NOT NULL THEN CAST(hhm.villageId AS TEXT) ELSE '0' END as villageId, hhm.fhir_id AS memberId, hh.household_no as householdNo, hh.fhir_id AS householdId, hhm.id AS id, COALESCE(hh.id, 0) AS householdLocalId, NULL AS contactTracingStatus, NULL AS isPregnant, hhm.phone_number AS phoneNumber FROM HouseholdMember AS hhm LEFT JOIN Household AS hh ON hh.id = hhm.household_id WHERE hhm.id=:id")
    suspend fun getAssessmentMemberDetails(id: Long): AssessmentMemberDetails

    @Query("DELETE FROM HouseholdMember")
    suspend fun deleteAllHouseholdMembers()

    @Query("SELECT patient_id FROM HouseholdMember WHERE fhir_id =:fhirId")
    suspend fun getPatientIdByFhirId(fhirId: String): String?

    @Query("SELECT patient_id FROM HouseholdMember WHERE id =:id")
    suspend fun getPatientIdById(id: Long): String

    @Query("SELECT * FROM HouseholdMember WHERE fhir_id = :fhirId LIMIT 1")
    suspend fun getByUniqueField(fhirId: String): HouseholdMemberEntity?

    // Merges a backend member into the local row. Keeps the local copy if it's NotSynced.
    @Transaction
    suspend fun insertOrUpdateFromBE(entity: HouseholdMemberEntity): Long {
        if (!entity.isActive && entity.fhirId != null) {
            deleteRxBuddyOnDeceased(entity.fhirId!!)
        }
        val existingEntity = entity.fhirId?.let {
            getByUniqueField(it)
        }
        if (existingEntity?.sync_status != OfflineSyncStatus.NotSynced) {
            val entityToInsert = existingEntity?.let {
                entity.copy(id = it.id)
            } ?: entity
            entityToInsert.sync_status = existingEntity?.sync_status ?: OfflineSyncStatus.Success
            entityToInsert.fhirId = entity.fhirId
            entityToInsert.createdAt = entity.createdAt
            entityToInsert.updatedAt = entity.updatedAt
            return insertMember(entityToInsert)
        } else {
            return existingEntity.id
        }
    }

    @Query("DELETE FROM RxBuddyDetails WHERE patientMemberId = :memberId")
    suspend fun deleteRxBuddyOnDeceased(memberId: String)

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus WHERE id IN (:memberIds)")
    suspend fun updateInProgress(
        memberIds: List<String>,
        syncStatus: String,
    )

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus WHERE id = :id")
    suspend fun changeMemberDetailsToNotSynced(
        id: Long,
        syncStatus: OfflineSyncStatus = OfflineSyncStatus.NotSynced,
    )

    @Query("UPDATE HouseholdMember SET isActive = :status, sync_status =:syncStatus, updated_at =:updatedAt  WHERE id = :id")
    suspend fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
        syncStatus: OfflineSyncStatus,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE HouseholdMember SET isActive = :status, sync_status =:syncStatus , deceasedReason=:deceasedReason ,updated_at =:updatedAt WHERE id = :id")
    suspend fun updateMemberDeceasedReason(
        id: Long,
        status: Boolean,
        syncStatus: OfflineSyncStatus,
        deceasedReason: String?,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE householdmember SET phone_number = :phoneNumber, sync_status =:syncStatus, updated_at =:updatedAt  WHERE household_id = :householdId AND is_house_hold_head = 1")
    suspend fun updatePhoneNumberForHouseholdHead(
        householdId: Long,
        phoneNumber: String?,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE HouseholdMember SET household_id = :householdId, sync_status =:syncStatus, updated_at =:updatedAt  WHERE fhir_id IN (:memberIds)")
    suspend fun updateHouseholdHeadAndRelationShip(
        memberIds: List<String>,
        householdId: Long,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("SELECT date_of_birth FROM HouseholdMember WHERE household_id = :householdId AND is_house_hold_head = 1 LIMIT 1")
    suspend fun getHouseholdHeadDob(
        householdId: Long,
    ): String

    @Query("SELECT hm.* FROM HouseholdMember AS hm WHERE hm.household_id = :hhId")
    fun getHouseholdMemberWithTBContactTraceStatus(hhId: Long): LiveData<List<HouseholdMemberEntity>>

    @Query("UPDATE PregnancyDetail SET tbContactTraceStatus = :tbContactTraceStatus WHERE householdMemberLocalId = :householdMemberId")
    suspend fun updateTBContactTraceStatus(
        householdMemberId: Long,
        tbContactTraceStatus: Int,
    )

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus WHERE id = :memberId AND (:isPregnant IS NULL OR :isPregnant IS NOT NULL)")
    suspend fun updatePregnantStatus(
        memberId: Long,
        isPregnant: Boolean,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
    )

    @Query("SELECT * FROM HouseholdMember WHERE household_id = :householdId AND id != :patientId AND isActive=1 AND substr(date_of_birth, 1, 10) < date('now','-10 years') ")
    suspend fun getOtherHouseholdExcludeTBPatient(
        householdId: Long,
        patientId: Long,
    ): List<HouseholdMemberEntity>

    @Query("SELECT hhm.*, hh.fhir_id as household_fhir_id, hh.village_id as village_id, ve.name as village_name FROM HouseHoldMember AS hhm INNER JOIN Household as hh ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id Where hhm.id = :hhmId")
    suspend fun getHouseholdMemberForRxBuddy(hhmId: Long): HouseHoldMember

    @Query("SELECT id, isActive FROM HouseholdMember WHERE fhir_id =:fhirId")
    suspend fun getHouseholdMemberIdAndStatusByFhirId(fhirId: String): HouseholdMemberStatus?

    @Query("SELECT fhir_id FROM HouseholdMember WHERE id =:hhmId")
    suspend fun getMemberFhirIdByLocalId(hhmId: Long): String?

    @Query("SELECT hhm.id FROM HouseholdMember AS hhm JOIN TreatmentDetailsEntity AS td ON hhm.fhir_id = td.memberId WHERE hhm.household_id =:householdId")
    suspend fun getTbPatientLocalIdByHouseholdId(householdId: Long): MutableList<Long>

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus, updated_at =:updatedAt WHERE id = :memberId AND (:status IS NULL OR :status IS NOT NULL)")
    suspend fun updateContactTracingStatus(
        memberId: Long,
        status: Int?,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus, updated_at =:updatedAt WHERE household_id = :householdId AND id != :tbHHMId")
    suspend fun updateContactTracingForLinkTbPatient(
        tbHHMId: Long,
        householdId: Long,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("SELECT COUNT(id) FROM HouseholdMember WHERE disability='present' AND household_id=:householdId")
    suspend fun getDisabilityMembersCountForHousehold(householdId: Long): Int

    @Query(
        """
        UPDATE HouseholdMember
        SET guardian_hh_member_id = (
            SELECT guardian.id
            FROM HouseholdMember AS guardian
            WHERE guardian.fhir_id = HouseholdMember.guardian_hh_member_fhir_id
        )
        WHERE guardian_hh_member_fhir_id IS NOT NULL
        """,
    )
    suspend fun updateGuardianHhIds(): Int

    /**
     * Internal raw-query entry point. Use [getServiceMembers] instead.
     *
     * **observedEntities** ensures Room re-delivers LiveData whenever any of the
     * three underlying tables change.
     */
    @RawQuery(observedEntities = [HouseholdEntity::class, HouseholdMemberEntity::class, MemberAssessmentHistoryEntity::class])
    fun getServiceMembersRaw(query: SimpleSQLiteQuery): LiveData<List<HouseholdMemberWithTb>>

    /**
     * Returns a live list of members with last-activity info.
     *
     * **Filters** (all optional):
     * @param searchInput match against member name or phone number; blank = no filter
     * @param filterBySs whitelist of Shasthya Shebika IDs; empty = no filter
     * @param filterBySubVillages whitelist of sub-village IDs; empty = no filter
     * @param staticFilter selected static service bucket to apply
     *
     */
    fun getServiceMembers(
        searchInput: String?,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        staticFilter: ServiceStaticFilter,
        /** FO/PO: members may have no household; use member-level joins and sub-village like external flow. */
        allowNullHousehold: Boolean = false,
        qrCode: String? = null,
        restrictExternalToSkCreator: Boolean = false,
    ): LiveData<List<HouseholdMemberWithTb>> {
        val query = ServiceMemberQueryBuilder.buildListQuery(
            searchInput = searchInput,
            filterBySs = filterBySs,
            filterBySubVillages = filterBySubVillages,
            staticFilter = staticFilter,
            allowNullHousehold = allowNullHousehold,
            qrCode = qrCode,
            restrictExternalToSkCreator = restrictExternalToSkCreator,
        )
        return attachAssessmentHistoryToMembers(getServiceMembersRaw(query))
    }

    @Query(
        """
        SELECT memberId, serviceProvided, visitDate, customStatus
        FROM MemberAssessmentHistory
        WHERE memberId IN (:memberIds)
            AND serviceProvided IS NOT NULL AND serviceProvided != ''
        ORDER BY visitDate DESC, id DESC
        """,
    )
    fun getServiceIconsForMembers(
        memberIds: List<Long>,
    ): LiveData<List<MemberServiceIconRow>>

    @Query(
        """
        SELECT * FROM MemberAssessmentHistory
        WHERE memberId IN (:memberIds)
        ORDER BY visitDate DESC, id DESC
        """,
    )
    fun getAssessmentHistoryForMembers(
        memberIds: List<Long>,
    ): LiveData<List<MemberAssessmentHistoryEntity>>

    private fun attachAssessmentHistoryToMembers(
        membersLiveData: LiveData<List<HouseholdMemberWithTb>>,
    ): LiveData<List<HouseholdMemberWithTb>> {
        val result = MediatorLiveData<List<HouseholdMemberWithTb>>()
        var latestMembers: List<HouseholdMemberWithTb> = emptyList()
        var historySource: LiveData<List<MemberServiceIconRow>>? = null

        fun attachHistory(history: List<MemberServiceIconRow>?) {
            val grouped = history.orEmpty().groupBy { it.memberId }
            result.value = latestMembers.map { member ->
                member.apply {
                    assessmentHistory = grouped[member.id].orEmpty().map { row ->
                        MemberAssessmentHistoryEntity(
                            memberId = row.memberId,
                            visitDate = row.visitDate,
                            serviceProvided = row.serviceProvided,
                            customStatus = row.customStatus,
                            latestVisit = false,
                            referralStatus = null,
                            referralReason = null,
                        )
                    }
                }
            }
        }

        result.addSource(membersLiveData) { members ->
            latestMembers = members
            historySource?.let { result.removeSource(it) }
            val memberIds = members.map { it.id }
            if (memberIds.isEmpty()) {
                result.value = emptyList()
                return@addSource
            }
            val newHistorySource = getServiceIconsForMembers(memberIds)
            historySource = newHistorySource
            result.addSource(newHistorySource, ::attachHistory)
        }

        return result
    }

    /**
     * Combined count query: returns a single row with one `cnt_<index>` column per requested
     * filter (see [ServiceMemberCountQueryBuilder]). Read via [getServiceMemberCounts].
     */
    @RawQuery
    fun getServiceMemberCountsCursor(query: SimpleSQLiteQuery): Cursor

    /**
     * Returns counts for [filters] in a single pass over the member table.
     * Dynamic filters match [getServiceMembers].
     */
    suspend fun getServiceMemberCounts(
        filters: List<ServiceStaticFilter>,
        searchInput: String = "",
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
        allowNullHousehold: Boolean = false,
        qrCode: String? = null,
        restrictExternalToSkCreator: Boolean = false,
    ): Map<ServiceStaticFilter, Int> {
        if (filters.isEmpty()) return emptyMap()
        val query = ServiceMemberCountQueryBuilder.buildCombinedCountQuery(
            filters = filters,
            searchInput = searchInput,
            filterBySs = filterBySs,
            filterBySubVillages = filterBySubVillages,
            allowNullHousehold = allowNullHousehold,
            qrCode = qrCode,
            restrictExternalToSkCreator = restrictExternalToSkCreator,
        )
        val counts = LinkedHashMap<ServiceStaticFilter, Int>()
        getServiceMemberCountsCursor(query).use { cursor ->
            if (cursor.moveToFirst()) {
                filters.forEachIndexed { index, filter ->
                    val columnIndex = cursor.getColumnIndex("${ServiceMemberCountQueryBuilder.COUNT_COLUMN_PREFIX}$index")
                    counts[filter] = if (columnIndex >= 0) cursor.getInt(columnIndex) else 0
                }
            } else {
                filters.forEach { counts[it] = 0 }
            }
        }
        return counts
    }

    /**
     * Retrieves a member and their associated assessment history, sorted by visit date in descending order.
     * Uses a LEFT JOIN to combine [HouseholdMemberEntity] and [MemberAssessmentHistoryEntity] in a single query.
     *
     * Recent pregnancy is not included here; [org.medtroniclabs.uhis.db.local.RoomHelper.getMemberWithAssessmentHistory]
     * composes the full [org.medtroniclabs.uhis.db.response.MemberAssessmentHistoryResponse], including the
     * pregnancy episode with the latest [org.medtroniclabs.uhis.db.entity.PregnancyDetail.endAt].
     *
     * @param memberId The local ID of the member to retrieve.
     * @return A map where the key is the member entity and the value is a list of their assessment histories.
     */
    @Transaction
    @Query("SELECT * FROM HouseHoldMember AS hhm LEFT JOIN memberassessmenthistory AS mah ON hhm.id = mah.memberId WHERE hhm.id = :memberId ORDER BY mah.visitDate DESC")
    fun getMemberWithAssessmentHistory(memberId: Long): LiveData<Map<HouseholdMemberEntity, List<MemberAssessmentHistoryEntity>>?>

    @Query("SELECT * FROM HouseHoldMember WHERE qr_code = :qrCode AND (:memberId IS NULL OR id != :memberId)")
    suspend fun getMemberByQRCode(
        qrCode: String,
        memberId: Long?,
    ): List<HouseholdMemberEntity>
}
