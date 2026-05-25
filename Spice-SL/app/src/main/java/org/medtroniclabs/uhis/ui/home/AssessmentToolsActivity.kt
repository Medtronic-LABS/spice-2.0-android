package org.medtroniclabs.uhis.ui.home

import android.content.Intent
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.ActivityToolsBinding
import org.medtroniclabs.uhis.ncd.medicalreview.NCDMRUtil.EncounterReference
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.landing.LandingActivity

@AndroidEntryPoint
class AssessmentToolsActivity : BaseActivity() {
    private lateinit var binding: ActivityToolsBinding

    private val toolsViewModel: ToolsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityToolsBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.assessment),
            homeAndBackVisibility = Pair(true, true),
            callbackHome = {
                toolsViewModel.setUserJourney(AnalyticsDefinedParams.ONHOMEBUTTONTRIGGERED)
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            },
        )
        initializeView()
    }

    override fun onResume() {
        super.onResume()
        toolsViewModel.setUserJourney(AnalyticsDefinedParams.ASSESSMENTTOOLSELECTION)
    }

    private fun initializeView() {
        toolsViewModel.selectedHouseholdId = intent.getLongExtra(DefinedParams.HOUSEHOLD_ID, -1)
        toolsViewModel.selectedHouseholdMemberID = intent.getLongExtra(DefinedParams.MEMBER_ID, -1)
        toolsViewModel.selectedMemberDob = intent.getStringExtra(DefinedParams.DOB)
        toolsViewModel.followUpId = intent.getLongExtra(DefinedParams.FOLLOW_UP_ID, -1)
        toolsViewModel.entryPoint = intent.getStringExtra(DefinedParams.ENTRY_POINT)
        val isDeepLink = intent.getBooleanExtra(DefinedParams.IS_DEEP_LINK, false)
        val bundle = Bundle()
        bundle.putString(DefinedParams.FhirId, intent.getStringExtra(DefinedParams.FhirId))
        bundle.putString(DefinedParams.ORIGIN, intent.getStringExtra(DefinedParams.ORIGIN))
        bundle.putString(DefinedParams.GENDER, intent.getStringExtra(DefinedParams.GENDER))
        bundle.putBoolean(MenuConstants.FOLLOW_UP, intent.getBooleanExtra(MenuConstants.FOLLOW_UP, false))
        bundle.putBoolean(DefinedParams.IS_DEEP_LINK, isDeepLink)
        // only for Africa encounterReference is visit id(Support landscape )
        intent.getStringExtra(EncounterReference)?.let { encounterReference ->
            bundle.putString(EncounterReference, encounterReference)
            requestedOrientation = if (encounterReference.isNotBlank()) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        } ?: run {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        val fragmentTag =
            ToolsMenuFragment.TAG
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            fragment = ToolsMenuFragment.newInstance()
            fragment.arguments = bundle
            supportFragmentManager
                .beginTransaction()
                .add(R.id.menuItemsFragment, fragment, fragmentTag)
                .commit()
        }
    }
}
