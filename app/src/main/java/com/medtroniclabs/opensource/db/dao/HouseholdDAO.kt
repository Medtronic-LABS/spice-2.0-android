package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Transaction
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import com.medtroniclabs.opensource.data.model.HouseholdCardDetail
import com.medtroniclabs.opensource.db.entity.HouseholdEntity
import com.medtroniclabs.opensource.db.response.HouseHoldEntityWithMemberCount
import com.medtroniclabs.opensource.db.response.HouseholdMemberCount
import com.medtroniclabs.opensource.data.offlinesync.model.HouseHold
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus

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
            return insertHouseHold(entityToInsert)
        } else {
            return existingEntity.id
        }
    }


    @Query("SELECT MAX(household_no) FROM household WHERE village_id = :villageId")
    suspend fun getLastHouseholdNo(villageId: Long): Long?

    @Query("SELECT * FROM (SELECT hh.*, ve.name as village_name, COUNT(hhm.household_id) AS member_count, case when :status == '' then '' when COUNT(hhm.household_id) == hh.no_of_people then 'Finished' " +
            " when COUNT(hhm.household_id) != hh.no_of_people then 'Pending' else '' end as status FROM Household AS hh LEFT JOIN HouseholdMember AS hhm ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id WHERE (hh.name LIKE '%' || :searchTerm || '%' OR hh.household_no LIKE :searchTerm OR hh.head_phone_number LIKE :searchTerm) GROUP BY hh.id) as subTable Where status=:status")
    fun getHouseholdsWithFilterLiveData(
        searchTerm: String, status: String): LiveData<List<HouseHoldEntityWithMemberCount>>

    @Query("SELECT * FROM (SELECT hh.*, ve.name as village_name, COUNT(hhm.household_id) AS member_count, case when :status == '' then '' when COUNT(hhm.household_id) == hh.no_of_people then 'Finished' " +
            " when COUNT(hhm.household_id) != hh.no_of_people then 'Pending' else '' end as status FROM Household AS hh LEFT JOIN HouseholdMember AS hhm ON hh.id = hhm.household_id INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id WHERE hh.village_id IN (:ids) AND (hh.name LIKE '%' || :searchTerm || '%' OR hh.household_no LIKE :searchTerm OR hh.head_phone_number LIKE :searchTerm) GROUP BY hh.id) as subTable Where status=:status")
    fun getHouseholdsWithFilterLiveData(
        searchTerm: String,
        status: String,
            ids: List<Long>
    ): LiveData<List<HouseHoldEntityWithMemberCount>>

    @Query("SELECT * FROM HouseHold WHERE id= :houseHoldId")
    suspend fun getHouseHoldDetailsById(houseHoldId: Long): HouseholdEntity

    @Query("SELECT hh.no_of_people as noOfPeople, count(hhm.id) AS memberCount FROM HouseHold AS hh INNER JOIN HouseHoldMember AS hhm ON hhm.household_id = hh.id WHERE household_id =:householdId")
    fun getHouseholdMemberCountLiveData(householdId: Long): LiveData<HouseholdMemberCount>

    @Query("UPDATE HouseHold SET no_of_people =:newNoOfPeople, sync_status =:syncStatus, updated_at =:updatedAt WHERE id =:householdId")
    suspend fun updateHeadCount(householdId: Long, newNoOfPeople: Int, syncStatus: String = OfflineSyncStatus.NotSynced.name, updatedAt: Long = System.currentTimeMillis())
    @Query("SELECT hh.*, ve.name as villageName FROM HouseHold as hh INNER JOIN VillageEntity AS ve ON hh.village_id = ve.id WHERE hh.sync_status IN (:status)")
    suspend fun getAllUnSyncedHouseHolds(status: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): List<HouseHold>
    @RawQuery
    suspend fun updateFhirId(query: SimpleSQLiteQuery) : Long

    @Query("SELECT COUNT(id) FROM Household WHERE sync_status IN (:syncStatus)")
    suspend fun getUnSyncedCount(syncStatus: List<String> = listOf(OfflineSyncStatus.NotSynced.name, OfflineSyncStatus.NetworkError.name)): Int

    @Query("SELECT id FROM Household WHERE fhir_id =:fhirId")
    suspend fun getHouseholdIdByFhirId(fhirId: String): Long?

    @Query("DELETE FROM Household")
    suspend fun deleteAllHouseholds()

    @Query("SELECT hh.id, hh.name, hh.household_no AS householdNo, hh.landmark, ve.name AS villageName, hh.head_phone_number AS householdHeadPhoneNumber, hh.no_of_people AS memberRegistered, COUNT(hhm.id) AS memberAdded " +
            "FROM Household as hh INNER JOIN VillageEntity as ve ON hh.village_id = ve.id LEFT JOIN HouseholdMember as hhm ON hhm.household_id = hh.id  WHERE hh.id =:id")
    fun getHouseholdCardDetailLiveData(id: Long): LiveData<HouseholdCardDetail>


    @Query("UPDATE HouseHold SET sync_status =:syncStatus, updated_at =:updatedAt WHERE id IN (:householdIds)")
    suspend fun updateInProgress(householdIds: List<String>, syncStatus: String, updatedAt: Long = System.currentTimeMillis())

    @Query("UPDATE Household SET head_phone_number = :phoneNumber, head_phone_number_category = :phoneNumberCategory,  sync_status =:syncStatus, updated_at =:updatedAt WHERE id = :id")
    fun updateHeadPhoneNumber(id: Long, phoneNumber: String, phoneNumberCategory: String, syncStatus: String = OfflineSyncStatus.NotSynced.name, updatedAt: Long = System.currentTimeMillis())
   }