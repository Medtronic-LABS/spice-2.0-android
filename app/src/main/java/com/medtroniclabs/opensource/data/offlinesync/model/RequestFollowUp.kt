package com.medtroniclabs.opensource.data.offlinesync.model

import androidx.room.Ignore
import com.medtroniclabs.opensource.db.entity.FollowUpCall

data class RequestFollowUp(
    val referenceId: Long,
    val id: Long?,
    val attempts: Int,
    val successfulAttempts: Int,
    val unsuccessfulAttempts: Int,
    val currentPatientStatus: String?
) {
    @Ignore
    var followUpDetails : List<FollowUpCall> = listOf()
}
