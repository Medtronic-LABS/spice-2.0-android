package com.medtroniclabs.opensource.ui.phuwalkins.listener

import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail

interface PhuLinkCallback {
    fun onLinkClicked(patientLinkedDetails: Any)
    fun onCallClicked(patientLinkedDetails: UnAssignedHouseholdMemberDetail)

}