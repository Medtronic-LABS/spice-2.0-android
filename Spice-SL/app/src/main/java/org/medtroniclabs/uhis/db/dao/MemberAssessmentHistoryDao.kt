package org.medtroniclabs.uhis.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import org.medtroniclabs.uhis.db.entity.MemberAssessmentHistoryEntity
import org.medtroniclabs.uhis.db.response.DashboardCountsRow
import org.medtroniclabs.uhis.db.response.MaternalDashboardCountsRow

@Dao
interface MemberAssessmentHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberAssessmentHistory(historyList: List<MemberAssessmentHistoryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemberAssessmentHistory(history: MemberAssessmentHistoryEntity): Long

    @Update(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateAssessmentHistory(assessmentHistory: MemberAssessmentHistoryEntity)

    @Query("SELECT * FROM memberassessmenthistory WHERE (memberFhirId = :memberFhirId OR memberId = :memberId) AND visitDate = :visitDate AND serviceProvided = :serviceProvided LIMIT 1")
    suspend fun getAssessmentHistory(
        memberFhirId: String?,
        memberId: Long?,
        visitDate: String?,
        serviceProvided: String?,
    ): MemberAssessmentHistoryEntity?

    @Query("DELETE FROM memberassessmenthistory")
    suspend fun deleteMemberAssessmentHistory()

    @Query(
        """
        SELECT * FROM memberassessmenthistory
        WHERE memberId = :memberLocalId AND LOWER(serviceProvided) = :serviceTypeFor
        ORDER BY visitDate DESC, id DESC
        LIMIT 1
        """,
    )
    suspend fun getLatestMemberAssessmentHistoryByMemberLocalIdAndType(
        memberLocalId: Long,
        serviceTypeFor: String,
    ): MemberAssessmentHistoryEntity?

    @Query(
        """
        SELECT
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('screened') THEN 1 ELSE 0 END) AS screened,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('referred','referral') THEN 1 ELSE 0 END) AS referred,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('registration','pwprofile') THEN 1 ELSE 0 END) AS registered,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('assessment','generalassessment','followupassessment','anc','pncmother','pncchild','childvisit','tb') THEN 1 ELSE 0 END) AS assessed,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('dispensed','pharmacydispense') THEN 1 ELSE 0 END) AS dispensed,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('investigation','labinvestigation','labtest') THEN 1 ELSE 0 END) AS investigated,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('lifestylereview','nutritionistlifestyle','lifestylecounselling') THEN 1 ELSE 0 END) AS nutritionistLifestyleCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('psychologicalcounselling','psychologicalnotes') THEN 1 ELSE 0 END) AS psychologicalNotesCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('family_planning','familyplanning') THEN 1 ELSE 0 END) AS familyPlanningCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('pwprofile') THEN 1 ELSE 0 END) AS pregnantWomenRegistrationCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('pregnancyoutcome') THEN 1 ELSE 0 END) AS pregnancyOutcomeCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('anc') THEN 1 ELSE 0 END) AS ancCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('pnc_mother') THEN 1 ELSE 0 END) AS pncCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('childhood_visit') THEN 1 ELSE 0 END) AS childVisitCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('tb') THEN 1 ELSE 0 END) AS tbAssessmentCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('tbcontacttracing') THEN 1 ELSE 0 END) AS tbContactTracingCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('eye_care') THEN 1 ELSE 0 END) AS eyeCareCount,
            SUM(CASE WHEN LOWER(h.serviceProvided) IN ('cataract') THEN 1 ELSE 0 END) AS cataractCount,
            0 AS householdRegisteredCount,
            0 AS pwIdentifiedFirst4MonthsWithAncCount,
            0 AS anc3PlusCount,
            SUM(
                CASE
                    WHEN LOWER(h.serviceProvided) IN ('anc')
                        AND h.customStatus IS NOT NULL
                        AND INSTR(h.customStatus, 'HIGH_RISK_PW') > 0
                    THEN 1
                    ELSE 0
                END
            ) AS highRiskPregnantWomenCount
        FROM memberassessmenthistory AS h
        LEFT JOIN householdmember AS hm ON hm.id = h.memberId
        LEFT JOIN household AS hh ON hh.id = hm.household_id
        WHERE (:startDate IS NULL OR date(datetime(h.visitDate, 'localtime')) >= :startDate)
        AND (:endDate IS NULL OR date(datetime(h.visitDate, 'localtime')) <= :endDate)
        AND (
            (:ssIdsSize = 0 AND :subVillageIdsSize = 0)
            OR (:subVillageIdsSize > 0 AND COALESCE(hm.sub_village_id, hh.sub_village_id) IN (:subVillageIds))
            OR (
                :ssIdsSize > 0
                AND COALESCE(hm.sub_village_id, hh.sub_village_id) IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN (:ssIds)
                )
            )
        )
        """,
    )
    suspend fun getDashboardCounts(
        startDate: String?,
        endDate: String?,
        ssIds: List<Long>,
        ssIdsSize: Int,
        subVillageIds: List<Long>,
        subVillageIdsSize: Int,
    ): DashboardCountsRow?

    @Query(
        """
        WITH latest_pregnancy AS (
            SELECT pd.*
            FROM PregnancyDetail AS pd
            INNER JOIN (
                SELECT householdMemberLocalId, MAX(id) AS maxId
                FROM PregnancyDetail
                GROUP BY householdMemberLocalId
            ) AS latest
                ON latest.householdMemberLocalId = pd.householdMemberLocalId
               AND latest.maxId = pd.id
        ),
        filtered_members AS (
            SELECT
                hm.id AS memberId,
                COALESCE(hm.shasthya_shebika_id, hh.shasthya_shebika_id) AS ssId,
                COALESCE(hm.sub_village_id, hh.sub_village_id) AS subVillageId
            FROM HouseholdMember AS hm
            LEFT JOIN Household AS hh ON hh.id = hm.household_id
        )
        SELECT
            SUM(
                CASE
                    WHEN EXISTS (
                        SELECT 1
                        FROM MemberAssessmentHistory AS h
                        WHERE h.memberId = lp.householdMemberLocalId
                          AND LOWER(h.serviceProvided) = 'anc'
                          AND (:startDate IS NULL OR date(datetime(h.visitDate, 'localtime')) >= :startDate)
                          AND (:endDate IS NULL OR date(datetime(h.visitDate, 'localtime')) <= :endDate)
                          AND date(datetime(h.visitDate, 'localtime')) >= substr(lp.lastMenstrualPeriod, 1, 10)
                          AND date(datetime(h.visitDate, 'localtime')) <= date(substr(lp.lastMenstrualPeriod, 1, 10), '+4 months')
                    )
                    THEN 1 ELSE 0
                END
            ) AS pwIdentifiedFirst4MonthsWithAncCount,
            SUM(
                CASE
                    WHEN (
                        SELECT COUNT(1)
                        FROM MemberAssessmentHistory AS h
                        WHERE h.memberId = lp.householdMemberLocalId
                          AND LOWER(h.serviceProvided) = 'anc'
                          AND (:startDate IS NULL OR date(datetime(h.visitDate, 'localtime')) >= :startDate)
                          AND (:endDate IS NULL OR date(datetime(h.visitDate, 'localtime')) <= :endDate)
                    ) >= 3
                    THEN 1 ELSE 0
                END
            ) AS anc3PlusCount,
            0 AS highRiskPregnantWomenCount
        FROM latest_pregnancy AS lp
        INNER JOIN filtered_members AS fm ON fm.memberId = lp.householdMemberLocalId
        WHERE (lp.dateOfDelivery IS NULL OR lp.dateOfDelivery = '')
        AND (lp.lastMenstrualPeriod IS NOT NULL AND lp.lastMenstrualPeriod != '')
        AND (lp.estimatedDeliveryDate IS NULL OR substr(lp.estimatedDeliveryDate, 1, 10) >= date('now', '-45 days'))
        AND (
            (:ssIdsSize = 0 AND :subVillageIdsSize = 0)
            OR (:subVillageIdsSize > 0 AND fm.subVillageId IN (:subVillageIds))
            OR ( :ssIdsSize > 0
                AND fm.subVillageId IN (
                    SELECT DISTINCT sslv.subVillageId
                    FROM ShasthyaShebikaLinkedVillageEntity AS sslv
                    WHERE sslv.shasthyaShebikaId IN (:ssIds)
                )
            )
        )
        """,
    )
    suspend fun getMaternalDashboardCounts(
        startDate: String?,
        endDate: String?,
        ssIds: List<Long>,
        ssIdsSize: Int,
        subVillageIds: List<Long>,
        subVillageIdsSize: Int,
    ): MaternalDashboardCountsRow?

    /**
     * Method updates and sets visit date as visit date - [noOfDays] for given member [memberId]
     */
    @Query(
        """
            UPDATE memberassessmenthistory
                SET visitDate = strftime(
                    '%Y-%m-%dT%H:%M:%S+00:00',
                    datetime(
                        visitDate,
                        '-' || :noOfDays || ' day'
                    )
                )
            WHERE memberId = :memberId
        """,
    )
    suspend fun decrementDateByDays(
        memberId: Long,
        noOfDays: Int,
    )
}
