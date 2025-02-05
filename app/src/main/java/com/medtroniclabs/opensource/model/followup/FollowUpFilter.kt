package com.medtroniclabs.opensource.model.followup

import com.medtroniclabs.opensource.data.model.ChipViewItemModel

data class FollowUpFilter(
    var search: String = "",
    var type: String = "", //HH_VISIT, REFERRED, MEDICAL_REVIEW
    var villages: List<Long> = listOf(),
    var selectedVillages: List<ChipViewItemModel>? = null,
    var selectedDateRange: List<ChipViewItemModel>? = null,
    var fromDate: String = "",
    var toDate: String = ""
)
