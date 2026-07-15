package org.medtroniclabs.uhis.data.model

import com.google.gson.annotations.SerializedName

/**
 * Village entries from API are only used to build [org.medtroniclabs.uhis.db.entity.ShasthyaKormiLinkedVillageEntity];
 * master data stays in [org.medtroniclabs.uhis.db.entity.VillageEntity].
 */
data class ShasthyaKormiVillageRef(
    @SerializedName("id")
    val id: Long,
)

data class ShasthyaKormi(
    @SerializedName("id")
    val id: Long,
    @SerializedName("firstName")
    val firstName: String,
    @SerializedName("lastName")
    val lastName: String,
    @SerializedName("gender")
    val gender: String? = null,
    @SerializedName("phoneNumber")
    val phoneNumber: String? = null,
    @SerializedName("username")
    val username: String? = null,
    @SerializedName("email")
    val email: String? = null,
    @SerializedName("villages")
    val villages: List<ShasthyaKormiVillageRef>? = null,
)
