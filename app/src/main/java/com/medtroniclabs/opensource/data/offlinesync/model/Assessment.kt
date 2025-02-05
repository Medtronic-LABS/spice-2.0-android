package com.medtroniclabs.opensource.data.offlinesync.model

import com.google.gson.JsonElement

data class Assessment(
    val referenceId: Long,
    val assessmentType: String,
    val assessmentDetails: JsonElement,
    val villageId: String,
    val assessmentDate: String? = null,
    val patientStatus: String,
    val referredReasons: String? = null,
    val summary: JsonElement?,
    val encounter: AssessmentEncounter,
    val followUpId: Long? = null,
    val updatedAt: Long
)
