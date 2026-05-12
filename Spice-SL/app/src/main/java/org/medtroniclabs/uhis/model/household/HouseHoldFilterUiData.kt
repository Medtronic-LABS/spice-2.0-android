package org.medtroniclabs.uhis.model.household

import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity
import org.medtroniclabs.uhis.db.entity.SubVillageEntity

/**
 * UI filter data for filtering house holds / service recipient filters.
 *
 * @param skList FO/PO service recipient: all Shasthya Kormis for single-select; empty for other roles.
 * @param selectedShasthyaKormiId FO/PO after SS/village data loaded for a chosen SK; used to restore SK chip selection.
 */
data class HouseHoldFilterUiData(
    val ssList: List<ShasthyaShebikaEntity>,
    val subVillages: List<SubVillageEntity>,
    val skList: List<ShasthyaKormiEntity> = emptyList(),
    val selectedShasthyaKormiId: Long? = null,
)
