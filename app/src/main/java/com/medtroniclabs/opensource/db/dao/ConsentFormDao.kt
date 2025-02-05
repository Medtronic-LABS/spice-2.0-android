package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.db.entity.ConsentForm

@Dao
interface ConsentFormDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(consentForm: ConsentForm): Long

    @Query("DELETE FROM ConsentForm")
    suspend fun delete()

    @Query("SELECT * FROM ConsentForm WHERE type = :type")
    suspend fun getConsentFormByType(type: String): ConsentForm?

}