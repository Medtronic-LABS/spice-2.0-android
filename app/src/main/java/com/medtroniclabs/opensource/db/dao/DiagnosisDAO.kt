package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.data.DiseaseCategoryItems

@Dao
interface DiagnosisDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDiagnosisList(diseaseEntityList: ArrayList<DiseaseCategoryItems>)

    @Query("DELETE FROM DiagnosisEntity where type = :diagnosisType")
    suspend fun deleteDiagnosisList(diagnosisType: String)

    @Query("SELECT * FROM DiagnosisEntity Where type=:diagnosisType ORDER BY displayOrder ASC")
    suspend fun getDiagnosisList(diagnosisType: String): List<DiseaseCategoryItems>

}