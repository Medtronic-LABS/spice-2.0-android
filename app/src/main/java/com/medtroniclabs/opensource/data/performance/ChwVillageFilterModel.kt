package com.medtroniclabs.opensource.data.performance

data class ChwVillageFilterModel(
    val firstName: String,
    val lastName: String,
    val username: String,
    val villages: ArrayList<VillageFilterModel>,
    val id: Long,
    val fhirId: String,
)

data class VillageFilterModel(val id: Long, val name: String, val userId: Long)