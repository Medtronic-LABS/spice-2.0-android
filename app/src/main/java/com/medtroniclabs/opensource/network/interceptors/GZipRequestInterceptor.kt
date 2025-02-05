package com.medtroniclabs.opensource.network.interceptors

import com.medtroniclabs.opensource.network.NetworkConstants
import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.RequestBody
import okhttp3.Response
import okio.BufferedSink
import okio.GzipSink
import okio.buffer

class GZipRequestInterceptor : Interceptor {

    private val gZIPHeaderKey = "Content-Encoding"
    private val gZIPHeaderValue = "gzip"
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        if (chain.request().url.toString().contains(NetworkConstants.OFFLINE_SYNC_CREATE)) {
            val compressedRequest = originalRequest.newBuilder()
                .header(gZIPHeaderKey, gZIPHeaderValue)
                .method(originalRequest.method, gzip(originalRequest.body))
                .build()
            return chain.proceed(compressedRequest)
        }
        return chain.proceed(originalRequest)
    }

    private fun gzip(body: RequestBody?): RequestBody {
        return object : RequestBody() {
            override fun contentType(): MediaType? {
                return body?.contentType()
            }

            override fun writeTo(sink: BufferedSink) {
                val gzipSink = GzipSink(sink).buffer()
                body?.writeTo(gzipSink)
                gzipSink.close()
            }
        }
    }
}