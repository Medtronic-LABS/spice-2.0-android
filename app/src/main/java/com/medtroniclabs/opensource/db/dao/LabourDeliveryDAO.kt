package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

import com.medtroniclabs.opensource.data.LabourDeliveryMetaEntity

@Dao
interface LabourDeliveryDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLabourDelivery(entityList: List<LabourDeliveryMetaEntity>)

    @Query("DELETE FROM LabourDeliveryMetaEntity")
    suspend fun deleteLabourDelivery()

    @Query("SELECT * FROM LabourDeliveryMetaEntity ORDER BY displayOrder ASC")
    suspend fun getLabourDelivery(): List<LabourDeliveryMetaEntity>
}