package com.medtroniclabs.opensource.ncd.screening.ui

import android.Manifest
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.isFineAndCoarseLocationPermissionGranted
import com.medtroniclabs.opensource.appextensions.isGpsEnabled
import com.medtroniclabs.opensource.common.CommonUtils
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.databinding.ActivityScreeningBinding
import com.medtroniclabs.opensource.ncd.screening.fragment.GeneralDetailsFragment
import com.medtroniclabs.opensource.ncd.screening.fragment.ScreeningFormBuilderFragment
import com.medtroniclabs.opensource.ncd.screening.fragment.ScreeningSummaryFragment
import com.medtroniclabs.opensource.ncd.screening.fragment.StatsFragment
import com.medtroniclabs.opensource.ncd.screening.viewmodel.ScreeningFormBuilderViewModel
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ncd.registration.fragment.TermsAndConditionsFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ScreeningActivity : BaseActivity() {

    private lateinit var binding: ActivityScreeningBinding
    private val viewModel: ScreeningFormBuilderViewModel by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fixOrientation()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityScreeningBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.screening),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            }
        )
        initView()
        setAnalytics()
    }

    private fun setAnalytics() {
        UserDetail.eventName = AnalyticsDefinedParams.ScreeningCreation
        viewModel.setUserJourney(AnalyticsDefinedParams.ScreeningCreation)
    }

    private fun fixOrientation() {
        requestedOrientation =
            if (CommonUtils.checkIsTablet(this)) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    }

    private fun initView() {
        replaceFragmentIfExists<GeneralDetailsFragment>(
            R.id.screeningParentLayout,
            bundle = null,
            tag = GeneralDetailsFragment.TAG
        )
    }

    private val onBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            backNavigation()
        }
    }

    fun backNavigation() {
        val fragmentManager = supportFragmentManager

        when (fragmentManager.findFragmentById(R.id.screeningParentLayout)) {
            is GeneralDetailsFragment, is ScreeningSummaryFragment -> startActivityWithoutSplashScreen()
            is StatsFragment -> {
                if (viewModel.screeningSaveResponse.value?.data != null) {
                    startActivityWithoutSplashScreen()
                } else {
                    replaceFragment<GeneralDetailsFragment>(GeneralDetailsFragment.TAG)
                }
            }

            is TermsAndConditionsFragment -> replaceFragment<StatsFragment>(StatsFragment.TAG)
            is ScreeningFormBuilderFragment -> replaceFragment<TermsAndConditionsFragment>(
                TermsAndConditionsFragment.TAG
            )

            else -> {

            }
        }
    }

    private inline fun <reified T : Fragment> replaceFragment(tag: String) {
        replaceFragmentIfExists<T>(
            R.id.screeningParentLayout,
            bundle = null,
            tag = tag
        )
    }

    fun ableToGetLocation(): Boolean {
        //Check Location service is enabled
        if (!isGpsEnabled()) {
            showTurnOnGPSDialog(isNegativeButtonNeed = true)
            return false
        }

        //Check Location permission for limit exceed
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            showAllowLocationServiceDialog(isNegativeButtonNeed = true)
            return false
        }

        //Check Location permission
        if (!isFineAndCoarseLocationPermissionGranted()) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return false
        }

        return true
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val finePermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION]
            val coarsePermission = permissions[Manifest.permission.ACCESS_COARSE_LOCATION]

            if (finePermission == true && coarsePermission == true) {
                SpiceLocationManager(this).getCurrentLocation {
                    viewModel.setCurrentLocation(it)
                }
            }
        }
}