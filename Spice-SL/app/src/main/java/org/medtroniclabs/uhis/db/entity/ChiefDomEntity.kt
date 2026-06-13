package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "ChiefDomEntity")
data class ChiefDomEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val code: String? = null,
    val districtId: Long,
) {
    @Ignore
    @SerializedName("isDistrictChiefdom")
    var isDistrictChiefdom: Boolean? = false
}
