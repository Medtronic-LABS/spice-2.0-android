package com.medtroniclabs.opensource.app.analytics.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.medtroniclabs.opensource.app.analytics.model.Analytics
import com.medtroniclabs.opensource.app.analytics.model.UserJourneyAnalytics

@Dao
interface AnalyticsDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalytics(analytics: Analytics): Long

    @Query("SELECT * FROM analytics")
    suspend fun getAllAnalytics(): List<Analytics>

    @Query("SELECT parameter FROM analytics WHERE referenceId=:referenceId AND eventType=:eventType")
    suspend fun getParameterByRefIdAndEvent(referenceId: String, eventType: String): String?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserJourneyAnalytics(userJourneyAnalytics: UserJourneyAnalytics): Long

    @Query("SELECT * FROM userJourneyAnalytics")
    suspend fun getUserJourney(): List<UserJourneyAnalytics>

    @Query("DELETE FROM userJourneyAnalytics")
    suspend fun deleteAllUserJourneys()

}