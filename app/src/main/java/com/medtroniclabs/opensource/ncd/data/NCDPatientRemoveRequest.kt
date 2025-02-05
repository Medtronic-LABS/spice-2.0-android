package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDPatientRemoveRequest(
    val patientId: String? = null,
    val reason: String? = null,
    val provenance: ProvanceDto = ProvanceDto(),
    val otherReason: String? = null,
    val memberId: String? = null
)