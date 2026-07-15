package org.medtroniclabs.uhis.data.registration

data class SiteRoleModel(
    val limit: Int? = null,
    val skip: Int? = null,
    val roleName: String? = null,
    val tenantId: Long? = null,
    val searchTerm: String? = null,
)
