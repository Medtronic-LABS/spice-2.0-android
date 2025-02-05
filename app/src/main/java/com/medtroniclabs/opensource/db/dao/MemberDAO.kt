package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.data.offlinesync.model.HHSignatureDetail
import com.medtroniclabs.opensource.db.entity.HouseholdMemberEntity
import com.medtroniclabs.opensource.model.MemberDobGenderModel
import com.medtroniclabs.opensource.model.assessment.AssessmentMemberDetails
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHoldMember
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus

@Dao
interface MemberDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(memberEntity: HouseholdMemberEntity): Long

    @Query("SELECT * FROM HouseHoldMember WHERE household_id = :houseHoldId")
    suspend fun getAllHouseHoldMemberList(houseHoldId: Long): List<HouseholdMemberEntity>

    @Query("SELECT * FROM HouseHoldMember WHERE household_id = :houseHoldId")
    fun getAllHouseHoldMembersLiveData(houseHoldId: Long): LiveData<List<HouseholdMemberEntity>>

    @Query("SELECT * FROM HouseHoldMember WHERE household_id = :houseHoldId AND isActive =:aliveStatus")
    fun getAliveHouseHoldMembers(houseHoldId: Long,aliveStatus: Boolean): List<HouseholdMemberEntity>

    @Query("SELECT * FROM HouseHoldMember WHERE id = :memberId")
    suspend fun getMemberDetailsById(memberId: Long): HouseholdMemberEntity

    @Query("SELECT * FROM HouseHoldMember WHERE patient_id = :patientId")
    suspend fun getMemberDetailsByPatientId(patientId: String): HouseholdMemberEntity?

    @Query("SELECT * FROM HouseHoldMember WHERE parentId = :memberId ORDER BY fhir_id IS NULL, fhir_id ASC")
    suspend fun getMemberDetailsByParentId(memberId: String): List<HouseholdMemberEntity>

    @Query("SELECT id, fhir_id AS fhirId, localSignatureFile AS signatureName FROM HouseHoldMember WHERE localSignatureFile IS NOT NULL AND fhir_id IS NOT NULL")
    suspend fun getHHSignatureDetails(): List<HHSignatureDetail>

    @Query("SELECT COUNT(household_id) FROM HouseHoldMember WHERE household_id = :householdId")
    suspend fun getMemberCountPerHouseHold(householdId: Long): Int

    @Query("SELECT patient_id FROM HouseholdMember WHERE patient_id LIKE :patientIdStarts ORDER BY patient_id DESC LIMIT 1")
    suspend fun getLastPatientId(patientIdStarts: String): String?

    @Query("SELECT date_of_birth,gender FROM HouseHoldMember WHERE id = :memberId")
    suspend fun getDobAndGenderById(memberId: Long): MemberDobGenderModel

    @Query("SELECT hhm.*, hh.fhir_id as household_fhir_id, hh.village_id as village_id, ve.name as village_name, CASE WHEN lhhm.memberId IS NOT NULL AND lhhm.syncStatus IN (:status) THEN 1 ELSE NULL END AS assignHousehold FROM HouseHoldMember AS hhm INNER JOIN Household as hh ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id LEFT JOIN LinkHouseholdMember AS lhhm ON lhhm.memberId = hhm.fhir_id WHERE (hh.fhir_id IS NULL OR hhm.fhir_id IS NULL) AND hhm.household_id = :houseHoldId AND hhm.sync_status IN (:status)")
    suspend fun getAllUnSyncedHouseHoldMembers(
        houseHoldId: Long,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)
    ): List<HouseHoldMember>

    @Query("SELECT hhm.*, hh.fhir_id as household_fhir_id, hh.village_id as village_id, ve.name as village_name, CASE WHEN lhhm.memberId IS NOT NULL AND lhhm.syncStatus IN (:status) THEN 1 ELSE NULL END AS assignHousehold FROM HouseHoldMember AS hhm INNER JOIN Household as hh ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id LEFT JOIN LinkHouseholdMember AS lhhm ON lhhm.memberId = hhm.fhir_id WHERE hhm.id NOT IN (:memberIds) AND hh.fhir_id IS NOT NULL AND hhm.sync_status IN (:status)")
    suspend fun getOtherHouseholdMembers(
        memberIds: List<String>,
        status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)
    ): List<HouseHoldMember>

    @Query("SELECT COUNT(id) FROM HouseholdMember where sync_status IN (:syncStatus)")
    suspend fun getUnSyncedCount(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): Int

    @Query("SELECT id FROM HouseholdMember WHERE fhir_id =:fhirId")
    suspend fun getHouseholdMemberIdByFhirId(fhirId: String): Long?

    @Query("SELECT hhm.name, hhm.gender, hhm.date_of_birth AS dateOfBirth, hhm.patient_id AS patientId, hh.village_id as villageId, hhm.fhir_id AS memberId, hh.fhir_id AS householdId, hhm.id AS id,hhm.household_id AS householdLocalId  FROM HouseholdMember AS hhm INNER JOIN Household AS hh ON hh.id = hhm.household_id WHERE hhm.id=:id")
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
        val existingEntity = entity.fhirId?.let { getByUniqueField(it) }
        if (existingEntity?.sync_status != OfflineSyncStatus.NotSynced) {
            val entityToInsert = existingEntity?.let { entity.copy(id = it.id) } ?: entity
            entityToInsert.sync_status = existingEntity?.sync_status ?: OfflineSyncStatus.Success
            entityToInsert.fhirId = entity.fhirId
            return insertMember(entityToInsert)
        } else {
            return existingEntity.id
        }
    }

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus, updated_at =:updatedAt WHERE id IN (:memberIds)")
    suspend fun updateInProgress(memberIds: List<String>, syncStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE HouseholdMember SET sync_status =:syncStatus WHERE id = :id")
    suspend fun changeMemberDetailsToNotSynced(id: Long, syncStatus: OfflineSyncStatus = OfflineSyncStatus.NotSynced)

    @Query("UPDATE HouseholdMember SET isActive = :status, sync_status =:syncStatus  WHERE id = :id")
    suspend fun updateMemberDeceasedStatus(
        id: Long,
        status: Boolean,
        syncStatus: OfflineSyncStatus
    )
    @Query("UPDATE householdmember SET phone_number = :phoneNumber, phone_number_category = :phoneNumberCategory, sync_status =:syncStatus, updated_at =:updatedAt  WHERE household_id = :householdId AND household_head_relationship = :houseHoldRelationShip")
    suspend fun updatePhoneNumberForHouseholdHead(householdId: Long,phoneNumber: String?, phoneNumberCategory: String?, syncStatus: String = OfflineSyncStatus.NotSynced.name, updatedAt: Long = System.currentTimeMillis(),houseHoldRelationShip:String= DefinedParams.HosueHoldHead)


    @Query("UPDATE HouseholdMember SET household_id = :householdId, household_head_relationship = :defaultRelation, sync_status =:syncStatus, updated_at =:updatedAt  WHERE fhir_id IN (:memberIds)")
    suspend fun updateHouseholdHeadAndRelationShip(memberIds: List<String>, householdId: Long, defaultRelation: String = "",syncStatus: String = OfflineSyncStatus.NotSynced.name, updatedAt: Long = System.currentTimeMillis())

}