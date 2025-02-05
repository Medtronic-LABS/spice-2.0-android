package com.medtroniclabs.opensource.app.analytics.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils.getCurrentDateTimeInUTC

@Entity(tableName = "analytics")
data class Analytics(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    val userId: String,
    val role:String,
    val eventType: String,
    val parameter: String,
    val referenceId: String? = null,
    val lastSyncDate:String? = null,
    val createdAt: String = getCurrentDateTimeInUTC(),
    var updateAt: String = getCurrentDateTimeInUTC()
)
