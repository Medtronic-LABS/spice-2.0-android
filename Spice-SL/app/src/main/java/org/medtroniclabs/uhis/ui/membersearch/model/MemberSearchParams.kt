package org.medtroniclabs.uhis.ui.membersearch.model

import org.medtroniclabs.uhis.data.model.ChipViewItemModel
import org.medtroniclabs.uhis.model.services.ServiceStaticFilter

/**
 * Search and filter state for member search.
 *
 * Drives both the filter UI and the paging query (local Room + optional remote API).
 */
data class MemberSearchParams(
    val staticFilter: ServiceStaticFilter = ServiceStaticFilter.ALL_MEMBERS,
    val searchInput: String? = null,
    val isOnline: Boolean = false,
    val isSiteBasedSearch: Boolean = true,
    val filterSk: Long = -1L,
    val filterBySs: List<ChipViewItemModel> = emptyList(),
    val filterBySubVillages: List<ChipViewItemModel> = emptyList(),
    val allowNullHousehold: Boolean = true,
    val skScopeSsIds: List<Long> = emptyList(),
    val qrCode: String? = null,
) {
    /** SS ids applied to the Room query, including SK-only fallback. */
    val effectiveSsIds: List<Long>
        get() {
            val fromChips = filterBySs.mapNotNull { it.id }
            if (fromChips.isNotEmpty()) return fromChips
            if (filterSk != -1L) return skScopeSsIds
            return emptyList()
        }

    /** Sub-village ids applied to the Room query. */
    val effectiveSubVillageIds: List<Long>
        get() = filterBySubVillages.mapNotNull { it.id }

    /** Number of active FO/PO filter dimensions (SK, SS, sub-village). */
    val activeFilterCount: Int
        get() {
            var count = 0
            if (filterSk != -1L) count++
            if (filterBySs.isNotEmpty()) count++
            if (filterBySubVillages.isNotEmpty()) count++
            return count
        }
}
