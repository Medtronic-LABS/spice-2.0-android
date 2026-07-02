package org.medtroniclabs.uhis

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.medtroniclabs.microcoaching.Language
import com.medtroniclabs.microcoaching.MicroCoachingSDK
import com.medtroniclabs.microcoaching.ModelDownloadStrategy
import com.medtroniclabs.microcoaching.ai.model.ModelProvider
import com.medtroniclabs.microcoaching.domain.decision.CoachingMode
import com.medtroniclabs.microcoaching.sherpa.SherpaOnnxStt
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.medtroniclabs.uhis.app.analytics.db.AnalyticsRepository
import org.medtroniclabs.uhis.app.analytics.model.ScreenDetails
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.model.UserJourneyAnalytics
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils
import org.medtroniclabs.uhis.appextensions.isDebug
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SPICE
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.resolveCoachingPersona
import org.medtroniclabs.uhis.log.CrashReportingTree
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject

const val ACTIVITY_LIFECYCLE = "ActivityLifeCycle"
const val FRAGMENT_LIFECYCLE = "FragmentLifecycle"

@HiltAndroidApp
class SpiceBaseApplication : Application(), Configuration.Provider {
    @Inject
    lateinit var hiltWorkerFactory: HiltWorkerFactory

    @Inject
    lateinit var analyticsRepository: AnalyticsRepository

    private var activityCount = 0

    private var fragmentCallbacks =
        mutableMapOf<Activity, FragmentManager.FragmentLifecycleCallbacks>()

    override fun onCreate() {
        super.onCreate()
        initTimber()
        initPreference()
        initCoachingSdk()
        saveApplicationType()
        getUserJourneyAnalytics()
        handleAppForeground()
    }

    /**
     * Initialise the MicroCoaching SDK. Mirrors the v1 spice-android pattern.
     * Must run after [initPreference] so [SecuredPreference] is ready, and on the
     * Application thread so the SDK singleton is available before any Activity
     * (including the splash) tries to access it.
     *
     * The auth token is read opportunistically here; it may be empty on first
     * install. [LandingActivity.reinitCoachingSdkWithToken] re-builds the SDK
     * with the freshly issued JWT after a successful login.
     */
    private fun initCoachingSdk() {
        val modelDir = getExternalFilesDir(null)
        val existingModel = modelDir
            ?.listFiles()
            ?.firstOrNull { it.extension == "task" || it.extension == "litertlm" }
        val downloadStrategy = if (existingModel != null) {
            ModelDownloadStrategy.PROVIDED
        } else {
            ModelDownloadStrategy.ON_FIRST_USE
        }
        val authToken = SecuredPreference.getString(
            SecuredPreference.EnvironmentKey.TOKEN.name,
        ) ?: ""
        MicroCoachingSDK
            .Builder(this)
            .language(spiceLanguageToSdkLanguage(SecuredPreference.getCultureName()))
            .backendUrl(BuildConfig.COACHING_BACKEND_URL)
            .authToken(authToken)
            .persona(resolveCoachingPersona())
            .enableTelemetry(BuildConfig.ENABLE_COACHING_TELEMETRY)
            .enableChat(true)
            .enableLearnModule(true)
            .enableApplyModule(true)
            .enableVoice(true)
            .offlineSttEngineFactory(SherpaOnnxStt.factory)
            .modelDownloadStrategy(downloadStrategy)
            .modelProviders(listOf(ModelProvider.HuggingFace))
            .modelPath(existingModel?.absolutePath ?: "")
            .huggingFaceToken(BuildConfig.HF_TOKEN)
            .wifiOnlyModelDownload(false)
            .forceMode(CoachingMode.ONLINE)
            .build()
        if (BuildConfig.DEBUG) {
            Timber.i("MicroCoachingSDK health: %s", MicroCoachingSDK.getInstance().checkHealth())
        }
    }

