package com.medtroniclabs.opensource.model.household

import com.medtroniclabs.opensource.data.model.ChipViewItemModel

data class HouseHoldSearchFilter(
    var searchInput: String = "",
    var filterByVillage: List<ChipViewItemModel> = listOf(),
    var filterByStatus: List<ChipViewItemModel> = listOf()
)
