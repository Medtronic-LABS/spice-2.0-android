package org.medtroniclabs.uhis.data.registration

data class LabTestListResponse(
    var patientLabTest: ArrayList<LabTestModel>? = null,
    var patientLabtestDates: ArrayList<VisitDateModel>? = null,
)
