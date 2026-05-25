package org.medtroniclabs.uhis.data.servicerecipient

data class PatientHistoryData(
    val title: Int,
    val items: List<List<PatientHistoryDataItem>>,
    val isPaginationRequired: Boolean = true,
)

data class PatientHistoryDataItem(
    val labelId: Int,
    val valueText: String? = null,
    val unitText: Int? = null,
    val textColor: Int? = null,
)
