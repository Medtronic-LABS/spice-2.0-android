package org.medtroniclabs.uhis.data.servicerecipient

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class PatientHistoryData(
    val title: Int,
    val items: List<List<PatientHistoryDataItem>>,
    val isPaginationRequired: Boolean = true,
) : Parcelable

@Parcelize
data class PatientHistoryDataItem(
    val labelId: Int,
    val valueText: String? = null,
    val unitText: Int? = null,
    val textColor: Int? = null,
) : Parcelable
