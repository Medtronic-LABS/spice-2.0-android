package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.db.entity.RiskFactorEntity

@Dao
interface RiskFactorDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRiskFactor(riskFactorEntity: RiskFactorEntity)

    @Query("SELECT * FROM RiskFactorEntity")
    fun getAllRiskFactorEntity(): LiveData<List<RiskFactorEntity>>

    @Query("Delete from RiskFactorEntity")
    suspend fun deleteRiskFactor()

}