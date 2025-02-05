package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.data.ShortageReasonEntity
import com.medtroniclabs.opensource.db.entity.NCDMedicalReviewMetaEntity
import com.medtroniclabs.opensource.db.entity.LifestyleEntity
import com.medtroniclabs.opensource.db.entity.NCDDiagnosisEntity
import com.medtroniclabs.opensource.db.entity.TreatmentPlanEntity

@Dao
interface NcdMedicalReviewDao {
    @Query("DELETE FROM NCDMedicalReviewMetaEntity")
    suspend fun deleteNCDMedicalReviewMeta()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNCDMedicalReviewMeta(items: List<NCDMedicalReviewMetaEntity>)

    @Query("DELETE FROM LifestyleEntity")
    suspend fun deleteLifestyle()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifestyle(items: List<LifestyleEntity>)

    @Query("SELECT * FROM NCDMedicalReviewMetaEntity WHERE (LOWER(:type) IS NULL OR LOWER(type) = LOWER(:type)) AND LOWER(category) = LOWER(:category)  ORDER BY displayOrder ASC")
    fun getComorbidities(type: String?, category: String): LiveData<List<NCDMedicalReviewMetaEntity>>

    @Query("SELECT * FROM lifestyleentity ORDER BY displayOrder ASC")
    fun getLifeStyle(): LiveData<List<LifestyleEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNCDDiagnosisList(diseaseEntityList: ArrayList<NCDDiagnosisEntity>)

    @Query("DELETE FROM NCDDiagnosisEntity")
    suspend fun deleteNCDDiagnosisList()

    @Query(
        "SELECT * FROM NCDDiagnosisEntity " +
                "WHERE" +
                " (LOWER(type) IN (:types) OR type IS NULL) " +
                "AND " +
                "(((LOWER(gender) = LOWER(:gender) OR (LOWER(gender) = LOWER('Both')) OR (gender IS NULL)))" +
                "AND ((:isPregnant = 1) OR (:isPregnant = 0 AND LOWER(value) != LOWER('gestationalDiabetes'))))ORDER BY displayOrder ASC"
    )
    fun getNCDDiagnosisList(
        types: List<String>,
        gender: String,
        isPregnant: Boolean
    ): LiveData<List<NCDDiagnosisEntity>>

    @Query("DELETE FROM TreatmentPlanEntity")
    suspend fun deleteTreatmentPlan()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTreatmentPlan(items: List<TreatmentPlanEntity>)

    @Query("SELECT * FROM TreatmentPlanEntity ORDER BY displayOrder ASC")
    fun getFrequencies(): LiveData<List<TreatmentPlanEntity>>

    @Query("SELECT * FROM shortageReason where type=:type")
    suspend fun getNCDShortageEntries(type: String): List<ShortageReasonEntity>

    @Query("DELETE FROM shortageReason")
    suspend fun deleteNCDShortageReason()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveNCDShortageReason(diseaseEntityList: List<ShortageReasonEntity>)
}