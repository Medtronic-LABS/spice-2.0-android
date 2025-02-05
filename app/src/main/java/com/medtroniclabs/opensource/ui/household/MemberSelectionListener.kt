package com.medtroniclabs.opensource.ui.household

interface MemberSelectionListener {
    fun onMemberSelected(item: Long, isEdit: Boolean, dateOfBirth: String?)
}