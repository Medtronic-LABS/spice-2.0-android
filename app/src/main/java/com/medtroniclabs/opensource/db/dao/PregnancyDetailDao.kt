package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.medtroniclabs.opensource.db.entity.MemberClinicalEntity
import com.medtroniclabs.opensource.db.entity.PregnancyDetail

@Dao
interface PregnancyDetailDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePregnancyDetail(details: PregnancyDetail): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregnancyDetails(list: List<PregnancyDetail>)

    @Query("UPDATE PregnancyDetail SET ancVisitNo = :visitCount, lastMenstrualPeriod = :clinicalDate WHERE householdMemberLocalId = :hhmLocalId")
    suspend fun updatePregnancyAnc(visitCount: Long, clinicalDate: String?, hhmLocalId: Long )

    @Query("SELECT patientId, ancVisitNo as visitCount, lastMenstrualPeriod as clinicalDate FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId Limit 1")
    suspend fun getAncDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT patientId, pncVisitNo as visitCount, dateOfDelivery as clinicalDate, noOfNeonates as numberOfNeonate, isDeliveryAtHome FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId Limit 1")
    suspend fun getPncDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT patientId, childVisitNo as visitCount FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId Limit 1")
    suspend fun getChildhoodVisitDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT * FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId Limit 1")
    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail?

    @Query("SELECT id from HouseholdMember where fhir_id =:memberId")
    suspend fun getHHMLocalID(memberId: String): Long

    @Query("SELECT id from HouseholdMember where patient_id =:patientId")
    suspend fun getHHMLocalIDByPatientId(patientId: String): Long

    @Query("UPDATE PregnancyDetail SET neonateHouseholdMemberLocalId = :neonateId WHERE householdMemberLocalId = :hhmLocalId")
    suspend fun updateNeonatePatientId(hhmLocalId: Long, neonateId: Long)

    @Transaction
    suspend fun insertOrUpdateFromBE(entity: PregnancyDetail): Long {
        val hhmLocalId = getHHMLocalID(entity.householdMemberId!!)
        val neonateLocalId = entity.neonatePatientId?.let { getHHMLocalIDByPatientId(it) }
        val existingEntity = getPregnancyDetailByPatientId(hhmLocalId)
        val entityToInsert = existingEntity?.let { entity.copy(id = it.id) } ?: entity
        entityToInsert.householdMemberLocalId = hhmLocalId
        entityToInsert.neonateHouseholdMemberLocalId = neonateLocalId

        return savePregnancyDetail(entityToInsert)
    }

    @Query("DELETE from PregnancyDetail")
    suspend fun deleteAllPregnancyDetails()

    /*###################################################################*/






    @Query("SELECT neonateHouseholdMemberLocalId FROM PregnancyDetail where householdMemberLocalId =:parentId")
    suspend fun getChildPatientId(parentId: Long): Long?


}