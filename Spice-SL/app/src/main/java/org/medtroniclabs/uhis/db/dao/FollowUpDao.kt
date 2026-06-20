package org.medtroniclabs.uhis.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.medtroniclabs.uhis.data.FollowUpPatientModel
import org.medtroniclabs.uhis.data.offlinesync.utils.OfflineSyncStatus
import org.medtroniclabs.uhis.db.entity.FollowUp
import org.medtroniclabs.uhis.microcoaching.TodaysVisitRow

@Dao
interface FollowUpDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFollowUp(followUp: FollowUp): Long

    @Query("DELETE FROM FollowUp")
    suspend fun deleteAllFollowUps()

    @Query("SELECT * FROM FollowUp WHERE id = :id")
    suspend fun getFollowUpDetailsById(id: Long): FollowUp

    @Query(
        "SELECT fu.id, hhm.id AS localPatientId, hhm.name, fu.patientId, hhm.phone_number as phoneNumber, hhm.date_of_birth as dateOfBirth, hhm.gender, fu.reason, fu.patientStatus, ve.name AS village, hh.id as householdId, hh.name AS householdName, NULL as landmark, fu.type, fu.encounterType, fu.calledAt, fu.successfulAttempts, fu.unsuccessfulAttempts, fu.nextVisitDate, fu.encounterDate, fu.isWrongNumber, fu.updatedAt " +
            "FROM FollowUp AS fu INNER JOIN HouseholdMember AS hhm ON fu.memberId = hhm.fhir_id LEFT JOIN Household AS hh ON hhm.household_id = hh.id LEFT JOIN SubVillageEntity AS ve ON fu.villageId = ve.id " +
            "WHERE fu.isCompleted = 0 AND hhm.isActive = 1 AND fu.id IS NOT NULL AND fu.villageId IN (:villageIds) AND " +
            "fu.type=:type AND " +
            "(hhm.name LIKE '%' || :search || '%' OR fu.patientId LIKE :search || '%' OR :search IS NULL) AND " +
            "CASE WHEN :fromDate = '' THEN 1 ELSE date(fu.encounterDate) BETWEEN :fromDate AND :toDate END ORDER BY fu.encounterDate",
    )
    fun getReferredFollowUpPatientListLiveData(
        type: String,
        search: String? = null,
        villageIds: List<Long> = listOf(),
        fromDate: String = "",
        toDate: String = "",
    ): LiveData<List<FollowUpPatientModel>>

    @Query(
        "SELECT fu.id, hhm.id AS localPatientId, hhm.name, fu.patientId, hhm.phone_number as phoneNumber, hhm.date_of_birth as dateOfBirth, hhm.gender, fu.reason, fu.patientStatus, ve.name AS village, hh.id as householdId, hh.name AS householdName, NULL as landmark, fu.type, fu.encounterType, fu.calledAt, fu.successfulAttempts, fu.unsuccessfulAttempts, fu.nextVisitDate, fu.encounterDate, fu.isWrongNumber, fu.updatedAt " +
            "FROM FollowUp AS fu INNER JOIN HouseholdMember AS hhm ON fu.memberId = hhm.fhir_id LEFT JOIN Household AS hh ON hhm.household_id = hh.id LEFT JOIN SubVillageEntity AS ve ON fu.villageId = ve.id " +
            "WHERE fu.isCompleted = 0 AND hhm.isActive = 1 AND fu.id IS NOT NULL AND fu.villageId IN (:villageIds) AND " +
            "fu.type=:type AND " +
            "(hhm.name LIKE '%' || :search || '%' OR fu.patientId LIKE :search || '%' OR :search IS NULL) AND " +
            "CASE WHEN :fromDate = '' THEN 1 ELSE date(fu.nextVisitDate) BETWEEN :fromDate AND :toDate END ORDER BY fu.nextVisitDate",
    )
    fun getOtherFollowUpPatientListLiveData(
        type: String,
        search: String? = null,
        villageIds: List<Long> = listOf(),
        fromDate: String = "",
        toDate: String = "",
    ): LiveData<List<FollowUpPatientModel>>

    @Query("SELECT * FROM FollowUp WHERE syncStatus IN (:syncStatus)")
    suspend fun getAllFollowUps(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): List<FollowUp>

    /**
     * Minimal, PII-free projection of follow-ups due on [today] (`yyyy-MM-dd`)
     * that aren't completed — the MicroCoaching SDK's today's-visit refresher
     * source. The local DB only holds this CHW's villages' follow-ups, so no CHW/
     * village filter is needed; `date(nextVisitDate)` mirrors the existing
     * follow-up "today" filters above.
     */
    @Query(
        "SELECT type, encounterType, nextVisitDate, villageId FROM FollowUp " +
            "WHERE isCompleted = 0 AND nextVisitDate IS NOT NULL AND date(nextVisitDate) = :today",
    )
    suspend fun getVisitsDueOn(today: String): List<TodaysVisitRow>

    @Query("SELECT COUNT(referenceId) FROM FollowUp where syncStatus IN (:syncStatus)")
    suspend fun getUnSyncedCount(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): Int

    @Query(
        "UPDATE FollowUp SET isCompleted = 1, updatedAt = :updateAt WHERE memberId = :fhirId AND id != :id AND type = :type AND " +
            "CASE WHEN :type = 'HH_VISIT' THEN (encounterType = :encounterType AND reason= :reason) ELSE encounterType= :encounterType END",
    )
    suspend fun updateOtherDuplicateTickets(
        id: Long,
        fhirId: String,
        type: String,
        encounterType: String? = null,
        reason: String? = null,
        updateAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE FollowUp SET updatedAt = :updatedAt, calledAt = :updatedAt, syncStatus = :syncStatus WHERE memberId = :fhirId AND id != :id AND type = :type AND " +
            "CASE WHEN :type = 'HH_VISIT' THEN (encounterType = :encounterType AND reason= :reason) ELSE encounterType= :encounterType END",
    )
    suspend fun updateOnTreatmentStatus(
        id: Long,
        fhirId: String,
        type: String,
        updatedAt: Long,
        encounterType: String? = null,
        reason: String? = null,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
    )

    @Query("UPDATE FollowUp SET isWrongNumber = 1, syncStatus = :syncStatus, isCompleted = CASE WHEN type = 'HH_VISIT' THEN 0 ELSE 1 END WHERE memberId = :fhirId AND isWrongNumber = 0 AND id != :id ")
    suspend fun updateOtherFollowUpForWrongNumber(
        id: Long,
        fhirId: String,
        syncStatus: String = OfflineSyncStatus.NotSynced.name,
    )

    @Query("UPDATE FollowUp SET syncStatus =:syncStatus WHERE referenceId IN (:ids)")
    suspend fun updateInProgress(
        ids: List<Long>,
        syncStatus: String,
    )

    @Transaction
    suspend fun insertOrUpdateFromBE(entity: FollowUp) {
        val existingEntity = entity.id?.let { getFollowUpDetailsById(it) }
        if (existingEntity?.syncStatus != OfflineSyncStatus.NotSynced) {
            val entityToInsert = existingEntity?.let { entity.copy(referenceId = it.referenceId) } ?: entity
            entityToInsert.updatedAt = entity.calledAt ?: 0
            entityToInsert.syncStatus = OfflineSyncStatus.Success
            insertFollowUp(entityToInsert)
        }
    }

    @Query("DELETE FROM FollowUp WHERE isCompleted = 1 AND syncStatus = :syncStatus")
    suspend fun deleteCompletedFollowUp(syncStatus: String = OfflineSyncStatus.Success.name)

    @Query("UPDATE FollowUp SET isCompleted = 1, updatedAt = :updateAt WHERE memberId = :fhirId AND id != :id AND type = :type AND encounterType = :encounterType AND reason= :reason")
    suspend fun updateHHVisitTicketsOnRecovered(
        id: Long,
        fhirId: String,
        type: String,
        encounterType: String? = null,
        reason: String? = null,
        updateAt: Long = System.currentTimeMillis(),
    )

    @Query("UPDATE FollowUp SET isCompleted = 1, updatedAt = :updateAt WHERE memberId = :fhirId AND id != :id AND encounterType = :encounterType AND type IN (:types)")
    suspend fun closeTicketsForRMNCH(
        id: Long,
        fhirId: String,
        types: List<String>,
        encounterType: String? = null,
        updateAt: Long = System.currentTimeMillis(),
    )

    @Query(
        "UPDATE FollowUp SET isCompleted = 1, updatedAt = :updateAt WHERE memberId = :fhirId AND id != :id AND encounterType = :encounterType AND type IN (:types) AND " +
            "CASE WHEN :type = 'MEDICAL_REVIEW' THEN (encounterType != 'RMNCH' ) ELSE 1 END",
    )
    suspend fun closeTicketsForNonRMNCH(
        id: Long,
        fhirId: String,
        type: String,
        types: List<String>,
        encounterType: String? = null,
        updateAt: Long = System.currentTimeMillis(),
    )
}
