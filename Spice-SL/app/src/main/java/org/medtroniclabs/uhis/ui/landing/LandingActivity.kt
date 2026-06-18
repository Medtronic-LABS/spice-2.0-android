package org.medtroniclabs.uhis.ui.landing

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.GravityCompat
import androidx.core.view.forEach
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import com.medtroniclabs.microcoaching.MicroCoachingSDK
import com.medtroniclabs.microcoaching.ModelDownloadStrategy
import com.medtroniclabs.microcoaching.ai.model.ModelProvider
import com.medtroniclabs.microcoaching.domain.decision.CoachingMode
import com.medtroniclabs.microcoaching.sherpa.SherpaOnnxStt
import org.medtroniclabs.uhis.BuildConfig
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.SpiceBaseApplication
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.upload.UploadWorker
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils.getAppVersion
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsUtils.updateUserIdIfEmpty
import org.medtroniclabs.uhis.appextensions.WORKER_UNIQUE_NAME
import org.medtroniclabs.uhis.appextensions.WORKER_UNIQUE_NAME_FOR_NCD
import org.medtroniclabs.uhis.appextensions.cancelAllWorker
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.isVisible
import org.medtroniclabs.uhis.appextensions.setError
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.appextensions.triggerOneTimeWorker
import org.medtroniclabs.uhis.appextensions.visible
import org.medtroniclabs.uhis.common.ApiManager
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.REFRESH_FRAGMENT
import org.medtroniclabs.uhis.common.GeneralErrorDialog
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.TransferStatusEnum
import org.medtroniclabs.uhis.databinding.ActivityLandingBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferNotificationCountRequest
import org.medtroniclabs.uhis.ncd.data.NCDPatientTransferUpdateRequest
import org.medtroniclabs.uhis.ncd.data.NCDSupportRequest
import org.medtroniclabs.uhis.ncd.data.PatientTransfer
import org.medtroniclabs.uhis.ncd.data.PatientTransferListResponse
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationRequest
import org.medtroniclabs.uhis.ncd.data.PeerSupervisorNotificationResponse
import org.medtroniclabs.uhis.ncd.landing.dialog.LanguagePreferenceDialog
import org.medtroniclabs.uhis.ncd.landing.dialog.NCDOfflineDataDialog
import org.medtroniclabs.uhis.ncd.landing.dialog.NCDSupportDialogFragment
import org.medtroniclabs.uhis.ncd.landing.dialog.NCDSupportDialogListener
import org.medtroniclabs.uhis.ncd.landing.ui.UserTermsConditionsActivity
import org.medtroniclabs.uhis.ncd.landing.viewmodel.NCDOfflineDataViewModel
import org.medtroniclabs.uhis.network.NetworkConstants
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.ChooseSiteDialogueFragment
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.PrivacyPolicyFragment
import org.medtroniclabs.uhis.ui.boarding.LoginActivity
import org.medtroniclabs.uhis.ui.coaching.CoachingAssistantActivity
import org.medtroniclabs.uhis.ui.home.HomeScreenFragment
import org.medtroniclabs.uhis.ui.landing.adapter.PeerSupervisorNotificationAdapter
import org.medtroniclabs.uhis.ui.landing.viewmodel.LandingViewModel
import org.medtroniclabs.uhis.ui.landing.viewmodel.LanguagePreferenceViewModel
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientSearchFragment
import org.medtroniclabs.uhis.ui.mypatients.viewmodel.PatientDetailViewModel
import org.medtroniclabs.uhis.ui.patientTransfer.NCDApproveRejectListener
import org.medtroniclabs.uhis.ui.patientTransfer.adapter.NCDIncomingRequestAdapter
import org.medtroniclabs.uhis.ui.patientTransfer.adapter.NCDInformationMessageAdapter
import org.medtroniclabs.uhis.ui.patientTransfer.dialog.NCDPatientDetailDialogue
import java.util.UUID
import java.util.concurrent.TimeUnit
import android.net.ConnectivityManager as AndroidConnectivityManager

private const val LANDING_TAG = "LandingActivity"

