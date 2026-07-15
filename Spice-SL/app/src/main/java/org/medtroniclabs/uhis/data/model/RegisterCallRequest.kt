package org.medtroniclabs.uhis.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

data class RegisterCallRequest(var patientTrackId: Long, val callType: String?)

@Parcelize
data class PatientDetailsFollowUp(
    var patientTrackId: Long? = null,
    var callRegisterId: Long? = null,
) : Parcelable
