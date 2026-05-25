package org.medtroniclabs.uhis.data.registration

import com.google.gson.annotations.SerializedName

data class ConfirmDiagnosesRequest(
    var confirmDiagnosis: ArrayList<Diagnosis>? = null,
    var diagnosisComments: String? = null,
    var patientTrackId: Long? = null,
    @SerializedName("tenant_id")
    var tenantId: Long? = null,
)

data class Diagnosis(val name: String)
