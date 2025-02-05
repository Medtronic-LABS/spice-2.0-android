package com.medtroniclabs.opensource.app.analytics.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userJourneyAnalytics")
data class UserJourneyAnalytics(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val userId:String,
    val sessionId: String,
    val userJourney: String,
    val startTime: String? = null
)


data class ScreenDetails(
    val userJourney: String,
    val startTime: String
)