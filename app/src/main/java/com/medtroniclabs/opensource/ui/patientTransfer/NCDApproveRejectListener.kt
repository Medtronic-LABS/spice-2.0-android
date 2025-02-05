package com.medtroniclabs.opensource.ui.patientTransfer

import com.medtroniclabs.opensource.ncd.data.PatientTransfer

interface NCDApproveRejectListener {
    fun onTransferStatusUpdate(status: String, transfer: PatientTransfer)

    fun onViewDetail(patientID: Long)
}