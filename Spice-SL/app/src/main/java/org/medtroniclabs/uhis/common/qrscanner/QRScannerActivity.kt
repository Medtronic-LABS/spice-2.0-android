package org.medtroniclabs.uhis.common.qrscanner

import android.content.Intent
import android.os.Bundle
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AppCompatActivity
import io.github.g00fy2.quickie.QRResult
import io.github.g00fy2.quickie.ScanCustomCode
import io.github.g00fy2.quickie.config.BarcodeFormat
import io.github.g00fy2.quickie.config.ScannerConfig

class QRScannerActivity : AppCompatActivity() {
    private var requestFrom: String? = null

    private val scanLauncher: ActivityResultLauncher<ScannerConfig> =
        registerForActivityResult(ScanCustomCode()) { result ->

            when (result) {

                is QRResult.QRSuccess -> {

                    val qrValue = result.content.rawValue

                    if (!qrValue.isNullOrEmpty()) {

                        setResult(
                            QRScanResult.RESULT_CODE,
                            Intent().apply {
                                putExtra(
                                    QRScanResult.REQUEST_FROM,
                                    requestFrom,
                                )

                                putExtra(
                                    QRScanResult.QR_RESULT,
                                    qrValue,
                                )
                            },
                        )
                    } else {
                        setResult(RESULT_CANCELED)
                    }

                    finish()
                }

                QRResult.QRUserCanceled -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }

                QRResult.QRMissingPermission -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }

                is QRResult.QRError -> {
                    setResult(RESULT_CANCELED)
                    finish()
                }
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestFrom = intent.getStringExtra(QRScanResult.REQUEST_FROM)

        scanLauncher.launch(
            ScannerConfig.build {
                setBarcodeFormats(
                    listOf(BarcodeFormat.FORMAT_QR_CODE),
                )
                setShowTorchToggle(true)
                setShowCloseButton(true)
                setKeepScreenOn(true)
                setUseFrontCamera(false)
                setHapticSuccessFeedback(true)
            },
        )
    }
}
