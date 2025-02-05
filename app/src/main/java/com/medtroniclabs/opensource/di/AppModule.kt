package com.medtroniclabs.opensource.di

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.app.analytics.db.AnalyticsRepository
import com.medtroniclabs.opensource.common.AppConstants
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.ACTION_SESSION_EXPIRED
import com.medtroniclabs.opensource.common.DefinedParams.SL_SESSION
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.db.SpiceDataBase
import com.medtroniclabs.opensource.db.dao.AboveFiveYearsDAO
import com.medtroniclabs.opensource.db.dao.AssessmentDAO
import com.medtroniclabs.opensource.db.dao.CallHistoryDao
import com.medtroniclabs.opensource.db.dao.ConsentFormDao
import com.medtroniclabs.opensource.db.dao.DiagnosisDAO
import com.medtroniclabs.opensource.db.dao.ExaminationsComplaintsDAO
import com.medtroniclabs.opensource.db.dao.ExaminationsDAO
import com.medtroniclabs.opensource.db.dao.FollowUpCallsDao
import com.medtroniclabs.opensource.db.dao.FollowUpDao
import com.medtroniclabs.opensource.db.dao.FrequencyDAO
import com.medtroniclabs.opensource.db.dao.HouseholdDAO
import com.medtroniclabs.opensource.db.dao.LabourDeliveryDAO
import com.medtroniclabs.opensource.db.dao.LinkHouseholdMemberDao
import com.medtroniclabs.opensource.db.dao.MemberDAO
import com.medtroniclabs.opensource.db.dao.MetaDataDAO
import com.medtroniclabs.opensource.db.dao.NCDFollowUpDao
import com.medtroniclabs.opensource.db.dao.NcdMedicalReviewDao
import com.medtroniclabs.opensource.db.dao.PregnancyDetailDao
import com.medtroniclabs.opensource.db.dao.RiskFactorDAO
import com.medtroniclabs.opensource.db.dao.ScreeningDAO
import com.medtroniclabs.opensource.db.local.RoomHelper
import com.medtroniclabs.opensource.db.local.RoomHelperImpl
import com.medtroniclabs.opensource.network.ApiHelper
import com.medtroniclabs.opensource.network.ApiHelperImpl
import com.medtroniclabs.opensource.network.ApiService
import com.medtroniclabs.opensource.network.NetworkConstants
import com.medtroniclabs.opensource.network.NetworkConstants.BASE_URL
import com.medtroniclabs.opensource.network.interceptors.GZipRequestInterceptor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    private const val TIMEOUT_SECONDS = 60L // 3 Minutes to 1 Minutes

    @Singleton
    @Provides
    fun provideOkHttpClient(@ApplicationContext context: Context) = if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AppInterceptor(context))
            .addInterceptor(GZipRequestInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    } else {
        OkHttpClient.Builder()
            .addInterceptor(AppInterceptor(context))
            .addInterceptor(GZipRequestInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }


    class AppInterceptor(val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {

            var request: Request = chain.request()
            val requestBuilder = request.newBuilder()
                .header(
                    "Authorization",
                    SecuredPreference.getString(SecuredPreference.EnvironmentKey.TOKEN.toString())
                        ?: ""
                )
                .header("client", AppConstants.CLIENT_CONSTANT)
                .header("organizationId", SecuredPreference.getOrganizationFhirId())
                .header("tenantId", SecuredPreference.getTenantId().toString())
                .header("App-Version", getAppPackageInfo())

            SecuredPreference.getString(SecuredPreference.EnvironmentKey.TENANT_ID.toString())
                ?.let { tenantId ->
                    requestBuilder.header(DefinedParams.TenantId, tenantId)
                }

            request = requestBuilder.build()
            val response = chain.proceed(request)
            Timber.i("HEADERS ->\n${request.headers}")
            if (!request.url.toString().contains(NetworkConstants.AUTH_SESSION)
                && !response.isSuccessful && response.code == 401
            ) {
                redirectLogin(context)
            }
            return response
        }

        private fun isNetworkAvailable(): Boolean {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

            val network = connectivityManager.activeNetwork ?: return false
            val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false

            return when {
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                else -> false
            }
        }
    }

    private fun redirectLogin(context: Context) {
        val intent = Intent(ACTION_SESSION_EXPIRED)
        intent.putExtra(SL_SESSION, true)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    @Provides
    fun provideBaseUrl() = BASE_URL

    @Singleton
    @Provides
    fun providesRetrofit(okHttpClient: OkHttpClient, baseUrl: String): Retrofit =
        Retrofit.Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    @Singleton
    @Provides
    fun providesUserApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Singleton
    @Provides
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper {
        return apiHelper
    }


    @Singleton
    @Provides
    fun provideRoomHelper(roomHelper: RoomHelperImpl): RoomHelper {
        return roomHelper
    }

    @Singleton
    @Provides
    fun provideDatabase(@ApplicationContext context: Context): SpiceDataBase {
        return SpiceDataBase.getInstance(context)
    }

    @Singleton
    @Provides
    fun provideAnalyticsRepo(@ApplicationContext context: Context): AnalyticsRepository {
        return AnalyticsRepository(context)
    }

    @Singleton
    @Provides
    fun provideHouseholdDAO(db: SpiceDataBase): HouseholdDAO {
        return db.householdDAO()
    }

    @Singleton
    @Provides
    fun provideMemberDAO(db: SpiceDataBase): MemberDAO {
        return db.memberDAO()
    }

    @Singleton
    @Provides
    fun followUpCallDao(db: SpiceDataBase): FollowUpCallsDao {
        return db.followUpCallsDao()
    }

    @Singleton
    @Provides
    fun provideAssessmentDAO(db: SpiceDataBase): AssessmentDAO {
        return db.assessmentDAO()
    }

    @Singleton
    @Provides
    fun provideFollowUpDAO(db: SpiceDataBase): FollowUpDao {
        return db.followUpDao()
    }

    @Singleton
    @Provides
    fun provideMetaDataDAO(db: SpiceDataBase): MetaDataDAO {
        return db.metaDataDAO()
    }

    @Singleton
    @Provides
    fun provideExaminationComplaintsDAO(db: SpiceDataBase): ExaminationsComplaintsDAO {
        return db.examinationsComplaintsDAO()
    }

    @Singleton
    @Provides
    fun provideDiagnosisDAO(db: SpiceDataBase): DiagnosisDAO {
        return db.diagnosisDAO()
    }

    @Singleton
    @Provides
    fun provideConsentFormDAO(db: SpiceDataBase): ConsentFormDao {
        return db.consentFormDao()
    }

    @Singleton
    @Provides
    fun provideExaminationsDAO(db: SpiceDataBase): ExaminationsDAO {
        return db.examinationsDAO()
    }

    @Singleton
    @Provides
    fun provideAboveFiveYearsDAO(db: SpiceDataBase): AboveFiveYearsDAO {
        return db.aboveFiveYearsDAO()
    }

    @DefaultDispatcher
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @IoDispatcher
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main


    @Singleton
    @Provides
    fun provideLabourDeliveryDAO(db: SpiceDataBase): LabourDeliveryDAO {
        return db.labourDeliveryDAO()
    }

    @Singleton
    @Provides
    fun providePregnancyDetailDAO(db: SpiceDataBase): PregnancyDetailDao {
        return db.pregnancyDetailDao()
    }

    @Singleton
    @Provides
    fun provideLinkHouseholdMemberDao(db: SpiceDataBase): LinkHouseholdMemberDao {
        return db.linkHouseholdMemberDao()
    }

    @Singleton
    @Provides
    fun provideCallHistoryDao(db: SpiceDataBase): CallHistoryDao {
        return db.callHistoryDao()
    }

    @Singleton
    @Provides
    fun provideFrequencyDAO(db: SpiceDataBase): FrequencyDAO {
        return db.frequencyDao()
    }

    private fun getAppPackageInfo(): String {
        return BuildConfig.VERSION_NAME
    }

    /* NCD WorkFlow */
    @Singleton
    @Provides
    fun provideScreeningDAO(db: SpiceDataBase): ScreeningDAO {
        return db.screeningDAO()
    }

    @Singleton
    @Provides
    fun provideRiskFactorDao(db: SpiceDataBase): RiskFactorDAO {
        return db.riskFactorDao()
    }

    @Singleton
    @Provides
    fun provideNcdMedicalReviewDao(db: SpiceDataBase): NcdMedicalReviewDao {
        return db.ncdMedicalReviewDao()
    }

    @Singleton
    @Provides
    fun provideNcdFollowUpDAO(db: SpiceDataBase): NCDFollowUpDao {
        return db.ncdFollowUpDao()
    }
}

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class DefaultDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class IoDispatcher

@Retention(AnnotationRetention.BINARY)
@Qualifier
annotation class MainDispatcher