package org.medtroniclabs.uhis.data.registration

data class FillMedicineResponse(
    var medicationName: String,
    var requestedDays: Int,
    var dispensedDays: Int,
)
