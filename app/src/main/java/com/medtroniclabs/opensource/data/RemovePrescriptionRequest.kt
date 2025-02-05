package com.medtroniclabs.opensource.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class RemovePrescriptionRequest(
    val prescriptionId: String? = null,
    val provenance: ProvanceDto? = null,
    val discontinuedReason: String? = null,
    val requestFrom: String? = null
)