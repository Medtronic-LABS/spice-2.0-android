package com.medtroniclabs.opensource.ncd.medicalreview

interface NCDDialogDismissListener {
    fun onDialogDismissed(isConfirmed: Boolean = false)
    fun closePage()
}