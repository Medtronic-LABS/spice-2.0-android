package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class SiteResponse(
    val name: String?,
    @SerializedName("id")
    val _id: Long,
    val roleName: List<String>,
    @SerializedName("displayName")
    val roleDisplayName: List<String>? = null,
    val tenantId: Long,
    val countyId: Long? = null,
    var accountId: Long? = null,
    val subCountyId: Long? = null,
    val subCountyCode: String? = null,
    val culture: Culture,
    val isQualipharmEnabledSite: Boolean? = null,
    val siteLevel: String? = null,
    val workflows: WorkFlows? = null,
    val countryName: String? = null,
    val countyName: String? = null,
    val subCountyName: String? = null,
    val code: String? = null,
    val district: SiteDistrict,
)

data class SiteDistrict(
    val id: Long,
    val name: String?,
    val code: String?,
    val countryId: Long,
    val countyId: Long,
)

data class Culture(val id: Long)

data class WorkFlows(
    val bpLog: Boolean = false,
    val glucoseLog: Boolean = false,
    val phq4: Boolean = false,
    val cataract: Boolean = false,
    val eyeCare: Boolean = false,
)
