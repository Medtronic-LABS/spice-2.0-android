package org.medtroniclabs.uhis.ui.patient

import org.medtroniclabs.uhis.data.model.FilterModel
import org.medtroniclabs.uhis.data.model.SortModel

interface FilterSortInterface {
    fun filter(filterModel: FilterModel)

    fun sort(sortModel: SortModel?)
}
