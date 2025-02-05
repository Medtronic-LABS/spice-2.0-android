package com.medtroniclabs.opensource.ui.household

interface HouseholdSelectionListener {
    fun onHouseHoldSelected(id: Long)
    fun filterHouseholdList()
}