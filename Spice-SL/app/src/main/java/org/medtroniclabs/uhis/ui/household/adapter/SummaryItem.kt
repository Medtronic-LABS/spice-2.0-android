package org.medtroniclabs.uhis.ui.household.adapter

/**
 * Individual summary item getting used in assessment history adapter
 */
data class SummaryItem(
    val name: String,
    val value: String,
    val valueColor: Int? = null,
)
