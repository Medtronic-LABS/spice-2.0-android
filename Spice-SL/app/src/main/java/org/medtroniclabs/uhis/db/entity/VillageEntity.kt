package org.medtroniclabs.uhis.db.entity

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "VillageEntity")
data class VillageEntity(
    @PrimaryKey
    val id: Long,
    val name: String,
    val villagecode: String? = null,
    val chiefdomId: Long? = null,
    val countryId: Long,
    val districtId: Long? = null,
    var isUserVillage: Boolean = false,
    val chiefdomCode: String? = null,
    val districtCode: String? = null,
    val healthFacilityId: Long? = null,
) {
    @Ignore
    @SerializedName("isDistrictVillage")
    var isDistrictVillage: Boolean? = false
}