    companion object {
        /**
         * Map SPICE 2.0's stored culture display name to the SDK's [Language] enum.
         * SPICE stores "English" or "বাংলা" (Bengali in Bengali) — the SDK enum needs
         * the language code, so we branch on the Bengali display name.
         */
        fun spiceLanguageToSdkLanguage(cultureName: String?): Language {
            // cultureName may be the bare "বাংলা" or the full "বাংলা (Bangla)" display string —
            // use contains (case-insensitive) to match either form, consistent with CommonUtils.
            val resolvedLanguage = if (cultureName?.contains(DefinedParams.BN_Locale, ignoreCase = true) == true) {
                Language.BANGLA
            } else {
                Language.ENGLISH
            }
            Timber.i("spiceLanguageToSdkLanguage: resolved $cultureName to $resolvedLanguage")
            return resolvedLanguage
        }
    }

    private fun logActivityState(
        activity: Activity,
        state: String,
    ) {
        Timber
            .tag(ACTIVITY_LIFECYCLE)
            .d("$state : ${activity.javaClass.name} ${activity.hashCode()}")
    }

    private fun logFragmentState(
        activity: Activity,
        fragment: Fragment,
        state: String,
    ) {
        Timber
            .tag(FRAGMENT_LIFECYCLE)
            .d("$state : ${fragment.javaClass.name} ${fragment.hashCode()} in ${activity.javaClass.name} ${activity.hashCode()}")
    }

    private fun handleAppForeground() {
        registerActivityLifecycleCallbacks(object : ActivityLifecycleCallbacks {
            override fun onActivityStarted(activity: Activity) {
                logActivityState(activity, "onActivityStarted")
                activityCount++
                if (activityCount == 1) {
                    val backgroundTime = SecuredPreference.getLong(
                        SecuredPreference.EnvironmentKey.BACKGROUNDTIMESTAMP.name,
                        0L,
                    )
                    val currentTime = System.currentTimeMillis()
                    if (backgroundTime != 0L) {
                        val diffInMinutes = (currentTime - backgroundTime) / (1000 * 60)
                        if (diffInMinutes >= 2) {
                            SecuredPreference.putLong(
                                SecuredPreference.EnvironmentKey.BACKGROUNDTIMESTAMP.name,
                                0L,
                            )
                            getUserJourneyAnalytics()
                        }
                    }
                }
            }

            override fun onActivityStopped(activity: Activity) {
                logActivityState(activity, "onActivityStopped")
                activityCount--
                if (activityCount == 0) {
                    // App has gone to background
                    Log.d("SpiceApp", "🌙 App in BACKGROUND")
                    SecuredPreference.putLong(
                        SecuredPreference.EnvironmentKey.BACKGROUNDTIMESTAMP.name,
                        System.currentTimeMillis(),
                    )
                    // End session tracking, etc.
                }
            }

            // Other lifecycle methods — no-op
            override fun onActivityCreated(
                activity: Activity,
                savedInstanceState: Bundle?,
            ) {
                logActivityState(activity, "onActivityCreated")
                registerFragmentLifecycleCallbacks(activity as? AppCompatActivity)
            }

            override fun onActivityResumed(activity: Activity) {}

            override fun onActivityPaused(activity: Activity) {}

            override fun onActivitySaveInstanceState(
                activity: Activity,
                outState: Bundle,
            ) {}

            override fun onActivityDestroyed(activity: Activity) {
                logActivityState(activity, "onActivityDestroyed")
                unRegisterFragmentLifecycleCallbacks(activity as? AppCompatActivity)
            }
        })
    }

