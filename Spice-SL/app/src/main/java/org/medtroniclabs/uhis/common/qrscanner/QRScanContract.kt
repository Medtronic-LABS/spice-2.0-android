package org.medtroniclabs.uhis.common.qrscanner

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract

class QRScanContract : ActivityResultContract<Intent, QRScanResultItem>() {
    override fun createIntent(
        context: Context,
        input: Intent,
    ): Intent = input

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ): QRScanResultItem = QRScanResult.getScanResult(intent)
}
