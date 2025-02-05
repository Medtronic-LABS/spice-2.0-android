package com.medtroniclabs.opensource.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.medtroniclabs.opensource.db.entity.FollowUpCall

@Dao
interface FollowUpCallsDao {

    @Insert
    suspend fun insertFollowUpCall(followUpCall: FollowUpCall)

    @Query("SELECT * FROM FollowUpCall WHERE followUpId = :followUpId AND isSynced = 0")
    suspend fun getAllFollowUpCalls(followUpId: Long): List<FollowUpCall>

    @Query("DELETE FROM FollowUpCall")
    suspend fun deleteAllFollowUpCalls()

    @Query("UPDATE FollowUpCall SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun updateSyncSuccess(ids: List<Long>)
}