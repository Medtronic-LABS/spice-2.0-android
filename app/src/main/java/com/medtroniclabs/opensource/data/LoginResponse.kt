package com.medtroniclabs.opensource.data

data class LoginResponse(
    val firstName: String?,
    val lastName: String?,
    val username: String?,
    val isActive: Boolean,
    val roles: List<UserRole>?,
    val id: Long,
    val authorization: String?,
    var deviceInfoId: Long?,
    val countryCode: String?,
    val country: CountryModel,
    val currentDate: Long,
    var timezone: TimeZoneModel? = null,
    var tenantId: Long,
    var cultureId: Long? = null,
    var culture: CulturesEntity? = null,
    val organizations: ArrayList<OrganizationModel>? = null,
    val isSuperUser: Boolean,
    val isTermsAndConditionsAccepted: Boolean? = null,
    val suiteAccess: List<String>,
    val client: String,
    val phoneNumber: String? = null
)

data class TimeZoneModel(
    val id: Long,
    val offset: String? = null,
    val description: String
)

data class CountryModel(
    val id: Long,
    val name: String,
    val countryCode: String? = null,
    val phoneNumberCode: String? = null,
    val unitMeasurement:String? = null,
    val tenantId: Long? = null
)

data class OrganizationModel(
    val id: Long,
    val name: String
)

data class UserRole(
    val id: Int,
    val name: String,
    val level: Int,
    val authority: String
)