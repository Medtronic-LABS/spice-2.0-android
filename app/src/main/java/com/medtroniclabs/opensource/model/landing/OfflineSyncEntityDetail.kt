package com.medtroniclabs.opensource.model.landing

data class OfflineSyncEntityDetail(
    val tableName: String,
    var unSyncedCount: Int
)
