package org.medtroniclabs.uhis.data.registration

data class FillPrescriptionUpdateRequest(
    val patientTrackId: Long,
    val tenantId: Long,
    val patientVisitId: Long,
    val prescriptions: ArrayList<FillPrescription>,
)

data class FillPrescription(
    val id: Long,
    val prescription: String,
    var prescriptionFilledDays: Int = 0,
    var reason: String? = null,
    var otherReasonDetail: String? = null,
    var instructionNote: String = "",
    var instructionUpdated: Boolean = false,
    var productNumber: String? = null,
    var dosageFrequencyName: String? = null,
    val medicationName: String,
)
