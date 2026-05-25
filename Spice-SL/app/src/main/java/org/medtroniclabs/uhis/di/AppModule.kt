package org.medtroniclabs.uhis.di

import android.content.Context
import android.content.Intent
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
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
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.app.analytics.db.AnalyticsRepository
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.ACTION_SESSION_EXPIRED
import org.medtroniclabs.uhis.common.DefinedParams.SL_SESSION
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.db.SpiceDataBase
import org.medtroniclabs.uhis.db.dao.AboveFiveYearsDAO
import org.medtroniclabs.uhis.db.dao.AssessmentDAO
import org.medtroniclabs.uhis.db.dao.CallHistoryDao
import org.medtroniclabs.uhis.db.dao.CommunityDetailsDAO
import org.medtroniclabs.uhis.db.dao.ConsentFormDao
import org.medtroniclabs.uhis.db.dao.DiagnosisDAO
import org.medtroniclabs.uhis.db.dao.ExaminationsComplaintsDAO
import org.medtroniclabs.uhis.db.dao.ExaminationsDAO
import org.medtroniclabs.uhis.db.dao.FollowUpCallsDao
import org.medtroniclabs.uhis.db.dao.FollowUpDao
import org.medtroniclabs.uhis.db.dao.FrequencyDAO
import org.medtroniclabs.uhis.db.dao.HivMetaDataDAO
import org.medtroniclabs.uhis.db.dao.HouseholdDAO
import org.medtroniclabs.uhis.db.dao.LabourDeliveryDAO
import org.medtroniclabs.uhis.db.dao.LinkHouseholdMemberDao
import org.medtroniclabs.uhis.db.dao.MemberAssessmentHistoryDao
import org.medtroniclabs.uhis.db.dao.MemberDAO
import org.medtroniclabs.uhis.db.dao.MetaDataDAO
import org.medtroniclabs.uhis.db.dao.NCDFollowUpDao
import org.medtroniclabs.uhis.db.dao.NcdMedicalReviewDao
import org.medtroniclabs.uhis.db.dao.PregnancyDetailDao
import org.medtroniclabs.uhis.db.dao.RiskFactorDAO
import org.medtroniclabs.uhis.db.dao.RxBuddyDetailsDAO
import org.medtroniclabs.uhis.db.dao.RxBuddyFollowUpDAO
import org.medtroniclabs.uhis.db.dao.ScreeningDAO
import org.medtroniclabs.uhis.db.dao.TreatmentDetailsDAO
import org.medtroniclabs.uhis.db.local.RoomHelper
import org.medtroniclabs.uhis.db.local.RoomHelperImpl
import org.medtroniclabs.uhis.network.ApiHelper
import org.medtroniclabs.uhis.network.ApiHelperImpl
import org.medtroniclabs.uhis.network.ApiService
import org.medtroniclabs.uhis.network.NetworkConstants
import org.medtroniclabs.uhis.network.interceptors.GZipRequestInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    private const val TIMEOUT_SECONDS = 5 * 60L // 15 Minutes to 5 Minutes

    @Singleton
    @Provides
    fun provideOkHttpClient(
        @ApplicationContext context: Context,
    ) = if (BuildConfig.DEBUG) {
        val loggingInterceptor = HttpLoggingInterceptor()
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY)
        OkHttpClient
            .Builder()
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AppInterceptor(context))
            .addInterceptor(GZipRequestInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    } else {
        OkHttpClient
            .Builder()
            .addInterceptor(AppInterceptor(context))
            .addInterceptor(GZipRequestInterceptor())
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }

    class AppInterceptor(val context: Context) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val token = if (SecuredPreference.getString(SecuredPreference.EnvironmentKey.TOKEN.toString())?.isNotEmpty() == true) {
                SecuredPreference.getString(SecuredPreference.EnvironmentKey.TOKEN.toString())
            } else {
                SecuredPreference.getString(SecuredPreference.EnvironmentKey.PEER_SUPERVISOR_NOTIFICATION_TOKEN.toString())
            }
            var request: Request = chain.request()
            val requestBuilder = request
                .newBuilder()
                .header(
                    "Authorization",
                    token
                        ?: "",
                ).header("client", AppConstants.CLIENT_CONSTANT)
                .header("organizationId", SecuredPreference.getOrganizationFhirId())
                .header("tenantId", SecuredPreference.getTenantId().toString())
                .header("App-Version", getAppVersionName())
                .header("App-Version-Code", getAppVersionCode().toString())

            SecuredPreference
                .getString(SecuredPreference.EnvironmentKey.TENANT_ID.toString())
                ?.let { tenantId ->
                    requestBuilder.header(DefinedParams.TENANT_ID, tenantId)
                }

            request = requestBuilder.build()
            val response = chain.proceed(request)
            Timber.i("HEADERS ->\n${request.headers}")
            if (!request.url.toString().contains(NetworkConstants.AUTH_SESSION) &&
                !response.isSuccessful &&
                response.code == 401
            ) {
                redirectLogin(context)
            }
            return response
        }
    }

    private fun redirectLogin(context: Context) {
        val intent = Intent(ACTION_SESSION_EXPIRED)

        intent.putExtra(SL_SESSION, true)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    @Provides
    fun provideBaseUrl(): String = BuildConfig.API_BASE_URL

    @Singleton
    @Provides
    fun providesRetrofit(
        okHttpClient: OkHttpClient,
        baseUrl: String,
    ): Retrofit =
        Retrofit
            .Builder()
            .addConverterFactory(GsonConverterFactory.create())
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .build()

    @Singleton
    @Provides
    fun providesUserApiService(retrofit: Retrofit): ApiService = retrofit.create(ApiService::class.java)

    @Singleton
    @Provides
    fun provideApiHelper(apiHelper: ApiHelperImpl): ApiHelper = apiHelper

    @Singleton
    @Provides
    fun provideRoomHelper(roomHelper: RoomHelperImpl): RoomHelper = roomHelper

    @Singleton
    @Provides
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): SpiceDataBase = SpiceDataBase.getInstance(context)

    @Singleton
    @Provides
    fun provideAnalyticsRepo(
        @ApplicationContext context: Context,
    ): AnalyticsRepository = AnalyticsRepository(context)

    @Singleton
    @Provides
    fun provideHouseholdDAO(db: SpiceDataBase): HouseholdDAO = db.householdDAO()

    @Singleton
    @Provides
    fun provideMemberDAO(db: SpiceDataBase): MemberDAO = db.memberDAO()

    @Singleton
    @Provides
    fun followUpCallDao(db: SpiceDataBase): FollowUpCallsDao = db.followUpCallsDao()

    @Singleton
    @Provides
    fun provideAssessmentDAO(db: SpiceDataBase): AssessmentDAO = db.assessmentDAO()

    @Singleton
    @Provides
    fun provideFollowUpDAO(db: SpiceDataBase): FollowUpDao = db.followUpDao()

    @Singleton
    @Provides
    fun provideMetaDataDAO(db: SpiceDataBase): MetaDataDAO = db.metaDataDAO()

    @Singleton
    @Provides
    fun provideExaminationComplaintsDAO(db: SpiceDataBase): ExaminationsComplaintsDAO = db.examinationsComplaintsDAO()

    @Singleton
    @Provides
    fun provideDiagnosisDAO(db: SpiceDataBase): DiagnosisDAO = db.diagnosisDAO()

    @Singleton
    @Provides
    fun provideConsentFormDAO(db: SpiceDataBase): ConsentFormDao = db.consentFormDao()

    @Singleton
    @Provides
    fun provideExaminationsDAO(db: SpiceDataBase): ExaminationsDAO = db.examinationsDAO()

    @Singleton
    @Provides
    fun provideAboveFiveYearsDAO(db: SpiceDataBase): AboveFiveYearsDAO = db.aboveFiveYearsDAO()

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
    fun provideLabourDeliveryDAO(db: SpiceDataBase): LabourDeliveryDAO = db.labourDeliveryDAO()

    @Singleton
    @Provides
    fun providePregnancyDetailDAO(db: SpiceDataBase): PregnancyDetailDao = db.pregnancyDetailDao()

    @Singleton
    @Provides
    fun provideLinkHouseholdMemberDao(db: SpiceDataBase): LinkHouseholdMemberDao = db.linkHouseholdMemberDao()

    @Singleton
    @Provides
    fun provideCallHistoryDao(db: SpiceDataBase): CallHistoryDao = db.callHistoryDao()

    @Singleton
    @Provides
    fun provideFrequencyDAO(db: SpiceDataBase): FrequencyDAO = db.frequencyDao()

    private fun getAppVersionName(): String = BuildConfig.VERSION_NAME

    private fun getAppVersionCode(): Int = BuildConfig.VERSION_CODE

    // NCD WorkFlow
    @Singleton
    @Provides
    fun provideScreeningDAO(db: SpiceDataBase): ScreeningDAO = db.screeningDAO()

    @Singleton
    @Provides
    fun provideRiskFactorDao(db: SpiceDataBase): RiskFactorDAO = db.riskFactorDao()

    @Singleton
    @Provides
    fun provideNcdMedicalReviewDao(db: SpiceDataBase): NcdMedicalReviewDao = db.ncdMedicalReviewDao()

    @Singleton
    @Provides
    fun provideNcdFollowUpDAO(db: SpiceDataBase): NCDFollowUpDao = db.ncdFollowUpDao()

    @Singleton
    @Provides
    fun provideConsentForm(db: SpiceDataBase): CommunityDetailsDAO = db.communityDetailsDao()

    @Singleton
    @Provides
    fun providesRxBuddyDAO(db: SpiceDataBase): RxBuddyDetailsDAO = db.rxBuddyDao()

    @Singleton
    @Provides
    fun provideTreatmentDetailsDAO(db: SpiceDataBase): TreatmentDetailsDAO = db.treatmentDetailsDao()

    @Singleton
    @Provides
    fun provideRxBuddyFollowUpDAO(db: SpiceDataBase): RxBuddyFollowUpDAO = db.rxBuddyFollowUpDao()

    @Singleton
    @Provides
    fun provideHivMetaDataDAO(db: SpiceDataBase): HivMetaDataDAO = db.hivMetaDataDAO()

    @Singleton
    @Provides
    fun provideMemberAssessmentHistoryDao(db: SpiceDataBase): MemberAssessmentHistoryDao = db.memberAssessmentHistoryDao()

    @Singleton
    @Provides
    fun provideIsNonProd() = BuildConfig.FLAVOR != AppConstants.FLAVOUR_PROD

    @Singleton
    @Provides
    fun provideAppUpdateManager(
        @ApplicationContext context: Context,
    ) = AppUpdateManagerFactory.create(context)
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
