package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.AssessmentEntity
import com.medtroniclabs.opensource.db.entity.SignsAndSymptomsEntity
import com.medtroniclabs.opensource.model.assessment.AssessmentDetails
import com.medtroniclabs.opensource.ui.assessment.AssessmentNCDEntity

@Dao
interface AssessmentDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssessment(memberEntity: AssessmentEntity): Long

    @Update
    suspend fun updateOtherAssessmentDetails(assessmentEntity: AssessmentEntity)

    @Query("SELECT * FROM Assessment WHERE memberId = :memberId ORDER BY id DESC LIMIT 1")
    suspend fun getLatestAssessmentForMember(memberId: Long): AssessmentEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSymptoms(symptomEntity: List<SignsAndSymptomsEntity>)

    @Query("SELECT * FROM SymptomEntity WHERE LOWER(type) = LOWER(:type) ORDER BY display_order")
    suspend fun getSymptomListByType(type: String): List<SignsAndSymptomsEntity>

    @Query("SELECT a.id, a.villageId, a.assessmentType, a.assessmentDetails, a.patientId, a.referralStatus, a.referredReason, a.otherDetails, a.memberId, a.householdId, a.isReferred, a.created_at AS createdAt, a.followUpId, a.latitude, a.longitude, pd.neonatePatientId as neonatePatientId, pd.neonateHouseholdMemberLocalId as neonatePatientReferenceId " +
                "FROM Assessment AS a LEFT JOIN PregnancyDetail AS pd ON a.householdMemberLocalId = pd.householdMemberLocalId " +
                "WHERE a.sync_status IN (:status) AND a.householdMemberLocalId =:hhmId")
    suspend fun getUnSyncedAssessmentByHHMId(
        hhmId: Long,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)
    ): List<AssessmentDetails>

    @Query("SELECT a.id, a.villageId, a.assessmentType, a.assessmentDetails, hhm.patient_id as patientId, a.referralStatus, a.referredReason, a.otherDetails, hhm.fhir_id as memberId, hh.fhir_id as householdId, a.isReferred, a.created_at AS createdAt, a.followUpId, a.latitude, a.longitude, pd.neonatePatientId as neonatePatientId, pd.neonateHouseholdMemberLocalId as neonatePatientReferenceId " +
                "FROM Assessment AS a LEFT JOIN PregnancyDetail AS pd ON a.householdMemberLocalId = pd.householdMemberLocalId INNER JOIN HouseholdMember AS hhm ON a.householdMemberLocalId = hhm.id INNER JOIN Household AS hh ON hhm.household_id = hh.id " +
                "WHERE a.id NOT IN (:addedAssessmentIds) AND hhm.fhir_id IS NOT NULL AND a.sync_status IN (:status)")
    suspend fun getOtherUnSyncedAssessments(
        addedAssessmentIds: List<String>,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)
    ): List<AssessmentDetails>

    @Query("SELECT COUNT(id) FROM Assessment where sync_status IN (:syncStatus)")
    suspend fun getUnSyncedCount(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): Int

    @Query("DELETE FROM Assessment")
    suspend fun deleteAllAssessments()

    @Query("UPDATE Assessment SET sync_status =:syncStatus, updated_at =:updatedAt WHERE id IN (:ids)")
    suspend fun updateInProgress(ids: List<String>, syncStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM SymptomEntity WHERE LOWER(type) = LOWER(:type) ORDER BY display_order")
    fun getSymptomListByTypeForNCD(type: String): LiveData<List<SignsAndSymptomsEntity>>

    @Query("SELECT * FROM SymptomEntity ORDER BY display_order")
    suspend fun getSymptomList(): List<SignsAndSymptomsEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAssessmentInformation(assessmentOfflineEntity: AssessmentNCDEntity): Long

    @Query("SELECT * FROM AssessmentNCDEntity WHERE id = :id")
    suspend fun getAssessmentById(id: Long): AssessmentNCDEntity

    @Query("SELECT * FROM AssessmentNCDEntity WHERE uploadStatus = :uploadStatus ORDER BY createdAt DESC")
    suspend fun getAllAssessmentRecords(uploadStatus: Boolean): List<AssessmentNCDEntity>

    @Query("DELETE FROM AssessmentNCDEntity WHERE uploadStatus = 1")
    suspend fun deleteAssessmentList()

    @Query("UPDATE AssessmentNCDEntity SET uploadStatus = :uploadStatus WHERE id = :id")
    suspend fun updateAssessmentUploadStatus(id: Long, uploadStatus: Boolean)

    @Query("SELECT COUNT(assessmentDetails) FROM AssessmentNCDEntity WHERE uploadStatus=0")
    fun getUnSyncedNCDAssessmentCount(): LiveData<Long>

}