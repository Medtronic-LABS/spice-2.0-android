package org.medtroniclabs.uhis.data.registration

data class QRCodeResponse(
    val message: String?,
    val status: Boolean = false,
    var qrCode: String? = null,
)
