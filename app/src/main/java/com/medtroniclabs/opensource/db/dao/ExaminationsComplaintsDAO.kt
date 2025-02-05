package com.medtroniclabs.opensource.db.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems

@Dao
interface ExaminationsComplaintsDAO {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExaminationsComplaints(entityList: List<MedicalReviewMetaItems>)

    @Query("DELETE FROM MetaItemByTypeAndCategoryEntity WHERE type = :menuType")
    suspend fun deleteExaminationsComplaints(menuType: String)

    @Query("SELECT * FROM MetaItemByTypeAndCategoryEntity WHERE type = :workflow ORDER BY displayOrder ASC")
    suspend fun getExaminationsComplaintByType(workflow: String): List<MedicalReviewMetaItems>

    @Query("DELETE FROM MetaItemByTypeAndCategoryEntity WHERE type = :type")
    suspend fun deleteExaminationsComplaintsForAnc(type: String)

    @Query("SELECT * FROM MetaItemByTypeAndCategoryEntity WHERE LOWER(category) = LOWER(:category) AND LOWER(type) = LOWER(:type) ORDER BY displayOrder ASC")
    fun getExaminationsComplaintsForAnc(
        category: String,
        type: String
    ): LiveData<List<MedicalReviewMetaItems>>

    @Query("SELECT * FROM MetaItemByTypeAndCategoryEntity WHERE type = :workflow ORDER BY displayOrder ASC")
    fun getExaminationsComplaintByTypeLiveData(workflow: String): LiveData<List<MedicalReviewMetaItems>>
    @Query("SELECT * FROM MetaItemByTypeAndCategoryEntity WHERE LOWER(category) = LOWER(:category) AND LOWER(type) =  LOWER(:types) ORDER BY displayOrder ASC")
    fun getExaminationsComplaintsForPnc(category: String, types: String): LiveData<List<MedicalReviewMetaItems>>

}