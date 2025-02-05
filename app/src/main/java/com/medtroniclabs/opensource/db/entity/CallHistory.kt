package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus
import com.medtroniclabs.opensource.db.entity.EntitiesName.CALL_HISTORY

@Entity(tableName = CALL_HISTORY)
data class CallHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val type: String,
    val referenceId: String,
    val callStartTime: Long,
    val callEndTime: Long,
    val syncStatus: OfflineSyncStatus = OfflineSyncStatus.NotSynced
)
