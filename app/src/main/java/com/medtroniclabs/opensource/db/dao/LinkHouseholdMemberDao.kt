package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.Assigned
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.LinkHouseholdMember

@Dao
interface LinkHouseholdMemberDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkHHM: LinkHouseholdMember): Long

    @Query("DELETE FROM LinkHouseholdMember WHERE memberId IN (:memberIds)")
    suspend fun delete(memberIds: List<String>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(linkHHM: List<LinkHouseholdMember>)

    @Query("DELETE FROM LinkHouseholdMember")
    suspend fun deleteAllLinkHouseholdMember()

    @Query("SELECT l.memberId,hhm.id as lMemberId, hhm.name, hhm.phone_number as phoneNumber, hhm.date_of_birth as dateOfBirth, hhm.gender, hhm.villageId as villageId, v.name as villageName FROM HouseholdMember as hhm JOIN LinkHouseholdMember as l ON hhm.fhir_id = l.memberId JOIN VillageEntity as v ON v.id = hhm.villageId WHERE l.status = :status AND hhm.isActive IS 1")
    fun getUnAssignedHouseholdMembersLiveData(status: String = DefinedParams.UnAssigned): LiveData<List<UnAssignedHouseholdMemberDetail>>

    @Query("UPDATE LinkHouseholdMember SET syncStatus =:syncStatus WHERE memberId IN (:ids)")
    suspend fun updateInProgress(ids: List<String>, syncStatus: String)

    @Query("UPDATE LinkHouseholdMember SET status = :status, syncStatus = :syncStatus WHERE memberId = :memberId")
    suspend fun updateMemberAsAssigned(memberId: String, status: String = Assigned, syncStatus: OfflineSyncStatus = OfflineSyncStatus.NotSynced)


    @Query("SELECT fhir_id FROM HouseholdMember AS hhm INNER JOIN LinkHouseholdMember AS lhhm ON hhm.fhir_id = lhhm.memberId WHERE hhm.patient_id = :patientId AND lhhm.status = :status")
    suspend fun getUnAssignedParentFhirId(patientId: String, status: String = DefinedParams.UnAssigned): List<String>

    @Query("SELECT fhir_id FROM HouseholdMember AS hhm INNER JOIN LinkHouseholdMember AS lhhm ON hhm.fhir_id = lhhm.memberId where hhm.parentId = :parentId AND lhhm.status = :status")
    suspend fun getUnAssignedChildFhirIds(parentId: String, status: String = DefinedParams.UnAssigned): List<String>

    @Query("UPDATE LinkHouseholdMember SET status = :status, syncStatus = :syncStatus WHERE memberId in (:memberIds)")
    suspend fun updateMembersAsAssigned(memberIds: List<String>, status: String = Assigned, syncStatus: OfflineSyncStatus = OfflineSyncStatus.NotSynced)
}