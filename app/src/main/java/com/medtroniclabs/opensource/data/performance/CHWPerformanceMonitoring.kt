package com.medtroniclabs.opensource.data.performance

data class CHWPerformanceMonitoring(
    val userId: String? = null,
    val chwName: String? = null,
    val villageName: String? = null,
    val villageId: String? = null,
    val household: Int? = null,
    val iccm: Int? = null,
    val otherSymptoms: Int? = null,
    val rmnch: Int? = null,
    val referred: Int? = null,
    val recovered: Int? = null,
    val worsened: Int? = null,
    val onTreatment: Int? = null,
    val householdMember: Int? = null,


    val followUpDueVisit: Int? = null,
    val followUpDueCalls: Int? = null,
    val followUpCondVisit: Int? = null,
    val followUpCondCalls: Int? = null,
)