class LandingActivity :
    BaseActivity(),
    NavigationView.OnNavigationItemSelectedListener,
    DrawerLayout.DrawerListener,
    View.OnClickListener,
    OnDialogDismissListener,
    NCDApproveRejectListener,
    NCDSupportDialogListener {
    lateinit var binding: ActivityLandingBinding

    private val viewModel: LandingViewModel by viewModels()
    private val offlineDataViewModel: NCDOfflineDataViewModel by viewModels()
    private val patientViewModel: PatientDetailViewModel by viewModels()
    private val languageViewModel: LanguagePreferenceViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { true }

        super.onCreate(savedInstanceState)

        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val isLoggedIn =
            SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.ISLOGGEDIN.name) ||
                SecuredPreference.getBoolean(
                    SecuredPreference.EnvironmentKey.ISOFFLINELOGIN.name,
                )
        val isMetaLoaded =
            SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.ISMETALOADED.name)

        if (!(isLoggedIn && isMetaLoaded)) {
            startActivity(Intent(this, LoginActivity::class.java))
            splashScreen.setKeepOnScreenCondition { false }
            finish()
            return
        } else {
            if (CommonUtils.isNonCommunity()) {
                val isFromLauncher: Boolean =
                    intent?.categories?.contains(Intent.CATEGORY_LAUNCHER) ?: false
                if (isFromLauncher && !SecuredPreference.getTermsAndConditionsStatus()) {
                    startActivity(Intent(this, UserTermsConditionsActivity::class.java))
                    finish()
                    return
                }
            }
        }

        // Initiate schedule worker
        startSyncWorker()

        // screening and assessment sync
        offlineDataViewModel.getCountOfflineData()
        binding = ActivityLandingBinding.inflate(layoutInflater)
        splashScreen.setKeepOnScreenCondition { false }
        setContentView(binding.root)
        // Since landing activity is setting content with setContentView,
        // applying insets separately for this screen only
        CommonUtils.applyInsets(
            this,
            binding.root,
            binding.fakeStatusBar,
            binding.fakeNavBar,
            false,
        )
        if (CommonUtils.isNonCommunity()) {
            languageViewModel.getCultures()
        } else {
            val menu = binding.navView.menu
            //     menu.findItem(R.id.switch_language)?.let { menu.removeItem(it.itemId) }
        }
        initializeDrawerView()
        initializeHomeViews()
        updateSideBarFooter()
        schedulePeriodicUploadWork(this)
        onClickUploadLog()
        UserDetail.updateUserIdIfEmpty(SecuredPreference.getUserId().toString())
        UserDetail.getAppVersion(BuildConfig.VERSION_NAME)
        attachObserver()

        // Re-build the MicroCoaching SDK with the freshly issued JWT (it was
        // initialised with an empty token in SpiceBaseApplication.onCreate before login).
        reinitCoachingSdkWithToken()

        // Deeplink for directly goes to Search patient
        patientSearchDeepLink()
    }

    /**
     * Re-build the MicroCoaching SDK so its [authToken], language, and (potentially)
     * model path reflect the post-login state. Called from [onCreate] after auth is
     * confirmed. No-op if the token is still missing or the SDK was not initialised
     * in the Application class.
     */
    private fun reinitCoachingSdkWithToken() {
        val token = SecuredPreference.getString(SecuredPreference.EnvironmentKey.TOKEN.name)
        if (token.isNullOrEmpty() || !MicroCoachingSDK.isInitialized()) return
        val modelDir = getExternalFilesDir(null)
        val existingModel = modelDir
            ?.listFiles()
            ?.firstOrNull { it.extension == "task" || it.extension == "litertlm" }
        val downloadStrategy = if (existingModel != null) {
            ModelDownloadStrategy.PROVIDED
        } else {
            ModelDownloadStrategy.ON_FIRST_USE
        }
        MicroCoachingSDK
            .Builder(applicationContext)
            .language(SpiceBaseApplication.spiceLanguageToSdkLanguage(SecuredPreference.getCultureName()))
            .backendUrl(BuildConfig.COACHING_BACKEND_URL)
            .authToken(token)
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
            .forceMode(CoachingMode.EDGE)
            .build()
        MicroCoachingSDK.getInstance().syncCoordinator.schedulePeriodic()
        // Drawer item is `visible="false"` in XML — reveal it now that the SDK has a token.
        binding.navView.menu
            .findItem(R.id.chwAssistant)
            ?.isVisible = true
    }

    /**
     * Drawer-tap handler for "CHW Assistant". If the on-device LLM model is already
     * staged, launch [CoachingAssistantActivity] directly. Otherwise prompt the CHW to
     * download (~600 MB) — with a second confirmation if currently on metered (mobile)
     * data — then fire the SDK download and surface a Snackbar so the user knows the
     * chat will open once the model finishes downloading.
     */
    private fun launchCoachingAssistant() {
        if (!MicroCoachingSDK.isInitialized()) return
        val sdk = MicroCoachingSDK.getInstance()
        // Low-end devices (< 3 GB RAM) run the chat in retrieval-only mode —
        // no AI model is required, so skip the download prompt entirely.
        if (sdk.isLowEndDevice || sdk.modelManager.isModelPresent()) {
            CoachingAssistantActivity.launch(this)
        } else {
            showCoachingModelDownloadPrompt()
        }
    }

    /**
     * Single-dialog model-download confirmation. The previous two-dialog flow
     * (general prompt → metered warning → trigger) was the failure surface in
     * a QA report — the second dialog's positive callback was being dropped
     * on some devices, leaving the user on the landing screen with no
     * feedback. The metered-network hint is now baked into the message
     * string so the user gives a single explicit yes.
     */
    private fun showCoachingModelDownloadPrompt() {
        val metered = isOnMeteredNetwork()
        val messageRes = if (metered) {
            R.string.coaching_model_download_message_metered
        } else {
            R.string.coaching_model_download_message
        }
        // Dynamic download size from the SDK's configured model variant, formatted
        // locale-aware — tracks the selected model instead of a hard-coded "~600 MB".
        val sizeLabel = runCatching {
            android.text.format.Formatter.formatShortFileSize(
                this,
                MicroCoachingSDK.getInstance().selectedModelVariant().sizeInBytes,
            )
        }.getOrDefault("")
        showErrorDialogue(
            title = getString(R.string.coaching_model_download_title),
            message = getString(messageRes, sizeLabel),
            isNegativeButtonNeed = true,
            positiveButtonName = getString(R.string.yes),
            cancelBtnName = getString(R.string.no),
        ) { isPositive ->
            Log.i(LANDING_TAG, "ModelDownloadPrompt dismissed — positive=$isPositive metered=$metered")
            if (isPositive) triggerCoachingModelDownload()
        }
    }

    private fun triggerCoachingModelDownload() {
        Log.i(LANDING_TAG, "triggerCoachingModelDownload — calling modelManager.triggerDownload()")
        runCatching { MicroCoachingSDK.getInstance().modelManager.triggerDownload() }
            .onFailure { Log.e(LANDING_TAG, "modelManager.triggerDownload threw", it) }
        Toast.makeText(this, getString(R.string.coaching_download_started), Toast.LENGTH_LONG).show()
    }

    /**
     * Default to `true` (assume metered) when the connectivity manager or the
     * active network is null — a transient null read shouldn't bypass the
     * user's consent step on the rare race where we check right at network
     * handoff.
     */
    private fun isOnMeteredNetwork(): Boolean {
        val cm = getSystemService(AndroidConnectivityManager::class.java) ?: return true
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return true
        return !caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }

    /**
     * Forward connectivity-restored events to the MicroCoaching SDK so it can flush
     * pending telemetry spans and trigger an immediate sync. Idempotent on the SDK
     * side: safe to call on every [onResume].
     */
    private fun notifyCoachingSdkOnConnectivityRestored() {
        if (!MicroCoachingSDK.isInitialized()) return
        if (connectivityManager.isNetworkAvailable()) {
            MicroCoachingSDK.getInstance().onConnectivityRestored()
        }
    }

    private fun syncScreeningAndAssessment() {
        val screening = offlineDataViewModel.screeningCount.value ?: 0
        val assessment = offlineDataViewModel.assessmentType.value ?: 0
        if (CommonUtils.isNonCommunity() && (!CommonUtils.isCha() && screening > 0 || assessment > 0)) {
            withNetworkAvailability(online = {
                this.triggerOneTimeWorker()
                checkBGSyncStatusForNCD()
            }, isErrorShow = false)
        }
    }

    private fun attachObserver() {
        offlineDataViewModel.screeningCount.observe(this) {
            syncScreeningAndAssessment()
        }
        offlineDataViewModel.assessmentType.observe(this) {
        }
        offlineDataViewModel.followUpType.observe(this) {
        }
        viewModel.patientListResponse.observe(this) { resoruceState ->
            when (resoruceState.state) {
                ResourceState.LOADING -> {
                    showHideList(false)
                }

                ResourceState.ERROR -> {
                    showHideList(true)
                }

                ResourceState.SUCCESS -> {
                    showHideList(true)
                    resoruceState.data?.let { data ->
                        loadAdapterData(data)
                    }
                }
            }
        }
        viewModel.patientUpdateResponse.observe(this) { resorceState ->
            when (resorceState.state) {
                ResourceState.LOADING -> {
                    showHideList(false)
                }

                ResourceState.ERROR -> {
                    showHideList(true)
                }

                ResourceState.SUCCESS -> {
                    showHideList(true)
                    binding.drawerLayout.closeDrawer(binding.navNotificationView)
                    resorceState.data?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                if (viewModel.isSupport) {
                                    getString(R.string.alert)
                                } else {
                                    getString(R.string.transfer)
                                },
                                callback = {
                                    viewModel.patientUpdateResponse.setError(message = null)
                                    val dialog = supportFragmentManager.findFragmentByTag(
                                        GeneralErrorDialog.TAG,
                                    ) as? GeneralErrorDialog
                                    dialog?.dismiss()
                                },
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }
            }
        }
        viewModel.patientTransferNotificationCountResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.ERROR -> {
                    binding.appBarMain.tvNotificationCount.gone()
                    hideLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { dataResponse ->
                        val transferCount = dataResponse.patientTransferCount
                        binding.appBarMain.tvNotificationCount.visibility =
                            setTransferCount(transferCount)
                        binding.appBarMain.tvNotificationCount.text =
                            setNotificationCount(transferCount)
                    } ?: kotlin.run {
                        binding.appBarMain.tvNotificationCount.gone()
                    }
                }
            }
        }
        languageViewModel.cultureList.observe(this) { resource ->
            resource.data?.let {
                val menu: Menu = binding.navView.menu
                val switchLanguage: MenuItem? = menu.findItem(R.id.switch_language)
                if (switchLanguage != null && it.size <= 1) {
                    menu.removeItem(switchLanguage.itemId)
                }
            }
        }
        viewModel.supportResponseLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                getString(R.string.alert),
                                callback = {},
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                getString(R.string.alert),
                                callback = {},
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }
            }
        }
        viewModel.cbsNotificationListResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { notifications ->
                        if (!notifications.isNullOrEmpty()) {
                            storeNotificationIds(notifications)
                        } else {
                            binding.CenterProgress.gone()
                            binding.tvNoNotificationsFound.visible()
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                getString(R.string.alert),
                                callback = {},
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }
            }
        }
        viewModel.cbsNotificationUpdateListResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    resourceState.data?.let { notifications ->
                        if (!notifications.isNullOrEmpty()) {
                            showNotificationView(notifications)
                        } else {
                            binding.tvNoNotificationsFound.visible()
                        }
                    }
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                getString(R.string.alert),
                                callback = {},
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }
            }
        }

        viewModel.cbsNotificationUpdateResponse.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    SecuredPreference.removePeerSupervisorToken()
                    finishLogout()
                }

                ResourceState.ERROR -> {
                    hideLoading()
                    resourceState.message?.let {
                        val generalErrorDialog =
                            GeneralErrorDialog.newInstance(
                                getString(R.string.alert),
                                callback = {},
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true),
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG,
                        )
                        if (errorFragment == null) {
                            generalErrorDialog.show(supportFragmentManager, GeneralErrorDialog.TAG)
                        }
                    }
                }
            }
        }
    }

    private fun setTransferCount(transferCount: Long): Int {
        if (transferCount > 0) {
            return View.VISIBLE
        }
        return View.GONE
    }

    private fun setNotificationCount(transferCount: Long): CharSequence {
        if (transferCount > 99) {
            return getString(R.string.notification_plus)
        }
        return transferCount.toString()
    }

    private fun loadAdapterData(data: PatientTransferListResponse) {
        if (data.incomingPatientList.size > 0) {
            binding.rvOutgoingList.visible()
            binding.rvOutgoingList.addItemDecoration(
                DividerItemDecoration(
                    baseContext,
                    LinearLayoutManager.VERTICAL,
                ),
            )
            binding.rvOutgoingList.layoutManager = LinearLayoutManager(this@LandingActivity)
            binding.rvOutgoingList.adapter = NCDIncomingRequestAdapter(data.incomingPatientList, this)
        } else {
            binding.rvOutgoingList.gone()
        }
        if (data.outgoingPatientList.size > 0) {
            binding.rvInformationList.visible()
            binding.rvInformationList.layoutManager = LinearLayoutManager(this@LandingActivity)
            binding.rvInformationList.addItemDecoration(
                DividerItemDecoration(
                    baseContext,
                    LinearLayoutManager.VERTICAL,
                ),
            )
            binding.rvInformationList.adapter =
                NCDInformationMessageAdapter(data.outgoingPatientList, this)
        } else {
            binding.rvInformationList.gone()
        }
        val totalCount = data.incomingPatientList.size + data.outgoingPatientList.size
        if (totalCount > 0) {
            binding.tvNoNotificationsFound.gone()
            binding.tvDialogTitle.text =
                getString(R.string.notification_count, totalCount.toString())
        } else {
            binding.tvNoNotificationsFound.visible()
            binding.tvDialogTitle.text = getString(R.string.notification)
        }
    }

    private fun showHideList(status: Boolean) {
        if (status) {
            binding.CenterProgress.gone()
            binding.rvOutgoingList.visible()
            binding.rvOutgoingList.visible()
        } else {
            binding.CenterProgress.visible()
            binding.rvOutgoingList.gone()
            binding.rvInformationList.gone()
        }
    }

    private fun onClickUploadLog() {
        if (BuildConfig.BUILD_TYPE == "debug" || BuildConfig.BUILD_TYPE == "staging" || BuildConfig.BUILD_TYPE == "training") {
            binding.uploadLog.setOnClickListener {
                val uploadWorkRequest = OneTimeWorkRequest
                    .Builder(UploadWorker::class.java)
                    .setInputData(periodicUploaderInputData())
                    .setConstraints(
                        Constraints
                            .Builder()
                            .setRequiredNetworkType(NetworkType.CONNECTED)
                            .build(),
                    ).build()
                WorkManager.getInstance(this).enqueue(uploadWorkRequest)
            }
        } else {
            binding.uploadLog.gone()
        }
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backHandelFlow()
            }
        }

    private fun backHandelFlow() {
        viewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventName = AnalyticsDefinedParams.HouseholdCreation,
            exitReason = AnalyticsDefinedParams.CancelButtonClicked,
            isCompleted = false,
        )
        if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        } else {
            val currentFragment: Fragment? =
                supportFragmentManager.findFragmentById(R.id.fragmentContainerView)
            if (currentFragment is PrivacyPolicyFragment) {
                if (currentFragment.canGoBack()) {
                    currentFragment.goBack()
                } else {
                    onNavigationItemSelected(binding.navView.menu.findItem(R.id.home))
                }
            } else {
                finish()
            }
        }
    }

    /**
     * method to initialize home view toolbar and views
     */
    private fun initializeHomeViews() {
        if (CommonUtils.isNonCommunity()) {
            binding.navNotificationView.visible()
            binding.appBarMain.clNotification.visible()
        } else {
            if (CommonUtils.isPeerSuperVisor()) {
                binding.uploadLog.gone()
                binding.appBarMain.clNotification.visible()
                binding.tvDialogTitle.text = getString(R.string.notifications)
            }
        }
        binding.appBarMain.ivNotification.safeClickListener(this)
        binding.appBarMain.tvNotificationCount.safeClickListener(this)

        val isNotificationVisible =
            CommonUtils.isNCDProvider() || CommonUtils.isPhysicianPrescriber() || CommonUtils.isPeerSuperVisor()
        if (isNotificationVisible) {
            binding.appBarMain.ivNotification.visible()
        } else {
            binding.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END,
            )
            binding.appBarMain.ivNotification.gone()
        }
        binding.ivClose.safeClickListener(this)
    }

    private fun startSyncWorker() {
        if (CommonUtils.isChw() || (CommonUtils.isNonCommunity() && CommonUtils.isChp())) {
            startBackgroundOfflineSync()
            checkBGSyncStatus()
        }
    }

    private fun initializeDrawerView() {
        val menu: Menu = binding.navView.menu
        val menuItemToRemove: MenuItem? = menu.findItem(R.id.offline_sync)
        val changeFacilityMenuItem: MenuItem? = menu.findItem(R.id.changeFacility)
        if (CommonUtils.isCommunity() && !CommonUtils.isChw() && menuItemToRemove != null) {
            menu.removeItem(menuItemToRemove.itemId)
        }
        if (CommonUtils.isCommunity() && !CommonUtils.isProvider() && changeFacilityMenuItem != null) {
            menu.removeItem(changeFacilityMenuItem.itemId)
        }

        if (CommonUtils.isNonCommunity()) {
            menuItemToRemove?.let {
                if (CommonUtils.isTiberbuUser() || CommonUtils.isCha()) {
                    menu.removeItem(it.itemId)
                }
            }
            changeFacilityMenuItem?.let {
                if (CommonUtils.isChp() || CommonUtils.isCha()) {
                    menu.removeItem(it.itemId)
                }
            }
        }

        onNavigationItemSelected(binding.navView.menu.findItem(R.id.home))
        val toolBar = binding.appBarMain.toolbar
        val toggle = ActionBarDrawerToggle(
            this,
            binding.drawerLayout,
            toolBar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close,
        )
        binding.drawerLayout.addDrawerListener(toggle)
        binding.drawerLayout.addDrawerListener(this)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
        if (CommonUtils.isNonCommunity()) {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED, GravityCompat.END)
        } else {
            binding.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED, GravityCompat.END)
        }
    }

    private fun updateSideBarFooter() {
        binding.appBarBottom.tvAppVersion.text = this.getString(
            R.string.firstname_lastname,
            getString(R.string.app_version),
            getBuildVersion(),
        )
    }

    private fun getBuildVersion(): String {
        val index = BuildConfig.VERSION_NAME.indexOf('-')
        return if (index != -1) {
            BuildConfig.VERSION_NAME.substring(0, index)
        } else {
            BuildConfig.VERSION_NAME
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                if (ApiManager.isLoading.value) {
                    Toast.makeText(this, getString(R.string.please_wait_until_current_operation_completes), Toast.LENGTH_SHORT).show()

                    Handler(Looper.getMainLooper()).postDelayed({
                        val homeMenuItem = binding.navView.menu.findItem(R.id.home)
                        selectNavigationMenu(homeMenuItem)
                    }, 200)
                } else {
                    if (binding.appBarMain.includeMainContent.syncingHolder
                            .isVisible()
                    ) {
                        showSyncInProgressWarning()
                        return true
                    } else {
                        showLogoutDialogFlow()
                    }
                }
            }

            R.id.profile -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val profileDialogFragment =
                    supportFragmentManager.findFragmentByTag(ProfileDialogFragment.TAG)
                profileDialogFragment ?: ProfileDialogFragment
                    .newInstance()
                    .show(supportFragmentManager, ProfileDialogFragment.TAG)
                return true
            }

            R.id.offline_sync -> {
                if (CommonUtils.isCommunity()) {
                    goToOfflineSyncPage()
                } else {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                    val ncdOfflineDataDialog =
                        supportFragmentManager.findFragmentByTag(NCDOfflineDataDialog.TAG)
                    ncdOfflineDataDialog ?: NCDOfflineDataDialog.newInstance().show(
                        supportFragmentManager,
                        NCDOfflineDataDialog.TAG,
                    )
                }
                return true
            }

            R.id.privacy_policy -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                if (connectivityManager.isNetworkAvailable()) {
                    binding.appBarMain.tvTitle.text = getString(R.string.privacy_policy)
                    supportFragmentManager
                        .beginTransaction()
                        .replace(
                            R.id.fragmentContainerView,
                            PrivacyPolicyFragment(),
                            PrivacyPolicyFragment::class.simpleName,
                        ).commit()
                } else {
                    showErrorDialogue(
                        getString(R.string.error),
                        getString(R.string.no_internet_error),
                        false,
                    ) {}
                }
            }

            R.id.changeFacility -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val chooseSiteDialogueFragment = ChooseSiteDialogueFragment.newInstance()
                chooseSiteDialogueFragment.show(
                    supportFragmentManager,
                    ChooseSiteDialogueFragment.TAG,
                )
                return true
            }

            R.id.switch_language -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val languagePreferenceDialog =
                    LanguagePreferenceDialog.newInstance(languagePreferenceListener)
                languagePreferenceDialog.show(
                    supportFragmentManager,
                    LanguagePreferenceDialog.TAG,
                )
                return true
            }

            R.id.external_member -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val intent = Intent(this, org.medtroniclabs.uhis.ui.services.ServicesActivity::class.java)
                intent.putExtra("isExternalMember", true)
                startActivity(intent)
                return true
            }

            R.id.support -> {
                // TODO : Handle the tiberbu
                launchSupportDialogFragment()
                return true
            }

            R.id.chwAssistant -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                launchCoachingAssistant()
                return true
            }
        }
        selectNavigationMenu(item)
        displayScreen(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private fun launchSupportDialogFragment() {
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        val supportDialogFragment =
            supportFragmentManager.findFragmentByTag(NCDSupportDialogFragment.TAG)
        supportDialogFragment ?: NCDSupportDialogFragment.newInstance().show(
            supportFragmentManager,
            NCDSupportDialogFragment.TAG,
        )
    }

    private val languagePreferenceListener = object : OnDialogDismissListener {
        override fun onDialogDismissListener(isFinish: Boolean) {
            showErrorDialogue(
                message = getString(R.string.language_change_alert),
                isNegativeButtonNeed = true,
                cancelBtnName = getString(R.string.no),
                positiveButtonName = getString(R.string.yes),
            ) { isPositiveResult ->
                if (isPositiveResult && SecuredPreference.logout()) {
                    cancelAllWorker()
                    startActivity(Intent(this@LandingActivity, LoginActivity::class.java))
                    finish()
                    UserDetail.referenceId = UUID.randomUUID().toString()
                }
            }
        }
    }

    private fun goToOfflineSyncPage() {
        if (binding.appBarMain.includeMainContent.syncingHolder
                .isVisible()
        ) {
            showSyncInProgressWarning()
        } else {
            startActivity(Intent(this, OfflineSyncActivity::class.java))
            binding.drawerLayout.closeDrawer(GravityCompat.START)
        }
    }

    private fun showSyncInProgressWarning() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.background_sync_in_progress),
            isNegativeButtonNeed = false,
        ) {}
    }

    private fun displayScreen(id: Int) {
        when (id) {
            R.id.home -> {
                handleNavigation()
            }
        }
    }

    private fun handleNavigation(isDeepLink: Boolean = false) {
        if (CommonUtils.isCommunity() && CommonUtils.isRolePresent()) {
            binding.appBarMain.tvTitle.text = getString(R.string.search_patient)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            val bundle = Bundle().apply {
                putString(DefinedParams.ORIGIN, MenuConstants.MY_PATIENTS_MENU_ID)
            }
            replaceFragmentIfExists<PatientSearchFragment>(
                R.id.fragmentContainerView,
                bundle = bundle,
                tag = PatientSearchFragment.TAG,
            )
        } else {
            binding.appBarMain.tvTitle.text = getString(R.string.home_title)
            if (CommonUtils.isChwChp()) {
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
            val bundle = Bundle().apply {
                putBoolean(DefinedParams.IsDeepLink, isDeepLink)
            }
            replaceFragmentInId<HomeScreenFragment>(
                R.id.fragmentContainerView,
                bundle = bundle,
                tag = HomeScreenFragment.TAG,
            )
        }
    }

    private fun selectNavigationMenu(item: MenuItem) {
        if (binding.navView.menu.size() > 0) {
            binding.navView.menu.forEach { menuItem ->
                menuItem.isChecked = (menuItem.itemId == item.itemId)
            }
        }
    }

    override fun onDrawerSlide(
        drawerView: View,
        slideOffset: Float,
    ) {
    }

    override fun onDrawerOpened(drawerView: View) {
        when (drawerView.id) {
            R.id.nav_notification_view -> {
                if (CommonUtils.isNonCommunity()) {
                    viewModel.getPatientListTransfer(NCDPatientTransferNotificationCountRequest(SecuredPreference.getOrganizationId().toString()))
                } else {
                    if (CommonUtils.isCommunity() && CommonUtils.isPeerSuperVisor()) {
                        viewModel.notificationIsViewed = true
                        val request = PeerSupervisorNotificationRequest(
                            userId = SecuredPreference.getUserId().toString(),
                        )
                        viewModel.getCBSUpdatedNotificationList(request)
                    } else {
                        viewModel.notificationIsViewed = false
                    }
                }
            }
        }
    }

    private fun doRefreshForDataUpdate() {
        if (CommonUtils.isNCDProvider() || CommonUtils.isPhysicianPrescriber()) {
            viewModel.patientTransferNotificationCount(
                NCDPatientTransferNotificationCountRequest(
                    SecuredPreference.getOrganizationId().toString(),
                ),
            )
        }
    }

    override fun onDrawerClosed(drawerView: View) {
        when (drawerView.id) {
            R.id.nav_notification_view -> {
                doRefreshForDataUpdate()
            }
        }
    }

    override fun onDrawerStateChanged(newState: Int) {
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.ivNotification, R.id.tvNotificationCount -> {
                binding.drawerLayout.openDrawer(binding.navNotificationView)
            }

            R.id.ivClose -> {
                binding.drawerLayout.closeDrawer(binding.navNotificationView)
            }
        }
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        val homeMenuItem = binding.navView.menu.findItem(R.id.home)
        onNavigationItemSelected(homeMenuItem)
        if (isFinish) {
            if (CommonUtils.isNonCommunity()) {
                withNetworkAvailability(online = {
                    this.triggerOneTimeWorker()
                    // i added chp condition inside the method
                    startSyncWorker()
                })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        val refreshFragment =
            intent.getBooleanExtra(REFRESH_FRAGMENT, false)
        if (refreshFragment) {
            if (CommonUtils.isNonCommunity()) {
                supportFragmentManager.fragments.forEach { fragment ->
                    supportFragmentManager.beginTransaction().remove(fragment).commit()
                }
                handleNavigation()
            } else {
                val fragment = supportFragmentManager.findFragmentByTag(PatientSearchFragment.TAG)
                fragment?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
                replaceFragmentInId<PatientSearchFragment>(
                    R.id.fragmentContainerView,
                    tag = PatientSearchFragment.TAG,
                )
            }
        }
    }

    private fun checkBGSyncStatus() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWorkLiveData(WORKER_UNIQUE_NAME).observe(this) {
            if (!it.isNullOrEmpty() && it[0].state == WorkInfo.State.RUNNING) {
                binding.appBarMain.includeMainContent.syncingHolder
                    .visible()
            } else {
                binding.appBarMain.includeMainContent.syncingHolder
                    .gone()
            }
        }
    }

    private fun schedulePeriodicUploadWork(context: Context) {
        val periodicRequest =
            PeriodicWorkRequestBuilder<UploadWorker>(60, TimeUnit.MINUTES)
                .setInputData(periodicUploaderInputData())
                .setInitialDelay(0, TimeUnit.SECONDS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()

        Log.i("Analytics", "Periodic Request Added : ")
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "UploadWorker",
            ExistingPeriodicWorkPolicy.REPLACE,
            periodicRequest,
        )
    }

    private fun periodicUploaderInputData(): Data =
        Data
            .Builder()
            .apply {
                putString(
                    DefinedParams.BaseUrl,
                    NetworkConstants.BASE_URL,
                )
                putString(
                    DefinedParams.BuildConfigs,
                    BuildConfig.BUILD_TYPE,
                )
                putAll(
                    mapOf(
                        DefinedParams.Authorization to SecuredPreference.getString(
                            SecuredPreference.EnvironmentKey.TOKEN.toString(),
                        ),
                    ),
                )
            }.build()

    override fun onResume() {
        super.onResume()
        doRefreshForDataUpdate()
        notifyCoachingSdkOnConnectivityRestored()
    }

    private fun checkBGSyncStatusForNCD() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWorkLiveData(WORKER_UNIQUE_NAME_FOR_NCD).observe(this) {
            if (!it.isNullOrEmpty() && it[0].state == WorkInfo.State.RUNNING) {
                binding.appBarMain.includeMainContent.syncingHolder
                    .visible()
            } else {
                binding.appBarMain.includeMainContent.syncingHolder
                    .gone()
            }
        }
    }

    override fun onTransferStatusUpdate(
        status: String,
        transfer: PatientTransfer,
    ) {
        when (status) {
            TransferStatusEnum.REJECTED.name -> {
                showAlertDialogWithComments(
                    getString(R.string.reject),
                    message = getString(R.string.reject_confirmation),
                    true,
                    errorMessage = getString(R.string.valid_reason),
                    buttonName = Pair(
                        getString(R.string.ok),
                        getString(R.string.cancel),
                    ),
                ) { isPositiveResult, rejectionReason ->
                    if (isPositiveResult) {
                        viewModel.setAnalyticsData(
                            UserDetail.startDateTime,
                            eventName = AnalyticsDefinedParams.NCDTransferStatus + " " + TransferStatusEnum.REJECTED.name,
                            isCompleted = true,
                        )
                        viewModel.patientTransferUpdate(
                            NCDPatientTransferUpdateRequest(
                                transfer.id,
                                transferStatus = status,
                                rejectReason = rejectionReason,
                                memberReference = transfer.patient.id,
                                transferSite = transfer.transferSite,
                            ),
                        )
                    }
                }
            }

            else -> {
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDTransferStatus + " " + status,
                    isCompleted = true,
                )
                viewModel.patientTransferUpdate(
                    NCDPatientTransferUpdateRequest(
                        transfer.id,
                        transferStatus = status,
                        memberReference = transfer.patient.id,
                        transferSite = transfer.transferSite,
                    ),
                )
            }
        }
    }

    override fun onViewDetail(patientID: Long) {
        NCDPatientDetailDialogue
            .newInstance(patientID)
            .show(supportFragmentManager, NCDPatientDetailDialogue.TAG)
    }

    override fun onSubmitClicked(message: String?) {
        if (connectivityManager.isNetworkAvailable()) {
            message?.let {
                viewModel.isSupport = true
                val request =
                    NCDSupportRequest(
                        userId = SecuredPreference.getUserId().toString(),
                        summary = it,
                        healthFacilityId = SecuredPreference.getOrganizationId().toLong(),
                    )
                viewModel.createSupportRequest(request)
            }
        } else {
            showErrorDialogue(
                getString(R.string.error),
                getString(R.string.no_internet_error),
                isNegativeButtonNeed = false,
            ) {}
        }
    }

    private fun patientSearchDeepLink() {
        val data: Uri? = intent?.data
        val isDeepLink = data?.getQueryParameter("isDeepLink")
        if (isDeepLink == "true") {
            handleNavigation(true)
            // Perform necessary action when deep link is detected
        }
    }

    private fun showNotificationView(notificationResponses: ArrayList<PeerSupervisorNotificationResponse>) {
        binding.tvNoNotificationsFound.gone()
        val newIds = notificationResponses.map { it.id }
        val oldIds = SecuredPreference.notificationIds
        if (oldIds != newIds) {
            SecuredPreference.notificationIds = newIds
            binding.appBarMain.notificationDot.visible()
        } else {
            binding.appBarMain.notificationDot.gone()
        }
        val adapter = PeerSupervisorNotificationAdapter()
        adapter.setData(notificationResponses)
        binding.rvNotifications.layoutManager = LinearLayoutManager(this)
        binding.rvNotifications.adapter = adapter
    }

    private fun storeNotificationIds(notificationResponses: ArrayList<PeerSupervisorNotificationResponse>) {
        SecuredPreference.notificationIds = notificationResponses.map { it.id }
        if (!SecuredPreference.notificationIds.isNullOrEmpty()) {
            binding.appBarMain.notificationDot.visible()
        } else {
            binding.appBarMain.notificationDot.gone()
        }
    }

    override fun onStart() {
        super.onStart()
        if (CommonUtils.isCommunity() && CommonUtils.isPeerSuperVisor()) {
            val request = PeerSupervisorNotificationRequest(
                userId = SecuredPreference.getUserId().toString(),
            )
            viewModel.getCBSNotificationList(request)
        }
    }

    private fun showLogoutDialogFlow() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.logout_alert),
            positiveButtonName = getString(R.string.yes),
            cancelBtnName = getString(R.string.no),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (CommonUtils.isCommunity() && CommonUtils.isPeerSuperVisor()) {
                SecuredPreference.putString(
                    SecuredPreference.EnvironmentKey.PEER_SUPERVISOR_NOTIFICATION_TOKEN.name,
                    SecuredPreference.getString(SecuredPreference.EnvironmentKey.TOKEN.toString()),
                )
            }

            val isNetworkAvailable = connectivityManager.isNetworkAvailable()

            if (isPositive && SecuredPreference.logout(isNetworkAvailable)) {
                if (CommonUtils.isPeerSuperVisor()) {
                    if (!isNetworkAvailable) {
                        Handler(Looper.getMainLooper()).postDelayed({
                            showErrorDialogue(
                                getString(R.string.error),
                                getString(R.string.no_internet_error),
                                isNegativeButtonNeed = false,
                            ) {}
                        }, 600)
                    } else {
                        if (SecuredPreference.notificationIds != null && viewModel.notificationIsViewed) {
                            viewModel.updateCBSNotification()
                        } else {
                            SecuredPreference.removePeerSupervisorToken()
                            finishLogout()
                        }
                    }
                } else {
                    finishLogout()
                }
            } else {
                val homeMenuItem = binding.navView.menu.findItem(R.id.home)
                selectNavigationMenu(homeMenuItem)
            }
        }
    }

    private fun finishLogout() {
        viewModel.setUserJourney(AnalyticsDefinedParams.LOGOUT)
        cancelAllWorker()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
