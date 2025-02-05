package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDPregnancyRiskUpdate(
    var isPregnancyRisk: Boolean? = null,
    val memberReference: String? = null,
    val patientReference: String? = null,
    val provenance: ProvanceDto? = null
)