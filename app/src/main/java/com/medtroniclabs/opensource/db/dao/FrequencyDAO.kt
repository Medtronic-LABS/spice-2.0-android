package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.db.entity.FrequencyEntity

@Dao
interface FrequencyDAO {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFrequencyList(frequencyList: List<FrequencyEntity>): List<Long>

    @Query("SELECT * FROM FrequencyEntity order by displayOrder")
    suspend fun getFrequencyList(): List<FrequencyEntity>

    @Query("DELETE FROM FrequencyEntity")
    suspend fun deleteAllVillages()

}