package org.medtroniclabs.uhis.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.databinding.ActivityMedicalReviewToolsBinding
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.mypatients.fragment.PatientMenuFragment

@AndroidEntryPoint
class MedicalReviewToolsActivity : BaseActivity() {
    private lateinit var binding: ActivityMedicalReviewToolsBinding
    private val toolsViewModel: ToolsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMedicalReviewToolsBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.search_patient),
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
        toolsViewModel.setUserJourney(AnalyticsDefinedParams.MEDICALREVIEWTOOLS)
    }

    private fun initializeView() {
        val fragmentTag =
            PatientMenuFragment.TAG
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            fragment = PatientMenuFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                intent.getStringExtra(DefinedParams.ID),
                intent.getStringExtra(DefinedParams.MEMBER_ID),
                intent.getStringExtra(DefinedParams.GENDER),
                intent.getStringExtra(DefinedParams.DOB),
                childPatientId = intent.getStringExtra(DefinedParams.CHILD_PATIENT_ID),
                dateOfDelivery = intent.getStringExtra(DefinedParams.DateOfDelivery),
                intent.getStringExtra(DefinedParams.NEONATE_OUTCOME),
                villageId = intent.getStringExtra(DefinedParams.villageId),
                householdId = intent.getStringExtra(DefinedParams.householdId),
                isPregnant = intent.getBooleanExtra(DefinedParams.isPregnant, false),
                isEMTCTFlow = intent.getBooleanExtra(DefinedParams.EMTCT, false),
                hivTestedPositive = intent.getBooleanExtra(DefinedParams.HIV_TESTED_POSITIVE, false),
            )
            setTitle(
                intent.getStringExtra(DefinedParams.MenuTitle)
                    ?: getString(R.string.search_patient),
            )
            supportFragmentManager
                .beginTransaction()
                .add(R.id.menuItemsFragment, fragment, fragmentTag)
                .commit()
        }
    }
}
