package org.medtroniclabs.uhis.data.registration

data class PHQ4Response(
    var phq4Score: Int? = null,
    var phq4RiskLevel: String? = null,
)
