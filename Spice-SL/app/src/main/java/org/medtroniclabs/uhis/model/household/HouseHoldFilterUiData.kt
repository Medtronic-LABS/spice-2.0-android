package org.medtroniclabs.uhis.model.household

import org.medtroniclabs.uhis.db.entity.ShasthyaKormiEntity
import org.medtroniclabs.uhis.db.entity.ShasthyaShebikaEntity

/**
 * FO/PO Services filter UI payload. Sub-villages load on demand via [org.medtroniclabs.uhis.ui.BaseFilterViewModel].
 *
 * @param skList Populated on initial open (all Shasthya Kormis); empty when only refreshing SS for a selected Kormi.
 */
data class HouseHoldFilterUiData(
    val ssList: List<ShasthyaShebikaEntity> = emptyList(),
    val skList: List<ShasthyaKormiEntity> = emptyList(),
)
