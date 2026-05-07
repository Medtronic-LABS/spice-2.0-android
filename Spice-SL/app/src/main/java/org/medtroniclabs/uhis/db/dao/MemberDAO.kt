package org.medtroniclabs.uhis.db.dao

import androidx.lifecycle.LiveData
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
import org.medtroniclabs.uhis.model.MemberDobGenderModel
import org.medtroniclabs.uhis.model.assessment.AssessmentMemberDetails
import org.medtroniclabs.uhis.model.services.ServiceMemberCounts
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
                        mahAgg.services AS services,
                        mahAgg.recent_service_date
                    FROM householdmember AS hhm
                    LEFT JOIN TreatmentDetailsEntity AS td ON hhm.fhir_id = td.memberId

                    LEFT JOIN (
                        SELECT
                            memberId,
                            '[' || GROUP_CONCAT('"' || serviceProvided || '"') || ']' AS services,
                            MAX(last_visit) AS recent_service_date
                        FROM (
                            SELECT
                                mah.memberId,
                                mah.serviceProvided,
                                MAX(strftime('%s', mah.visitDate) * 1000) AS last_visit
                            FROM MemberAssessmentHistory mah
                            GROUP BY mah.memberId, mah.serviceProvided
                            ORDER BY last_visit DESC
                        )
                        GROUP BY memberId
                    ) AS mahAgg
                    ON mahAgg.memberId = hhm.id

                    WHERE hhm.household_id = :houseHoldId
    """,
    )
    fun getAllHouseHoldMembersLiveData(houseHoldId: Long): LiveData<List<HouseholdMemberWithTb>>

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

    @Transaction
    suspend fun insertOrUpdateFromBE(entity: HouseholdMemberEntity): Long {
        if (!entity.isActive && entity.fhirId != null) {
            deleteRxBuddyOnDeceased(entity.fhirId!!)
        }
        val existingEntity = entity.fhirId?.let {
            getByUniqueField(it)
        }
        if (existingEntity?.sync_status != OfflineSyncStatus.NotSynced) {
            val entityToInsert = existingEntity?.let { entity.copy(id = it.id) } ?: entity
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
        searchInput: String,
        filterBySs: List<Long>,
        filterBySubVillages: List<Long>,
        staticFilter: ServiceStaticFilter,
    ): LiveData<List<HouseholdMemberWithTb>> {
        val args = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        // Check if this is an external-members scoped filter
        val isExternalMember =
            staticFilter == ServiceStaticFilter.EXTERNAL_MEMBERS ||
                staticFilter == ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN

        if (staticFilter != ServiceStaticFilter.EXTERNAL_MEMBERS &&
            staticFilter != ServiceStaticFilter.ALL_MEMBERS &&
            staticFilter != ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS
        ) {
            conditions += ServiceFilterConditions.IS_ACTIVE
        }

        if (searchInput.isNotBlank()) {
            conditions += "(hhm.name LIKE ? OR hhm.phone_number LIKE ?)"
            val pattern = "%${searchInput.trim()}%"
            args += pattern
            args += pattern
        }

        if (filterBySubVillages.isNotEmpty() || filterBySs.isNotEmpty()) {
            val subVillageColumn = if (isExternalMember) "hhm.sub_village_id" else "hh.sub_village_id"
            val subVillageFilterConditions = mutableListOf<String>()
            if (filterBySubVillages.isNotEmpty()) {
                subVillageFilterConditions += "$subVillageColumn IN (${filterBySubVillages.joinToString(",") { "?" }})"
                args.addAll(filterBySubVillages)
            }
            if (filterBySs.isNotEmpty()) {
                val ssPlaceholders = filterBySs.joinToString(",") { "?" }
                subVillageFilterConditions +=
                    """
                    $subVillageColumn IN (
                        SELECT DISTINCT sslv.subVillageId
                        FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                        WHERE sslv.shasthyaShebikaId IN ($ssPlaceholders)
                    )
                    """.trimIndent()
                args.addAll(filterBySs)
            }
            conditions += "(${subVillageFilterConditions.joinToString(" OR ")})"
        }

        when (staticFilter) {
            ServiceStaticFilter.FAMILY_PLANNING_COUNSELLING_ELIGIBLE -> {
                conditions += ServiceFilterConditions.FAMILY_PLANNING
            }
            ServiceStaticFilter.PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.PREGNANT_WOMEN
            }
            ServiceStaticFilter.POSTNATAL_CARE_MOTHERS -> {
                conditions += ServiceFilterConditions.POSTNATAL_MOTHERS
            }
            ServiceStaticFilter.CHILDREN_UNDER_TWO_YEARS -> {
                conditions += ServiceFilterConditions.CHILDREN_UNDER_TWO
            }
            ServiceStaticFilter.EXPECTED_DELIVERIES -> {
                conditions += ServiceFilterConditions.EXPECTED_DELIVERIES
            }
            ServiceStaticFilter.PENDING_DELIVERIES -> {
                conditions += ServiceFilterConditions.PENDING_DELIVERIES
            }
            ServiceStaticFilter.HIGH_RISK_PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.HIGH_RISK_PREGNANT_WOMEN
            }
            ServiceStaticFilter.EXTERNAL_MEMBERS -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
            }
            ServiceStaticFilter.EXTERNAL_PREGNANT_WOMEN -> {
                conditions += ServiceFilterConditions.EXTERNAL_MEMBER
                conditions += ServiceFilterConditions.PREGNANT_WOMEN
            }
            else -> {}
        }

        val whereClause = if (conditions.isEmpty()) {
            ""
        } else {
            "WHERE ${conditions.joinToString(" AND ")}"
        }

        // For external members, use LEFT JOIN since household_id is NULL
        val householdJoin = if (isExternalMember) {
            "LEFT JOIN Household AS hh ON hh.id = hhm.household_id"
        } else {
            "INNER JOIN Household AS hh ON hh.id = hhm.household_id"
        }

        val ssJoin = if (isExternalMember) {
            "LEFT JOIN ShasthyaShebikaEntity AS ss ON hhm.shasthya_shebika_id = ss.id"
        } else {
            "INNER JOIN ShasthyaShebikaEntity AS ss ON ss.id = hh.shasthya_shebika_id"
        }

        val svJoin = if (isExternalMember) {
            "LEFT JOIN SubVillageEntity AS sv ON hhm.sub_village_id = sv.id"
        } else {
            "INNER JOIN SubVillageEntity AS sv ON sv.id = hh.sub_village_id"
        }

        val latestPregnancyEddExpr =
            """
            (
                SELECT substr(lp.estimatedDeliveryDate, 1, 10)
                FROM (${ServiceFilterConditions.LATEST_PREGNANCY_SUBQUERY}) AS lp
            )
            """.trimIndent()

        val orderByClause = when (staticFilter) {
            ServiceStaticFilter.EXPECTED_DELIVERIES -> {
                "ORDER BY $latestPregnancyEddExpr ASC, hhm.id DESC"
            }
            ServiceStaticFilter.PENDING_DELIVERIES -> {
                "ORDER BY $latestPregnancyEddExpr DESC, hhm.id DESC"
            }
            else -> {
                "ORDER BY COALESCE(mahAgg.recent_service_date, hhm.created_at) DESC, hhm.id DESC"
            }
        }

        val query =
            """
            SELECT
                hhm.*, td.diagnoses,
                mahAgg.services AS services,
                mahAgg.recent_service_date,
                COALESCE(ss.name, '') AS shasthya_shebika_name,
                COALESCE(ss.ssId, '') AS shasthya_shebika_ssId,
                COALESCE(sv.name, '') AS sub_village_name
            FROM householdmember AS hhm

            $householdJoin

            $ssJoin

            $svJoin

            LEFT JOIN TreatmentDetailsEntity AS td
                ON hhm.fhir_id = td.memberId

            LEFT JOIN (
                SELECT
                    memberId,
                    '[' || GROUP_CONCAT('"' || serviceProvided || '"') || ']' AS services,
                    MAX(last_visit) AS recent_service_date
                FROM (
                    SELECT
                        mah.memberId,
                        mah.serviceProvided,
                        MAX(strftime('%s', mah.visitDate) * 1000) AS last_visit
                    FROM MemberAssessmentHistory mah
                    GROUP BY mah.memberId, mah.serviceProvided
                    ORDER BY last_visit DESC
                )
                GROUP BY memberId
            ) AS mahAgg
            ON mahAgg.memberId = hhm.id

            $whereClause
            $orderByClause
            """.trimIndent()
        return getServiceMembersRaw(SimpleSQLiteQuery(query, args.toTypedArray()))
    }

    /**
     * Internal raw-query entry point for all counts. Use [getAllServiceMemberCounts] instead.
     *
     * The query should project all aliases required by [ServiceMemberCounts].
     */
    @RawQuery(observedEntities = [HouseholdEntity::class, HouseholdMemberEntity::class, MemberAssessmentHistoryEntity::class])
    suspend fun getAllServiceMemberCountsRaw(query: SimpleSQLiteQuery): ServiceMemberCounts

    /**
     * Returns counts for all service static filters in a single optimized query.
     *
     * Uses conditional aggregation (SUM/CASE WHEN) to compute all counts in one database pass.
     * Dynamic filters are applied consistently with [getServiceMembers].
     *
     * **Filters** (all optional):
     * @param searchInput match against member name or phone number; blank = no filter
     * @param filterBySs whitelist of Shasthya Shebika IDs; empty = no filter
     * @param filterBySubVillages whitelist of sub-village IDs; empty = no filter
     *
     * @return [ServiceMemberCounts] containing counts for each filter
     */
    suspend fun getAllServiceMemberCounts(
        searchInput: String = "",
        filterBySs: List<Long> = emptyList(),
        filterBySubVillages: List<Long> = emptyList(),
    ): ServiceMemberCounts {
        val args = mutableListOf<Any>()
        val globalArgs = mutableListOf<Any>()
        val globalConditions = mutableListOf<String>()

        if (searchInput.isNotBlank()) {
            globalConditions += "(hhm.name LIKE ? OR hhm.phone_number LIKE ?)"
            val pattern = "%${searchInput.trim()}%"
            globalArgs += pattern
            globalArgs += pattern
        }

        val globalWhereClause = if (globalConditions.isEmpty()) "" else "WHERE ${globalConditions.joinToString(" AND ")}"

        val ssPlaceholders = if (filterBySs.isNotEmpty()) filterBySs.joinToString(",") { "?" } else ""
        val subVillagePlaceholders = if (filterBySubVillages.isNotEmpty()) filterBySubVillages.joinToString(",") { "?" } else ""

        val householdSubVillageFilters = mutableListOf<String>()
        val householdFilterArgs = mutableListOf<Any>()
        if (filterBySubVillages.isNotEmpty()) {
            householdSubVillageFilters += "hh.sub_village_id IN ($subVillagePlaceholders)"
            householdFilterArgs.addAll(filterBySubVillages)
        }
        if (filterBySs.isNotEmpty()) {
            householdSubVillageFilters +=
                """
                hh.sub_village_id IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN ($ssPlaceholders)
                )
                """.trimIndent()
            householdFilterArgs.addAll(filterBySs)
        }
        val householdAreaFilter = if (householdSubVillageFilters.isNotEmpty()) {
            "AND (${householdSubVillageFilters.joinToString(" OR ")})"
        } else {
            ""
        }

        val externalSubVillageFilters = mutableListOf<String>()
        val externalFilterArgs = mutableListOf<Any>()
        if (filterBySubVillages.isNotEmpty()) {
            externalSubVillageFilters += "hhm.sub_village_id IN ($subVillagePlaceholders)"
            externalFilterArgs.addAll(filterBySubVillages)
        }
        if (filterBySs.isNotEmpty()) {
            externalSubVillageFilters +=
                """
                hhm.sub_village_id IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN ($ssPlaceholders)
                )
                """.trimIndent()
            externalFilterArgs.addAll(filterBySs)
        }
        val externalAreaFilter = if (externalSubVillageFilters.isNotEmpty()) {
            "AND (${externalSubVillageFilters.joinToString(" OR ")})"
        } else {
            ""
        }

        val query =
            """
            SELECT
                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                THEN 1 ELSE 0 END) AS all_members,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.FAMILY_PLANNING}
                THEN 1 ELSE 0 END) AS family_planning,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.PREGNANT_WOMEN}
                THEN 1 ELSE 0 END) AS pregnant_women,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.HIGH_RISK_PREGNANT_WOMEN}
                THEN 1 ELSE 0 END) AS high_risk_pregnant,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.POSTNATAL_MOTHERS}
                THEN 1 ELSE 0 END) AS postnatal_mothers,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.CHILDREN_UNDER_TWO}
                THEN 1 ELSE 0 END) AS children_under_two,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.EXPECTED_DELIVERIES}
                THEN 1 ELSE 0 END) AS expected_deliveries,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.HAS_HOUSEHOLD}
                    $householdAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.PENDING_DELIVERIES}
                THEN 1 ELSE 0 END) AS pending_deliveries,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.EXTERNAL_MEMBER}
                    $externalAreaFilter
                THEN 1 ELSE 0 END) AS external_members,

                SUM(CASE WHEN
                    ${ServiceFilterConditions.EXTERNAL_MEMBER}
                    $externalAreaFilter
                    AND ${ServiceFilterConditions.IS_ACTIVE}
                    AND ${ServiceFilterConditions.PREGNANT_WOMEN}
                THEN 1 ELSE 0 END) AS external_pregnant
            FROM householdmember AS hhm
            LEFT JOIN Household AS hh ON hh.id = hhm.household_id
            $globalWhereClause
            """.trimIndent()

        // Placeholder order in SELECT is:
        // household filter x8, external filter x2 then global WHERE args.
        repeat(8) { args.addAll(householdFilterArgs) }
        repeat(2) { args.addAll(externalFilterArgs) }
        args.addAll(globalArgs)

        return getAllServiceMemberCountsRaw(SimpleSQLiteQuery(query, args.toTypedArray()))
    }

    /**
     * Retrieves a member and their associated assessment history, sorted by visit date in descending order.
     * Uses a LEFT JOIN to combine [HouseholdMemberEntity] and [MemberAssessmentHistoryEntity] in a single query.
     *
     * @param memberId The local ID of the member to retrieve.
     * @return A map where the key is the member entity and the value is a list of their assessment histories.
     */
    @Transaction
    @Query("SELECT * FROM HouseHoldMember AS hhm LEFT JOIN memberassessmenthistory AS mah ON hhm.id = mah.memberId WHERE hhm.id = :memberId ORDER BY mah.visitDate DESC")
    fun getMemberWithAssessmentHistory(memberId: Long): LiveData<Map<HouseholdMemberEntity, List<MemberAssessmentHistoryEntity>>?>
}
