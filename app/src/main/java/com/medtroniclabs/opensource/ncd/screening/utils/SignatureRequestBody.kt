package com.medtroniclabs.opensource.ncd.screening.utils

import android.content.Context
import android.graphics.Bitmap
import com.google.gson.Gson
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.mappingkey.Screening
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.FileOutputStream

object SignatureRequestBody {
    fun signatureRequestBody(
        context: Context,
        triple: Triple<String, String, String?>,
        signature: ByteArray?,
        isScreening: Boolean
    ): RequestBody {
        val builder = MultipartBody.Builder()
        builder.setType(MultipartBody.FORM)

        val hashMap = CommonUtils.parseRequest(
            triple.first,
            triple.second,
            triple.third
        )
        val request = Gson().toJson(hashMap)
        builder.addFormDataPart(getParam(isScreening), request)

        signature?.let { sign ->
            val signMap = CommonUtils.convertByteArrayToBitmap(sign)

            val identityValue = CommonUtils.getIdentityValue(hashMap)
            val fileName = "${identityValue}${getSuffix(isScreening)}.jpeg"

            val filePath = CommonUtils.getFilePath(identityValue, context)
            filePath.mkdirs()

            val file = File(filePath, fileName)

            val clearedExistingFile: Boolean = if (file.exists()) file.delete() else true

            if (clearedExistingFile && signMap != null) {
                val out = FileOutputStream(file)
                signMap.compress(Bitmap.CompressFormat.JPEG, 20, out)
                out.flush()
                out.close()
                file.let {
                    builder.addFormDataPart(
                        "signatureFile",
                        file.name,
                        file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                    )
                }
            }
        }

        return builder.build()
    }

    private fun getParam(screening: Boolean): String {
        return if (screening) "screeningRequest" else "registrationRequest"
    }

    private fun getSuffix(screening: Boolean): String {
        return if (screening) Screening.ScreeningSignSuffix else Screening.RegistrationSignSuffix
    }
}