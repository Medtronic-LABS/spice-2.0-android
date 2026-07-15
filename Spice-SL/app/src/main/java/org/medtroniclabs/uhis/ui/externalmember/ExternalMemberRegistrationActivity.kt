package org.medtroniclabs.uhis.ui.externalmember

import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.CommonUtils
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.databinding.ActivityExternalMemberRegistrationBinding
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseRegistrationViewModel
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener

@AndroidEntryPoint
class ExternalMemberRegistrationActivity : BaseActivity(), OnDialogDismissListener {
    private lateinit var binding: ActivityExternalMemberRegistrationBinding

    private val householdRegistrationViewModel: HouseRegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityExternalMemberRegistrationBinding.inflate(layoutInflater)
        val toolbarTitle =
            if (CommonUtils.isFoPoOrChcp()) {
                getString(R.string.member_registration)
            } else {
                getString(R.string.external_member_registration)
            }
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = toolbarTitle,
            homeAndBackVisibility = Pair(true, true),
            callback = {
                if (validateFormInputs()) {
                    backNavigation()
                } else {
                    onBackPressPopStack()
                }
            },
            callbackHome = {
                backNavigationToHome()
            },
        )
        initializeView()
    }

    override fun consumeImeInsets() = true

    private fun validateFormInputs(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is ExternalMemberRegistrationFragment) {
            return fragment.getEnteredInputs()
        }
        return false
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                onBackPressPopStack()
            }
        }
    }

    private fun backNavigationToHome() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true,
        ) { isPositive ->
            if (isPositive) {
                householdRegistrationViewModel.setUserJourney(AnalyticsDefinedParams.ONHOMEBUTTONTRIGGERED)
                startActivityWithoutSplashScreen()
            }
        }
    }

    private fun onBackPressPopStack() {
        this@ExternalMemberRegistrationActivity.finish()
    }

    private fun initializeView() {
        replaceFragmentInId<ExternalMemberRegistrationFragment>(
            binding.fragmentContainer.id,
            bundle = Bundle(),
            tag = ExternalMemberRegistrationFragment::class.simpleName,
        )
    }

    override fun onDialogDismissListener(isFinish: Boolean) {
        finish()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                backNavigation()
            }
        }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            householdRegistrationViewModel.setCurrentLocation(it)
        }
    }

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }
}
