package org.medtroniclabs.uhis.data.registration

data class GraphModel(
    var bpResponse: BPResponse? = null,
    var bgResponse: BloodGlucose? = null,
    var index: Int,
    var size: Int,
    var isForward: Boolean? = null,
)
