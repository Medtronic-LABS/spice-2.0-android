package org.medtroniclabs.uhis.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import org.medtroniclabs.uhis.db.entity.MemberClinicalEntity
import org.medtroniclabs.uhis.db.entity.PregnancyDetail

@Dao
interface PregnancyDetailDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun savePregnancyDetail(details: PregnancyDetail): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPregnancyDetails(list: List<PregnancyDetail>)

    @Query("DELETE FROM PregnancyDetail WHERE householdMemberLocalId = :hhmLocalId")
    suspend fun updatePregnancyAnc(hhmLocalId: Long)

    @Query("SELECT patientId, ancVisitNo as visitCount, lastMenstrualPeriod as clinicalDate FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId ORDER BY CASE WHEN ancVisitNo IS NULL THEN 0 ELSE 1 END DESC, ancVisitNo DESC, id DESC Limit 1")
    suspend fun getAncDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT pd.patientId, pd.pncVisitNo as visitCount, pd.dateOfDelivery as clinicalDate, pd.noOfNeonates as numberOfNeonate, pd.isDeliveryAtHome, pd.neonateHouseholdMemberLocalId, hhm.isActive as isNeonateAlive, pd.isNeonateDeathRecordedByPHU FROM PregnancyDetail AS pd LEFT JOIN HouseholdMember AS hhm ON pd.neonateHouseholdMemberLocalId = hhm.id WHERE pd.householdMemberLocalId = :hhmLocalId ORDER BY CASE WHEN pd.pncVisitNo IS NULL THEN 0 ELSE 1 END DESC, pd.pncVisitNo DESC, pd.id DESC Limit 1")
    suspend fun getPncDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT patientId, childVisitNo as visitCount FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId ORDER BY CASE WHEN childVisitNo IS NULL THEN 0 ELSE 1 END DESC, childVisitNo DESC, id DESC Limit 1")
    suspend fun getChildhoodVisitDetail(hhmLocalId: Long): MemberClinicalEntity?

    @Query("SELECT * FROM PregnancyDetail WHERE householdMemberLocalId=:hhmLocalId ORDER BY endAt DESC, id DESC Limit 1")
    suspend fun getPregnancyDetailByPatientId(hhmLocalId: Long): PregnancyDetail?

    @Query("SELECT * FROM PregnancyDetail WHERE householdMemberLocalId = :hhmLocalId ORDER BY endAt DESC, id DESC LIMIT 1")
    fun observeRecentPregnancyDetail(hhmLocalId: Long): LiveData<PregnancyDetail?>

    @Query("SELECT id from HouseholdMember where fhir_id =:memberId")
    suspend fun getHHMLocalID(memberId: String): Long?

    @Query("SELECT id from HouseholdMember where patient_id =:patientId")
    suspend fun getHHMLocalIDByPatientId(patientId: String): Long

    @Query("UPDATE PregnancyDetail SET neonateHouseholdMemberLocalId = :neonateId WHERE householdMemberLocalId = :hhmLocalId")
    suspend fun updateNeonatePatientId(
        hhmLocalId: Long,
        neonateId: Long,
    )

    @Transaction
    suspend fun insertOrUpdateFromBE(entity: PregnancyDetail) {
        val hhmLocalId = getHHMLocalID(entity.householdMemberId!!)
        hhmLocalId?.let {
            val neonateLocalId = entity.neonatePatientId?.let { getHHMLocalIDByPatientId(it) }
            entity.householdMemberLocalId = hhmLocalId
            entity.neonateHouseholdMemberLocalId = neonateLocalId
            savePregnancyDetail(entity)
        }
    }

    @Query("DELETE from PregnancyDetail")
    suspend fun deleteAllPregnancyDetails()

    // ###################################################################

    @Query("SELECT neonateHouseholdMemberLocalId FROM PregnancyDetail where householdMemberLocalId =:parentId")
    suspend fun getChildPatientId(parentId: Long): Long?
}
