package org.medtroniclabs.uhis.common.qrscanner

import android.content.Intent

object QRScanResult {
    const val REQUEST_CODE = 1901
    const val RESULT_CODE = 1902
    const val REQUEST_FROM = "REQUEST_FROM"
    const val QR_RESULT = "QR_RESULT"

    fun getScanResult(intent: Intent?): QRScanResultItem {
        val from = intent?.getStringExtra(REQUEST_FROM) ?: ""
        val resultString = intent?.getStringExtra(QR_RESULT)
        return QRScanResultItem(from, resultString)
    }
}
