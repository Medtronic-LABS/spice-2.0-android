package com.medtroniclabs.opensource.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.medtroniclabs.opensource.data.offlinesync.utils.OfflineSyncStatus

@Entity(tableName = EntitiesName.LINK_HOUSEHOLD_MEMBER)
data class LinkHouseholdMember(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val memberId: String,
    var status: String,
    var syncStatus: OfflineSyncStatus? = OfflineSyncStatus.NotSynced
)
