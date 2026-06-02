package org.medtroniclabs.uhis.model.services

import org.medtroniclabs.uhis.data.model.ChipViewItemModel

data class ServicesSearchFilter(
    var searchInput: String = "",
    var filterBySs: List<ChipViewItemModel> = listOf(),
    var filterBySubVillages: List<ChipViewItemModel> = listOf(),
    var staticFilter: ServiceStaticFilter = ServiceStaticFilter.ALL_MEMBERS,
    var filterSk: Long = -1,
    var qrCode: String? = null,
)
