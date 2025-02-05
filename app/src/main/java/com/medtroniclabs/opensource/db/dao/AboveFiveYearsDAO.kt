package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Query
import com.medtroniclabs.opensource.data.MedicalReviewMetaItems

@Dao
interface AboveFiveYearsDAO {

    @Query("SELECT * FROM MetaItemByTypeAndCategoryEntity WHERE type = :workflow ORDER BY displayOrder ASC")
    suspend fun getSummaryDetailMetaItems(workflow: String) : List<MedicalReviewMetaItems>
}