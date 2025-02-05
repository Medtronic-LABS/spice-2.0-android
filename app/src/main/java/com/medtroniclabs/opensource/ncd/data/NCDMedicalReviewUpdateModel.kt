package com.medtroniclabs.opensource.ncd.data

import com.medtroniclabs.opensource.data.offlinesync.model.ProvanceDto

data class NCDMedicalReviewUpdateModel(
    val memberReference: String? = null,
    val villageId: String? = null,
    val nextMedicalReviewDate: String? = null,
    val patientReference: String? = null,
    val provenance: ProvanceDto? = null
)

