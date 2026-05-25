package org.medtroniclabs.uhis.common.qrscanner

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.budiyev.android.codescanner.AutoFocusMode
import com.budiyev.android.codescanner.CodeScanner
import com.budiyev.android.codescanner.DecodeCallback
import com.budiyev.android.codescanner.ErrorCallback
import com.budiyev.android.codescanner.ScanMode
import org.medtroniclabs.uhis.databinding.ActivityQrscannerBinding

class QRScannerActivity : AppCompatActivity(), View.OnClickListener {
    lateinit var binding: ActivityQrscannerBinding
    lateinit var codeScanner: CodeScanner
    var requestFrom: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrscannerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        parseIntentData()
        initializeScanner()
    }

    private fun parseIntentData() {
        intent?.let {
            requestFrom = it.getStringExtra(QRScanResult.REQUEST_FROM)
        }
    }

    /*
     * camera -> CodeScanner.CAMERA_BACK or CAMERA_FRONT or specific camera id
     * formats -> list of type BarcodeFormat, Ex. listOf(BarcodeFormat.QR_CODE)
     * autoFocusMode -> AutoFocusMode.SAFE or CONTINUOUS
     * scanMode -> ScanMode.SINGLE or CONTINUOUS or PREVIEW
     * isAutoFocusEnabled -> Whether to enable auto focus or not
     * isFlashEnabled -> Whether to enable flash or not
     * errorCallback -> Define a ErrorCallback or ErrorCallback.SUPPRESS
     */
    private fun initializeScanner() {
        codeScanner = CodeScanner(this, binding.scannerView)
        codeScanner.camera = CodeScanner.CAMERA_BACK
        codeScanner.formats = CodeScanner.TWO_DIMENSIONAL_FORMATS
        codeScanner.autoFocusMode = AutoFocusMode.SAFE
        codeScanner.scanMode = ScanMode.SINGLE
        codeScanner.isAutoFocusEnabled = true
        codeScanner.isFlashEnabled = false

        codeScanner.decodeCallback = DecodeCallback { result ->
            runOnUiThread {
                codeScanner.releaseResources()
                setResult(
                    QRScanResult.RESULT_CODE,
                    Intent().apply {
                        putExtra(QRScanResult.REQUEST_FROM, requestFrom)
                        putExtra(QRScanResult.QR_RESULT, result.text)
                    },
                )
                finish()
            }
        }
        codeScanner.errorCallback = ErrorCallback {
//        runOnUiThread {
//          Camera initialization error
//        }
        }
    }

    override fun onClick(mView: View?) {
        when (mView?.id) {
            binding.scannerView.id -> {
                codeScanner.startPreview()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::codeScanner.isInitialized) {
            codeScanner.startPreview()
        }
    }

    override fun onPause() {
        if (::codeScanner.isInitialized) {
            codeScanner.releaseResources()
        }
        super.onPause()
    }
}
