package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "ShasthyaShebikaEntity",
    indices = [Index(value = ["ssId"], name = "idx_shasthya_shebika_ssId")],
)
data class ShasthyaShebikaEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val phoneNumber: String? = null,
    val ssId: String? = null,
    val shasthyaKormiId: Long? = null,
    val isActive: Boolean? = null,
)
