package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Index

@Entity(
    tableName = "ShasthyaKormiLinkedVillageEntity",
    primaryKeys = ["shasthyaKormiId", "villageId"],
    indices = [
        Index(value = ["shasthyaKormiId"], name = "idx_linked_shasthya_kormi_id"),
        Index(value = ["villageId"], name = "idx_linked_kormi_village_id"),
    ],
)
data class ShasthyaKormiLinkedVillageEntity(
    val shasthyaKormiId: Long,
    val villageId: Long,
)
