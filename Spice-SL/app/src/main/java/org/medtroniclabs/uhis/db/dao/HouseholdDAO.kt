package org.medtroniclabs.uhis.db.dao

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import org.medtroniclabs.uhis.data.model.HouseholdCardDetail
import org.medtroniclabs.uhis.data.offlinesync.model.HouseHold
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.AssessmentEntity
import org.medtroniclabs.uhis.db.entity.EntitiesName
import org.medtroniclabs.uhis.db.entity.HouseholdEntity
import org.medtroniclabs.uhis.db.entity.HouseholdMemberEntity
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.db.response.HouseholdMemberCount
import org.medtroniclabs.uhis.db.response.MemberAssessmentHistoryWithHouseholdId

@Dao
interface HouseholdDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHouseHold(houseHold: HouseholdEntity): Long

    @Update
    suspend fun updateHouseHold(houseHold: HouseholdEntity)

    @Query("SELECT * FROM Household WHERE fhir_id = :fhirId LIMIT 1")
    suspend fun getByUniqueField(fhirId: String): HouseholdEntity?

    @Transaction
    suspend fun insertOrUpdateFromBE(entity: HouseholdEntity): Long {
        val existingEntity = entity.fhirId?.let { getByUniqueField(it) }
        if (existingEntity?.sync_status != OfflineSyncStatus.NotSynced) {
            val entityToInsert = existingEntity?.let { entity.copy(id = it.id) } ?: entity
            entityToInsert.sync_status = existingEntity?.sync_status ?: OfflineSyncStatus.Success
            entityToInsert.fhirId = entity.fhirId
            entityToInsert.createdAt = entity.createdAt
            entityToInsert.updatedAt = entity.updatedAt
            return insertHouseHold(entityToInsert)
        } else {
            return existingEntity.id
        }
    }

    @Query("SELECT MAX(household_no) FROM household WHERE village_id = :villageId")
    suspend fun getLastHouseholdNo(villageId: Long): Long?

    @Query("SELECT COUNT(*) FROM household WHERE household_no = :householdNo")
    suspend fun checkHouseholdNumberExists(householdNo: Long): Int

    @Query("SELECT * FROM HouseHold WHERE id= :houseHoldId")
    suspend fun getHouseHoldDetailsById(houseHoldId: Long): HouseholdEntity

    @Query("SELECT hh.no_of_people as noOfPeople, count(hhm.id) AS memberCount FROM HouseHold AS hh INNER JOIN HouseHoldMember AS hhm ON hhm.household_id = hh.id WHERE household_id =:householdId")
    fun getHouseholdMemberCountLiveData(householdId: Long): LiveData<HouseholdMemberCount>

    @Query(
        """
        UPDATE HouseHold
            SET
                no_of_people = (
                    SELECT COUNT(id)
                        FROM ${EntitiesName.HOUSEHOLD_MEMBER} AS member
                    WHERE member.household_id = :householdId
                ),
                sync_status =:syncStatus,
                updated_at =:updatedAt
        WHERE id =:householdId
        AND no_of_people < (
                SELECT COUNT(member.id)
                        FROM ${EntitiesName.HOUSEHOLD_MEMBER} AS member
                WHERE member.household_id = :householdId
        )
        """,
    )
    suspend fun updateHeadCountIfUnderCounted(
        householdId: Long,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query("SELECT hh.*, ve.name as villageName FROM HouseHold as hh INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id WHERE hh.id NOT IN (:hhIds) AND hh.sync_status IN (:status)")
    suspend fun getAllUnSyncedHouseHolds(
        hhIds: List<String>,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name),
    ): List<HouseHold>

    @RawQuery
    suspend fun updateFhirId(query: SimpleSQLiteQuery): Long

    @Query("SELECT COUNT(id) FROM Household WHERE sync_status IN (:syncStatus)")
    suspend fun getUnSyncedCount(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): Int

    @Query("SELECT id FROM Household WHERE fhir_id =:fhirId")
    suspend fun getHouseholdIdByFhirId(fhirId: String): Long?

    @Query("DELETE FROM Household")
    suspend fun deleteAllHouseholds()

    /**
     * Returns count of households registered within optional date range and filtered by SS/Sub-village.
     * Date comparisons are based on household creation time only.
     */
    @Query(
        """
        SELECT COUNT(hh.id)
        FROM Household AS hh
        WHERE (
            :startDate IS NULL OR
            date(datetime(hh.created_at / 1000, 'unixepoch', 'localtime')) >= :startDate
        )
        AND (
            :endDate IS NULL OR
            date(datetime(hh.created_at / 1000, 'unixepoch', 'localtime')) <= :endDate
        )
        AND (
            CASE
                WHEN :subVillageIdsSize > 0
                THEN hh.sub_village_id IN (:subVillageIds)

                WHEN :ssIdsSize > 0
                THEN hh.sub_village_id IN (SELECT DISTINCT sslv.subVillageId FROM ShasthyaShebikaLinkedVillageEntity AS sslv WHERE sslv.shasthyaShebikaId IN (:ssIds))

                ELSE 1
            END
        )
        """,
    )
    suspend fun getHouseholdRegisteredCount(
        startDate: String?,
        endDate: String?,
        ssIds: List<Long>,
        ssIdsSize: Int,
        subVillageIds: List<Long>,
        subVillageIdsSize: Int,
    ): Int?

    @Query(
        "SELECT hh.id, hh.name, hh.household_no AS householdNo, ve.name AS villageName, hh.no_of_people AS memberRegistered, COUNT(hhm.id) AS memberAdded " +
            "FROM Household as hh INNER JOIN VillageEntity as ve ON hh.village_id = ve.id LEFT JOIN HouseholdMember as hhm ON hhm.household_id = hh.id  WHERE hh.id =:id",
    )
    fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail>

    @Query("UPDATE HouseHold SET sync_status =:syncStatus WHERE id IN (:householdIds)")
    suspend fun updateInProgress(
        householdIds: List<String>,
        syncStatus: String,
    )

    @Query("SELECT hh.*, ve.name as villageName FROM HouseHold as hh INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id INNER JOIN HouseholdMember as hhm ON hh.id = hhm.household_id WHERE hh.fhir_id IS NULL AND hhm.id = :hhmId AND hh.sync_status IN (:status)")
    suspend fun getUnSyncedHouseHoldByMemberId(
        hhmId: Long,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name),
    ): HouseHold?

    /**
     * Returns number of households registered in particular sub village
     */
    @Query("SELECT COUNT(id) FROM household WHERE sub_village_id = :subVillageId")
    suspend fun getHouseholdsCountBasedSubVillage(
        subVillageId: Long,
    ): Int

    /**
     * Internal raw-query entry point. Use [getHouseholdsWithLastActivity] instead.
     *
     * **observedEntities** ensures Room re-delivers LiveData whenever any of the
     * three underlying tables change.
     */
    @RawQuery(observedEntities = [HouseholdEntity::class, HouseholdMemberEntity::class, AssessmentEntity::class, MemberAssessmentHistoryEntity::class])
    fun getHouseholdsRaw(query: SimpleSQLiteQuery): LiveData<List<HouseHoldEntityWithLastActivity>>

    @Query(
        """
        SELECT mah.*, hm.household_id AS household_id
        FROM MemberAssessmentHistory mah
        INNER JOIN HouseholdMember hm ON hm.id = mah.memberId
        WHERE hm.household_id IN (:householdIds)
        ORDER BY mah.visitDate DESC, mah.id DESC
        """,
    )
    fun getAssessmentHistoryForHouseholds(
        householdIds: List<Long>,
    ): LiveData<List<MemberAssessmentHistoryWithHouseholdId>>

    /**
     * Returns a live list of households with last-activity info.
     *
     * **Filters** (all optional):
     * @param searchTerm           match against household name or number; blank = no filter
     * @param shasthyaShebikaIds   whitelist of Shasthya Shebika IDs; null/empty = no filter
     * @param subVillageIds        whitelist of sub-village IDs; null/empty = no filter
     *
     * **Sorting** (pick one, all DESC):
     * @param sortOrder  [HouseholdSortOrder.HOUSEHOLD_NO]           → household_no DESC
     *                   [HouseholdSortOrder.LAST_VISIT_DATE]        → last_activity_at DESC
     *                   [HouseholdSortOrder.LAST_MEMBER_REGISTRATION] → last_member_registered_at DESC
     *                   [HouseholdSortOrder.DEFAULT]                → household.id DESC
     */
    fun getHouseholdsWithLastActivity(
        searchTerm: String = "",
        villageIds: List<Long> = emptyList(),
        shasthyaShebikaIds: List<Long> = emptyList(),
        subVillageIds: List<Long> = emptyList(),
        hhIds: List<Long> = emptyList(),
        sortOrder: HouseholdSortOrder = HouseholdSortOrder.DEFAULT,
    ): LiveData<List<HouseHoldEntityWithLastActivity>> {
        val args = mutableListOf<Any>()
        val conditions = mutableListOf<String>()

        if (searchTerm.isNotBlank()) {
            conditions += "(hh.name LIKE ? OR hh.household_no LIKE ? OR EXISTS (SELECT 1 FROM HouseholdMember hm WHERE hm.household_id = hh.id AND (hm.phone_number LIKE ? OR hm.national_id LIKE ?)))"
            val pattern = "%${searchTerm.trim()}%"
            args += pattern
            args += pattern
            args += pattern
            args += pattern
        }
        if (villageIds.isNotEmpty()) {
            val placeholders = villageIds.joinToString(",") { "?" }
            conditions += "hh.village_id IN ($placeholders)"
            args.addAll(villageIds)
        }
        if (subVillageIds.isNotEmpty() || shasthyaShebikaIds.isNotEmpty()) {
            val subVillageFilterConditions = mutableListOf<String>()
            if (subVillageIds.isNotEmpty()) {
                val placeholders = subVillageIds.joinToString(",") { "?" }
                subVillageFilterConditions += "hh.sub_village_id IN ($placeholders)"
                args.addAll(subVillageIds)
            } else {
                val placeholders = shasthyaShebikaIds.joinToString(",") { "?" }
                subVillageFilterConditions +=
                    """
                    hh.sub_village_id IN (
                        SELECT DISTINCT sslv.subVillageId
                        FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                        WHERE sslv.shasthyaShebikaId IN ($placeholders)
                    )
                    """.trimIndent()
                args.addAll(shasthyaShebikaIds)
            }
            conditions += "(${subVillageFilterConditions.joinToString(" OR ")})"
        }
        if (hhIds.isNotEmpty()) {
            val placeholders = hhIds.joinToString(",") { "?" }
            conditions += "hh.id IN ($placeholders)"
            args.addAll(hhIds)
        }

        val whereClause = if (conditions.isEmpty()) {
            ""
        } else {
            "WHERE ${conditions.joinToString(" AND ")}"
        }

        val orderByClause = when (sortOrder) {
            HouseholdSortOrder.HOUSEHOLD_NO -> "fh.household_no DESC"
            HouseholdSortOrder.LAST_VISIT_DATE -> "last_activity_at DESC"
            HouseholdSortOrder.LAST_MEMBER_REGISTRATION -> "last_member_registered_at DESC"
            HouseholdSortOrder.DEFAULT -> "fh.id DESC"
        }

        val sql =
            """
            WITH filtered_households AS (
                SELECT
                    hh.id,
                    hh.name,
                    hh.household_no,
                    hh.updated_at,
                    COALESCE(ve.name, '') AS village_name,
                    COALESCE(ss.name, ss_fallback.name, '') AS shasthya_shebika_name,
                    COALESCE(ss.ssId, ss_fallback.ssId, '') AS shasthya_shebika_ssId,
                    COALESCE(sv.name, '') AS sub_village_name
                FROM Household AS hh
                -- ve/ss/sv supply display names only; scope is enforced by the WHERE clause. A
                -- reassigned household keeps its original SS (and possibly village) which may not be
                -- in this device's tables — INNER joins here wrongly hide those households, so a
                -- newly-assigned village's households never appear (UHIS-1173). Use LEFT joins.
                -- Display new ss name based on village where the old ss is not there in the DB.
                LEFT JOIN VillageEntity AS ve
                    ON ve.id = hh.village_id
                LEFT JOIN ShasthyaShebikaEntity AS ss
                    ON ss.id = hh.shasthya_shebika_id
                LEFT JOIN SubVillageEntity AS sv
                    ON sv.id = hh.sub_village_id
                LEFT JOIN ShasthyaShebikaLinkedVillageEntity AS sslv
                    ON sslv.subVillageId = hh.sub_village_id
                    AND EXISTS (
                        SELECT 1
                        FROM ShasthyaShebikaEntity AS active_ss
                        WHERE active_ss.id = sslv.shasthyaShebikaId
                            AND active_ss.isActive = true
                    )
                LEFT JOIN ShasthyaShebikaEntity AS ss_fallback
                    ON ss_fallback.id = sslv.shasthyaShebikaId
                $whereClause
            )
            SELECT
                fh.id,
                fh.name,
                fh.household_no,
                fh.village_name,
                fh.shasthya_shebika_name,
                fh.shasthya_shebika_ssId,
                fh.sub_village_name,
                memberAgg.last_member_registered_at,
                memberAgg.total_registered_members,
                MAX(
                    COALESCE(fh.updated_at, 0),
                    COALESCE(memberAgg.last_member_registered_at, 0),
                    COALESCE(assessmentAgg.last_assessment_at, 0)
                ) AS last_activity_at
            FROM filtered_households AS fh
            INNER JOIN (
                SELECT
                    household_id,
                    COUNT(id) AS total_registered_members,
                    MAX(updated_at) AS last_member_registered_at
                FROM HouseholdMember
                WHERE household_id IN (SELECT id FROM filtered_households)
                GROUP BY household_id
            ) AS memberAgg
                ON memberAgg.household_id = fh.id
            LEFT JOIN (
                SELECT
                    hm.household_id,
                    MAX(strftime('%s', mah.visitDate) * 1000) AS last_assessment_at
                FROM MemberAssessmentHistory mah
                INNER JOIN HouseholdMember hm
                    ON hm.id = mah.memberId
                WHERE hm.household_id IN (SELECT id FROM filtered_households)
                GROUP BY hm.household_id
            ) AS assessmentAgg
                ON assessmentAgg.household_id = fh.id
            ORDER BY $orderByClause
            """.trimIndent()

        val householdsLiveData = getHouseholdsRaw(SimpleSQLiteQuery(sql, args.toTypedArray()))
        val result = MediatorLiveData<List<HouseHoldEntityWithLastActivity>>()
        var latestHouseholds: List<HouseHoldEntityWithLastActivity> = emptyList()
        var historySource: LiveData<List<MemberAssessmentHistoryWithHouseholdId>>? = null

        fun attachAssessmentHistory(history: List<MemberAssessmentHistoryWithHouseholdId>?) {
            val grouped = history.orEmpty().groupBy(
                keySelector = { it.householdId },
                valueTransform = { it.history },
            )
            result.value = latestHouseholds.map { household ->
                household.apply {
                    assessmentHistory = grouped[household.id] ?: emptyList()
                }
            }
        }

        result.addSource(householdsLiveData) { households ->
            latestHouseholds = households
            historySource?.let { result.removeSource(it) }
            val householdIds = households.map { it.id }
            if (householdIds.isEmpty()) {
                result.value = emptyList()
                return@addSource
            }
            val newHistorySource = getAssessmentHistoryForHouseholds(householdIds)
            historySource = newHistorySource
            result.addSource(newHistorySource, ::attachAssessmentHistory)
        }

        return result
    }

    @Query(
        """
            UPDATE HouseHold
            SET
                disability_persons_count = (
                    SELECT COUNT(member.id)
                        FROM HouseholdMember AS member
                    WHERE member.household_id = HouseHold.id
                    AND member.disability = 'present'
                ),
                sync_status =:syncStatus,
                updated_at =:updatedAt
            WHERE id =:householdId
            AND disability_persons_count < (
                SELECT COUNT(member.id)
                        FROM HouseholdMember AS member
                WHERE member.household_id = Household.id
                AND member.disability = 'present'
            )
            """,
    )
    suspend fun updateDisabilityPersonsCountIfUnderCounted(
        householdId: Long,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    )

    @Query(
        """
        UPDATE Household
        SET
            no_of_people = (
                SELECT COUNT(member.id)
                FROM HouseholdMember AS member
                WHERE member.household_id = Household.id
            ),
            sync_status =:syncStatus,
            updated_at =:updatedAt
        WHERE id IN (
            SELECT Household.id
            FROM Household
            INNER JOIN HouseholdMember AS member ON member.household_id = household.id
            GROUP BY Household.id
            HAVING Household.no_of_people < COUNT(member.id)
        )
    """,
    )
    suspend fun updateUndercountedHouseholds(
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    ): Int

    @Query(
        """
        UPDATE Household
        SET
            disability_persons_count = (
                SELECT COUNT(member.id)
                FROM HouseholdMember AS member
                WHERE member.household_id = Household.id
                AND member.disability = 'present'
            ),
            sync_status =:syncStatus,
            updated_at =:updatedAt
        WHERE id IN (
            SELECT Household.id
            FROM Household
            INNER JOIN HouseholdMember AS member
                ON member.household_id = Household.id
                WHERE member.disability = 'present'
            GROUP BY Household.id
            HAVING Household.disability_persons_count < COUNT(member.id)
        )
    """,
    )
    suspend fun updateUndercountedDisabilityHouseholds(
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
        updatedAt: Long = System.currentTimeMillis(),
    ): Int

    @Query("SELECT name FROM Household WHERE id = :householdId")
    fun observeHouseholdHeadName(householdId: Long): LiveData<String?>
}
