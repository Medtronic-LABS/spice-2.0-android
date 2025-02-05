package com.medtroniclabs.opensource.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus

@Entity
open class BaseEntity {

    @ColumnInfo("fhir_id")
    var fhirId: String? = null

    @ColumnInfo("sync_status")
    var sync_status: OfflineSyncStatus = OfflineSyncStatus.NotSynced

    @ColumnInfo("created_by")
    val createdBy: Long = SecuredPreference.getUserId()

    @ColumnInfo("updated_at")
    var updatedAt: Long = System.currentTimeMillis()

    @ColumnInfo("created_at")
    val createdAt: Long = System.currentTimeMillis()

    fun setCreatedBy(id: Long) {}

    fun setCreatedAt(id: Long) {}
}