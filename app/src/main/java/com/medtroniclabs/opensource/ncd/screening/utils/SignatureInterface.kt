package com.medtroniclabs.opensource.ncd.screening.utils

import android.graphics.Bitmap

interface SignatureInterface {
    fun applySignature(signature: Bitmap?, initial: String?)
}