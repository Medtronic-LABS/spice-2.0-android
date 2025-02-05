package com.medtroniclabs.opensource.app.analytics.network

import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


object RetrofitHelper {

    fun initializeRetrofit(baseUrl: String, headers: String): ApiService {

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(createClient(headers))
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ApiService::class.java)
    }

    private fun createClient(headers: String): OkHttpClient {
        return OkHttpClient.Builder()
            .apply {
                addHeaders(headers)
                addLoggingInterceptor()
            }
            .build()
    }

    private fun OkHttpClient.Builder.addHeaders(headers: String): OkHttpClient.Builder {
        addInterceptor { chain ->
            val request = chain.request().newBuilder()
                .apply {
                    addHeader(AnalyticsDefinedParams.Authorization, headers)
                    addHeader(AnalyticsDefinedParams.Client, AnalyticsDefinedParams.Client_Constant)
                }
                .build()
            chain.proceed(request)
        }
        return this
    }

    private fun OkHttpClient.Builder.addLoggingInterceptor(): OkHttpClient.Builder {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        addInterceptor(loggingInterceptor)
        return this
    }
}