    private fun registerFragmentLifecycleCallbacks(activity: AppCompatActivity?) {
        activity ?: return
        val listener = object : FragmentManager.FragmentLifecycleCallbacks() {
            override fun onFragmentViewCreated(
                fm: FragmentManager,
                fragment: Fragment,
                v: View,
                savedInstanceState: Bundle?,
            ) {
                super.onFragmentViewCreated(fm, fragment, v, savedInstanceState)
                logFragmentState(activity, fragment, "onFragmentViewCreated")
            }

            override fun onFragmentViewDestroyed(
                fm: FragmentManager,
                fragment: Fragment,
            ) {
                super.onFragmentViewDestroyed(fm, fragment)
                logFragmentState(activity, fragment, "onFragmentViewDestroyed")
            }
        }
        activity.supportFragmentManager.registerFragmentLifecycleCallbacks(listener, true)
        fragmentCallbacks[activity] = listener
    }

    private fun unRegisterFragmentLifecycleCallbacks(activity: AppCompatActivity?) {
        activity ?: return
        // remove() (not just a lookup) — leaving the entry keyed the map on the
        // destroyed Activity forever, retaining it and its view tree.
        val listener = fragmentCallbacks.remove(activity) ?: return
        activity.supportFragmentManager.unregisterFragmentLifecycleCallbacks(listener)
    }

    private fun saveApplicationType() {
        SecuredPreference.putString(
            SecuredPreference.EnvironmentKey.APPLICATION.name,
            getApplicationName(),
        )
    }

    private fun getApplicationName(): String {
        val packageName = applicationContext.packageName
        return when {
            packageName.contains(".sl") -> SPICE.SIERRA_LEONE.name
            else -> SPICE.AFRICA.name
        }
    }

    /**
     * method to initialize preference
     */
    private fun initPreference() {
        SecuredPreference
            .build(packageName, applicationContext)
    }

    /**
     * method to print debug and release logs
     */
    private fun initTimber() {
        isDebug { debug ->
            if (debug) {
                Timber.plant(Timber.DebugTree())
            } else {
                Timber.plant(CrashReportingTree())
            }
        }
    }

    private fun getUserJourneyAnalytics() {
        SecuredPreference.getString(AnalyticsDefinedParams.SessionId)?.let {
            getUserJourneyList()
        } ?: run {
            UserDetail.referenceId = UUID.randomUUID().toString()
//            UserDetail.role = SecuredPreference.getRole().toString()
            SecuredPreference.putString(AnalyticsDefinedParams.SessionId, UserDetail.referenceId)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun getUserJourneyList() {
        GlobalScope.launch {
            val list = analyticsRepository.getUserJourneyAnalytics()
            val userAnalytics = groupingBySessionId(list)
            UserDetail.referenceId = UUID.randomUUID().toString()
            UserDetail.role = SecuredPreference.getRole() ?: ""
            SecuredPreference.putString(
                AnalyticsDefinedParams.SessionId,
                UserDetail.referenceId,
            )

            userAnalytics?.second?.forEach {
                AnalyticsUtils.setUserJourneyData(
                    userId = it.value[0].userID,
                    eventName = AnalyticsDefinedParams.SessionTracking,
                    referenceId = it.key,
                    userJourney = it.value,
                    analyticsRepository = analyticsRepository,
                    lastSyncedAt = lastSyncDate(),
                )
            }
            analyticsRepository.deleteAllUserJourneys(UserDetail.referenceId)
        }
    }

    private fun lastSyncDate(): String {
        val lastSyncedAt =
            SecuredPreference.getString(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
        return lastSyncedAt ?: "--"
    }

    private fun groupingBySessionId(list: List<UserJourneyAnalytics>): Pair<String, MutableMap<String, List<ScreenDetails>>>? {
        return when {
            list.isNotEmpty() -> {
                Pair(
                    list[0].userId,
                    list
                        .groupBy(UserJourneyAnalytics::sessionId)
                        .mapValues { (_, analyticsList) ->
                            analyticsList.map {
                                ScreenDetails(
                                    it.userJourney,
                                    it.startTime ?: "",
                                    it.userId,
                                    it.userRole,
                                )
                            }
                        }.toMutableMap(),
                )
            }

            else -> {
                return null
            }
        }
    }

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(hiltWorkerFactory).build()
}
