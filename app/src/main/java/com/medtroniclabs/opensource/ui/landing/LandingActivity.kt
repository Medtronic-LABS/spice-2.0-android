package com.medtroniclabs.opensource.ui.landing

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
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
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequest
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.android.material.navigation.NavigationView
import com.medtroniclabs.opensource.BuildConfig
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.upload.UploadWorker
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils.getAppVersion
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils.getFileUploadTime
import com.medtroniclabs.opensource.app.analytics.utils.CommonUtils.updateUserIdIfEmpty
import com.medtroniclabs.opensource.appextensions.cancelAllWorker
import com.medtroniclabs.opensource.appextensions.gone
import com.medtroniclabs.opensource.appextensions.isVisible
import com.medtroniclabs.opensource.appextensions.setError
import com.medtroniclabs.opensource.appextensions.startBackgroundOfflineSync
import com.medtroniclabs.opensource.appextensions.triggerOneTimeWorker
import com.medtroniclabs.opensource.appextensions.visible
import com.medtroniclabs.opensource.appextensions.workerUniqueName
import com.medtroniclabs.opensource.appextensions.workerUniqueNameForNCD
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.REFRESH_FRAGMENT
import com.medtroniclabs.opensource.common.GeneralErrorDialog
import com.medtroniclabs.opensource.common.RoleConstant
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferNotificationCountRequest
import com.medtroniclabs.opensource.databinding.ActivityLandingBinding
import com.medtroniclabs.opensource.formgeneration.extension.safeClickListener
import com.medtroniclabs.opensource.ncd.landing.dialog.NCDOfflineDataDialog
import com.medtroniclabs.opensource.ncd.landing.ui.UserTermsConditionsActivity
import com.medtroniclabs.opensource.network.NetworkConstants
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.ChooseSiteDialogueFragment
import com.medtroniclabs.opensource.ui.PrivacyPolicyFragment
import com.medtroniclabs.opensource.ui.boarding.LoginActivity
import com.medtroniclabs.opensource.ui.home.HomeScreenFragment
import com.medtroniclabs.opensource.ui.landing.viewmodel.LandingViewModel
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientSearchFragment
import com.medtroniclabs.opensource.ui.patientTransfer.NCDApproveRejectListener
import com.medtroniclabs.opensource.ui.patientTransfer.adapter.NCDIncomingRequestAdapter
import com.medtroniclabs.opensource.ui.patientTransfer.adapter.NCDInformationMessageAdapter
import com.medtroniclabs.opensource.ui.patientTransfer.dialog.NCDPatientDetailDialogue
import com.medtroniclabs.opensource.ncd.data.PatientTransferListResponse
import com.medtroniclabs.opensource.ncd.data.NCDPatientTransferUpdateRequest
import com.medtroniclabs.opensource.common.TransferStatusEnum
import com.medtroniclabs.opensource.ncd.data.PatientTransfer
import com.medtroniclabs.opensource.ncd.landing.dialog.LanguagePreferenceDialog
import com.medtroniclabs.opensource.ncd.landing.viewmodel.NCDOfflineDataViewModel
import com.medtroniclabs.opensource.ui.landing.viewmodel.LanguagePreferenceViewModel
import com.medtroniclabs.opensource.ui.mypatients.viewmodel.PatientDetailViewModel
import java.util.concurrent.TimeUnit


