package org.medtroniclabs.uhis.model.followup

import org.medtroniclabs.uhis.data.model.ChipViewItemModel

data class FollowUpFilter(
    var search: String = "",
    var type: String = "", // HH_VISIT, REFERRED, MEDICAL_REVIEW
    var villages: List<Long> = listOf(),
    var selectedShashtyaShebikas: List<ChipViewItemModel>? = null,
    var selectedVillages: List<ChipViewItemModel>? = null,
    var selectedDateRange: List<ChipViewItemModel>? = null,
    var selectedReferralReasons: List<ChipViewItemModel>? = null,
    var ncdSelectedReasons: List<ChipViewItemModel>? = null,
    var ncdSelectedReferralTo: List<ChipViewItemModel>? = null,
    var fromDate: String = "",
    var toDate: String = "",
    var remainingAttempt: List<ChipViewItemModel>? = null,
    var callStatus: List<ChipViewItemModel>? = null,
    var sortOrder: FollowUpSortOrder = FollowUpSortOrder.DEFAULT,
)
