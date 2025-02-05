package com.medtroniclabs.opensource.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.databinding.ActivityMedicalReviewToolsBinding
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import com.medtroniclabs.opensource.ui.mypatients.fragment.PatientMenuFragment
import dagger.hilt.android.AndroidEntryPoint

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
                val intent = Intent(this, LandingActivity::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                startActivity(intent)
                finish()
            }
        )
        initializeView()
    }

    private fun initializeView() {
        val fragmentTag =
            PatientMenuFragment.TAG
        var fragment = supportFragmentManager.findFragmentByTag(fragmentTag)
        if (fragment == null) {
            fragment = PatientMenuFragment.newInstance(
                intent.getStringExtra(DefinedParams.PatientId),
                intent.getStringExtra(DefinedParams.ID),
                intent.getStringExtra(DefinedParams.Gender),
                intent.getStringExtra(DefinedParams.DOB),
                childPatientId =intent.getStringExtra(DefinedParams.ChildPatientId),
                dateOfDelivery = intent.getStringExtra(DefinedParams.DateOfDelivery),
                intent.getStringExtra(DefinedParams.NeonateOutcome)
            )
            setTitle(
                intent.getStringExtra(DefinedParams.MenuTitle)
                    ?: getString(R.string.search_patient)
            )
            supportFragmentManager.beginTransaction()
                .add(R.id.menuItemsFragment, fragment, fragmentTag)
                .commit()
        }
    }
}