class LandingActivity : BaseActivity(), NavigationView.OnNavigationItemSelectedListener,
    DrawerLayout.DrawerListener, View.OnClickListener, OnDialogDismissListener,
    NCDApproveRejectListener {

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
            SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.ISLOGGEDIN.name) || SecuredPreference.getBoolean(
                SecuredPreference.EnvironmentKey.ISOFFLINELOGIN.name
            )
        val isMetaLoaded =
            SecuredPreference.getBoolean(SecuredPreference.EnvironmentKey.ISMETALOADED.name)

        if (!(isLoggedIn && isMetaLoaded)) {
            startActivity(Intent(this, LoginActivity::class.java))
            splashScreen.setKeepOnScreenCondition { false }
            finish()
            return
        } else {
            if (CommonUtils.isNonCommunity() ) {
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
        languageViewModel.getCultures()
        initializeDrawerView()
        initializeHomeViews()
        updateSideBarFooter()
        schedulePeriodicUploadWork(this)
        onClickUploadLog()
        UserDetail.updateUserIdIfEmpty(SecuredPreference.getUserId().toString())
        UserDetail.getAppVersion(BuildConfig.VERSION_NAME)
        attachObserver()
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
                                getString(R.string.transfer),
                                callback = {
                                    viewModel.patientUpdateResponse.setError(message = null)
                                    val dialog = supportFragmentManager.findFragmentByTag(
                                        GeneralErrorDialog.TAG) as? GeneralErrorDialog
                                    dialog?.dismiss()
                                },
                                this,
                                false,
                                okayButton = getString(R.string.ok),
                                messageBtnData = Pair(it, true)
                            )
                        val errorFragment = supportFragmentManager.findFragmentByTag(
                            GeneralErrorDialog.TAG)
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
    }

    private fun setTransferCount(transferCount: Long): Int {
        if (transferCount > 0)
            return View.VISIBLE
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
                    LinearLayoutManager.VERTICAL
                )
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
                    LinearLayoutManager.VERTICAL
                )
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
    private fun onClickUploadLog(){
        if (BuildConfig.BUILD_TYPE=="staging"){
        binding.uploadLog.setOnClickListener {
            val uploadWorkRequest = OneTimeWorkRequest.Builder(UploadWorker::class.java)
                .setInputData(periodicUploaderInputData())
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                ).build()
                WorkManager.getInstance(this).enqueue(uploadWorkRequest)
        }
            }else{
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
            isCompleted = false
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
        if (CommonUtils.isNonCommunity() ) {
            binding.navNotificationView.visible()
            binding.appBarMain.clNotification.visible()
        }
        binding.appBarMain.ivNotification.safeClickListener(this)
        binding.appBarMain.tvNotificationCount.safeClickListener(this)
        val isNotificationVisible =
            CommonUtils.isNCDProvider() || CommonUtils.isPhysicianPrescriber()
        if (isNotificationVisible) {
            binding.appBarMain.ivNotification.visible()
        } else {
            binding.drawerLayout.setDrawerLockMode(
                DrawerLayout.LOCK_MODE_LOCKED_CLOSED,
                GravityCompat.END
            )
            binding.appBarMain.ivNotification.gone()
        }
        binding.ivClose.safeClickListener(this)
    }

    private fun startSyncWorker() {
        val userRole = SecuredPreference.getUserDetails()?.roles?.joinToString { it.name }
        if (userRole != null) {
            // add chp
            if (userRole.contains(RoleConstant.COMMUNITY_HEALTH_WORKER) || (CommonUtils.isNonCommunity() && CommonUtils.isChp())) {
                startBackgroundOfflineSync()
                checkBGSyncStatus()
            }
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
            R.string.navigation_drawer_close
        )
        binding.drawerLayout.addDrawerListener(toggle)
        binding.drawerLayout.addDrawerListener(this)
        toggle.syncState()
        binding.navView.setNavigationItemSelectedListener(this)
    }

    private fun updateSideBarFooter() {
        binding.appBarBottom.tvAppVersion.text = this.getString(
            R.string.firstname_lastname,
            getString(R.string.app_version),
            getBuildVersion()
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
                if (binding.appBarMain.includeMainContent.syncingHolder.isVisible()) {
                    showSyncInProgressWarning()
                    return true
                } else {
                    showErrorDialogue(
                        getString(R.string.alert),
                        getString(R.string.logout_alert),
                        positiveButtonName = getString(R.string.yes),
                        cancelBtnName = getString(R.string.no),
                        isNegativeButtonNeed = true
                    ) { isPositive ->
                        if (isPositive && SecuredPreference.logout()) {
                            cancelAllWorker()
                            startActivity(Intent(this, LoginActivity::class.java))
                            finish()
                        } else {
                            val homeMenuItem = binding.navView.menu.findItem(R.id.home)
                            selectNavigationMenu(homeMenuItem)
                        }
                    }
                }
            }

            R.id.profile -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val profileDialogFragment =
                    supportFragmentManager.findFragmentByTag(ProfileDialogFragment.TAG)
                profileDialogFragment ?: ProfileDialogFragment.newInstance()
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
                        NCDOfflineDataDialog.TAG
                    )
                }
                return true
            }

            R.id.privacy_policy -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                if (connectivityManager.isNetworkAvailable()) {
                    binding.appBarMain.tvTitle.text = getString(R.string.privacy_policy)
                    supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.fragmentContainerView,
                            PrivacyPolicyFragment(),
                            PrivacyPolicyFragment::class.simpleName
                        )
                        .commit()
                } else {
                    showErrorDialogue(
                        getString(R.string.error), getString(R.string.no_internet_error),
                        false
                    ) {}
                }
            }

            R.id.changeFacility -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val chooseSiteDialogueFragment = ChooseSiteDialogueFragment.newInstance()
                chooseSiteDialogueFragment.show(
                    supportFragmentManager,
                    ChooseSiteDialogueFragment.TAG
                )
                return true
            }

            R.id.switch_language -> {
                binding.drawerLayout.closeDrawer(GravityCompat.START)
                val languagePreferenceDialog =
                    LanguagePreferenceDialog.newInstance(languagePreferenceListener)
                languagePreferenceDialog.show(
                    supportFragmentManager,
                    LanguagePreferenceDialog.TAG
                )
                return true
            }
        }
        selectNavigationMenu(item)
        displayScreen(item.itemId)
        binding.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }

    private val languagePreferenceListener = object : OnDialogDismissListener {
        override fun onDialogDismissListener(isFinish: Boolean) {
            showErrorDialogue(
                message = getString(R.string.language_change_alert),
                isNegativeButtonNeed = true,
                cancelBtnName = getString(R.string.no),
                positiveButtonName = getString(R.string.yes)
            ) { isPositiveResult ->
                if (isPositiveResult && SecuredPreference.logout()) {
                    cancelAllWorker()
                    startActivity(Intent(this@LandingActivity, LoginActivity::class.java))
                    finish()
                }
            }
        }
    }

    private fun goToOfflineSyncPage() {
        if (binding.appBarMain.includeMainContent.syncingHolder.isVisible()) {
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
            isNegativeButtonNeed = false
        ) {}
    }

    private fun displayScreen(id: Int) {
        when (id) {
            R.id.home -> {
                handleNavigation()
            }

        }
    }

    private fun handleNavigation() {
        if (CommonUtils.isCommunity() && CommonUtils.isRolePresent()) {
            binding.appBarMain.tvTitle.text = getString(R.string.search_patient)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            replaceFragmentIfExists<PatientSearchFragment>(
                R.id.fragmentContainerView,
                bundle = null,
                tag = PatientSearchFragment.TAG
            )
        } else {
            binding.appBarMain.tvTitle.text = getString(R.string.home_title)
            if (CommonUtils.isChwChp())
                requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            replaceFragmentInId<HomeScreenFragment>(
                R.id.fragmentContainerView,
                tag = HomeScreenFragment.TAG
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

    override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
    }

    override fun onDrawerOpened(drawerView: View) {
        when (drawerView.id) {
            R.id.nav_notification_view -> {
                    viewModel.getPatientListTransfer(NCDPatientTransferNotificationCountRequest(SecuredPreference.getOrganizationId().toString()))
            }
        }
    }

    private fun doRefreshForDataUpdate() {
        if (CommonUtils.isNCDProvider() || CommonUtils.isPhysicianPrescriber()) {
            viewModel.patientTransferNotificationCount(
                NCDPatientTransferNotificationCountRequest(
                    SecuredPreference.getOrganizationId().toString()
                )
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
                    tag = PatientSearchFragment.TAG
                )
            }
        }
    }

    private fun checkBGSyncStatus() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWorkLiveData(workerUniqueName).observe(this) {
            if (!it.isNullOrEmpty() && it[0].state == WorkInfo.State.RUNNING) {
                binding.appBarMain.includeMainContent.syncingHolder.visible()
            } else {
                binding.appBarMain.includeMainContent.syncingHolder.gone()
            }
        }
    }

    private fun schedulePeriodicUploadWork(context: Context) {
        val periodicWorkRequest = PeriodicWorkRequest.Builder(
            UploadWorker::class.java, 1, TimeUnit.DAYS
        ).apply {
            setInputData(periodicUploaderInputData())
            setInitialDelay(getFileUploadTime(), TimeUnit.MILLISECONDS)
            setConstraints(
                Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
            )
        }.build()

        WorkManager.getInstance(context).enqueue(periodicWorkRequest)
    }

    private fun periodicUploaderInputData(): Data {
        return Data.Builder().apply {
            putString(
                DefinedParams.BaseUrl,
                NetworkConstants.BASE_URL
            )
            putString(
                DefinedParams.BuildConfigs,
                BuildConfig.BUILD_TYPE
            )
            putAll(
                mapOf(
                    DefinedParams.Authorization to SecuredPreference.getString(
                        SecuredPreference.EnvironmentKey.TOKEN.toString()
                    )
                )
            )
        }.build()
    }

    override fun onResume() {
        super.onResume()
        doRefreshForDataUpdate()
    }

    private fun checkBGSyncStatusForNCD() {
        val workManager = WorkManager.getInstance(this)
        workManager.getWorkInfosForUniqueWorkLiveData(workerUniqueNameForNCD).observe(this) {
            if (!it.isNullOrEmpty() && it[0].state == WorkInfo.State.RUNNING) {
                binding.appBarMain.includeMainContent.syncingHolder.visible()
            } else {
                binding.appBarMain.includeMainContent.syncingHolder.gone()
            }
        }
    }

    override fun onTransferStatusUpdate(status: String, transfer: PatientTransfer) {
        when (status) {
            TransferStatusEnum.REJECTED.name -> {
                showAlertDialogWithComments(
                    getString(R.string.reject),
                    message = getString(R.string.reject_confirmation),
                    true,
                    errorMessage = getString(R.string.valid_reason),
                    buttonName = Pair(
                        getString(R.string.ok),
                        getString(R.string.cancel)
                    )
                ) { isPositiveResult, rejectionReason ->
                    if (isPositiveResult) {
                        viewModel.setAnalyticsData(
                            UserDetail.startDateTime,
                            eventName = AnalyticsDefinedParams.NCDTransferStatus + " " + TransferStatusEnum.REJECTED.name,
                            isCompleted = true
                        )
                            viewModel.patientTransferUpdate(
                                NCDPatientTransferUpdateRequest(
                                    transfer.id,
                                    transferStatus = status,
                                    rejectReason = rejectionReason,
                                    memberReference = transfer.patient.id,
                                    transferSite = transfer.transferSite
                                )
                            )
                    }
                }
            }

            else -> {
                viewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventName = AnalyticsDefinedParams.NCDTransferStatus + " " + status,
                    isCompleted = true
                )
                viewModel.patientTransferUpdate(
                    NCDPatientTransferUpdateRequest(
                        transfer.id,
                        transferStatus = status,
                        memberReference = transfer.patient.id,
                        transferSite = transfer.transferSite
                    )
                )
            }
        }
    }

    override fun onViewDetail(patientID: Long) {
        NCDPatientDetailDialogue.newInstance(patientID)
            .show(supportFragmentManager, NCDPatientDetailDialogue.TAG)
    }


}