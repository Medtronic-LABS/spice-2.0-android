package org.medtroniclabs.uhis.ui.boarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.appextensions.gone
import org.medtroniclabs.uhis.appextensions.triggerOneTimeWorker
import org.medtroniclabs.uhis.common.AppConstants
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.databinding.ActivityResourceLoadingScreenBinding
import org.medtroniclabs.uhis.formgeneration.extension.safeClickListener
import org.medtroniclabs.uhis.ncd.landing.ui.UserTermsConditionsActivity
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.boarding.viewmodel.ResourceLoadingViewModel
import org.medtroniclabs.uhis.ui.landing.LandingActivity

@AndroidEntryPoint
class ResourceLoadingScreen : BaseActivity() {
    private lateinit var binding: ActivityResourceLoadingScreenBinding
    private val viewModel: ResourceLoadingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityResourceLoadingScreenBinding.inflate(layoutInflater)
        setMainContentView(binding.root)
        initView()
        attachObserver()
        setViewListener()
    }

    private fun initView() {
        viewModel.changeFacility = intent.getBooleanExtra(DefinedParams.changeFacility, false)
        syncAndDownloadInitialData()
    }

    private fun syncAndDownloadInitialData() {
        val isDifferentLogin = intent.getBooleanExtra(AppConstants.IS_DIFFERENT_LOGIN, false)
        if (isDifferentLogin) {
            viewModel.syncOldUserData()
        } else {
            getMetaData()
        }
    }

    private fun attachObserver() {
        viewModel.syncProgressPercent.observe(this) { percent ->
            binding.progressSync.progress = percent
            binding.tvSyncPercent.text = getString(R.string.sync_progress_percent, percent)
        }

        viewModel.oldUserDataSync.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.tvOfflineSyncMessage.gone()
                }
                ResourceState.SUCCESS -> {
                    SecuredPreference.remove(SecuredPreference.EnvironmentKey.SERVER_LAST_SYNCED.name)
                    getMetaData()
                }
                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }

        viewModel.deviceDetailsLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                }

                ResourceState.SUCCESS -> {
                    viewModel.getMetaDataInformation()
                }

                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }

        viewModel.metaDataCompleteLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.tvOfflineSyncMessage.gone()
                }

                ResourceState.SUCCESS -> {
                    if ((CommonUtils.isChw() || CommonUtils.isCHCP()) && CommonUtils.isCommunity()) {
                        viewModel.downloadInitialDetails()
                    } else if (CommonUtils.isNonCommunity() && CommonUtils.isChp()) {
                        viewModel.downloadTheFollowUpData()
                    } else {
                        viewModel.markSyncProgressComplete()
                        launchLandingScreen()
                    }
                }
                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }

        viewModel.householdsLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.tvOfflineSyncMessage.gone()
                }

                ResourceState.SUCCESS -> {
                    launchLandingScreen()
                }

                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }

        viewModel.ncdFollowUpLiveData.observe(this) { resourceState ->
            when (resourceState.state) {
                ResourceState.LOADING -> {
                    binding.tvOfflineSyncMessage.gone()
                }

                ResourceState.SUCCESS -> {
                    launchLandingScreen()
                    if (CommonUtils.isNonCommunity() && connectivityManager.isNetworkAvailable()) {
                        this.triggerOneTimeWorker()
                    }
                }

                ResourceState.ERROR -> {
                    handleError()
                }
            }
        }
    }

    private fun handleError() {
        viewModel.syncProgressPercent.value = 0
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISMETALOADED.name,
            false,
        )
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
            false,
        )
        binding.actionButton.visibility = View.VISIBLE
    }

    private fun setViewListener() {
        binding.actionButton.safeClickListener {
            binding.actionButton.visibility = View.GONE
            getMetaData()
        }
    }

    private fun getMetaData() {
        if (viewModel.changeFacility) {
            viewModel.updateDeviceDetails(this)
        } else {
            viewModel.getMetaDataInformation()
        }
    }

    private fun launchLandingScreen() {
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISLOGGEDIN.name,
            true,
        )
        SecuredPreference.putBoolean(
            SecuredPreference.EnvironmentKey.ISMETALOADED.name,
            true,
        )
        if (CommonUtils.isNonCommunity() && !SecuredPreference.getTermsAndConditionsStatus()) {
            startActivity(Intent(this, UserTermsConditionsActivity::class.java))
        } else {
            startActivity(Intent(this, LandingActivity::class.java))
        }
        finish()
    }
}
