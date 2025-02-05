package com.medtroniclabs.opensource.data.offlinesync.model

import com.medtroniclabs.opensource.appextensions.convertToUtcDateTime
import com.medtroniclabs.opensource.common.SecuredPreference


data class ProvanceDto(
    val userId: String = SecuredPreference.getUserFhirId(),
    val organizationId: String = SecuredPreference.getOrganizationFhirId(),
    var modifiedDate: String = System.currentTimeMillis().convertToUtcDateTime(),
    val spiceUserId: Long = SecuredPreference.getUserId()
)
