package com.medtroniclabs.opensource.ui.household

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.app.analytics.model.UserDetail
import com.medtroniclabs.opensource.app.analytics.utils.AnalyticsDefinedParams
import com.medtroniclabs.opensource.appextensions.startBackgroundOfflineSync
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.DefinedParams.FhirMemberID
import com.medtroniclabs.opensource.common.DefinedParams.MemberID
import com.medtroniclabs.opensource.common.DefinedParams.isMemberRegistration
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.databinding.ActivityHouseholdRegistrationBinding
import com.medtroniclabs.opensource.network.resource.ResourceState
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.household.fragment.HouseHoldRegistrationFragment
import com.medtroniclabs.opensource.ui.household.summary.HouseholdSummaryActivity
import com.medtroniclabs.opensource.ui.household.viewmodel.HouseRegistrationViewModel
import com.medtroniclabs.opensource.ui.landing.OnDialogDismissListener
import com.medtroniclabs.opensource.ui.member.MemberRegistrationFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

@AndroidEntryPoint
class HouseholdActivity : BaseActivity(), OnDialogDismissListener {

    private lateinit var binding: ActivityHouseholdRegistrationBinding

    private val householdRegistrationViewModel: HouseRegistrationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
        binding = ActivityHouseholdRegistrationBinding.inflate(layoutInflater)
        setMainContentView(
            binding.root,
            isToolbarVisible = true,
            title = getString(R.string.household_registration),
            callback = {
                backNavigation()
            }
        )

        intent.getStringExtra(DefinedParams.KeySignature)?.let {
            householdRegistrationViewModel.signatureFilename = it
        }

        intent.getStringExtra(DefinedParams.KeyInitial)?.let {
            householdRegistrationViewModel.initialValue = it
        }

        UserDetail.eventName=AnalyticsDefinedParams.HouseholdCreation
        householdRegistrationViewModel.setUserJourney(
            getString(R.string.household_registration)
        )
        initializeView()
        attachObserver()
        getCurrentLocation()
        phuMemberRegistration()
    }

    private fun backNavigation() {
        showErrorDialogue(
            getString(R.string.alert),
            getString(R.string.exit_reason),
            isNegativeButtonNeed = true
        ) { isPositive ->
            if (isPositive) {
                householdRegistrationViewModel.setAnalyticsData(
                    UserDetail.startDateTime,
                    eventType = AnalyticsDefinedParams.HouseholdCreation,
                    eventName = householdRegistrationViewModel.eventName,
                    exitReason = AnalyticsDefinedParams.BackButtonClicked,
                    isCompleted = false
                )
                this@HouseholdActivity.finish()
            }
        }
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            householdRegistrationViewModel.setCurrentLocation(it)
        }
    }

    private fun initializeView() {
        householdRegistrationViewModel.isMemberRegistration =
            intent.getBooleanExtra(isMemberRegistration, false)
        householdRegistrationViewModel.householdId =
            intent.getLongExtra(HouseholdDefinedParams.ID, -1L)
        Timber.d("Member id is not showing 4 ${intent.getLongExtra(MemberID, -1L)}")
        householdRegistrationViewModel.memberID = intent.getLongExtra(MemberID, -1L)
        loadFragment(if (householdRegistrationViewModel.isMemberRegistration || (householdRegistrationViewModel.memberID != -1L)) 2 else 1)
    }


    private fun loadFragment(status: Int) {
        when (status) {
            1 -> {
                setTitle(getString(R.string.household_registration))
                replaceFragmentInId<HouseHoldRegistrationFragment>(
                    binding.fragmentContainer.id,
                    tag = HouseHoldRegistrationFragment::class.simpleName
                )
            }

            2 -> {
                val arguments = Bundle().apply {
                    putBoolean(AnalyticsDefinedParams.AddNewMember, true)
                    putString(
                        AnalyticsDefinedParams.StartDate,
                        UserDetail.startDateTime
                    )
                }
                setTitle(getString(R.string.member_registration))
                replaceFragmentInId<MemberRegistrationFragment>(
                    binding.fragmentContainer.id,
                    bundle = arguments,
                    tag = MemberRegistrationFragment::class.simpleName
                )
            }

            3 -> {
                val intent = Intent(this, HouseholdSummaryActivity::class.java)
                intent.putExtra(
                    HouseholdDefinedParams.ID,
                    householdRegistrationViewModel.householdId
                )
                intent.putExtra(HouseholdDefinedParams.isFromHouseHoldRegistration, false)
                startActivity(intent)
                finish()
            }
        }
    }


    private inline fun <reified fragment : Fragment> replaceFragmentInId(
        id: Int? = null,
        bundle: Bundle? = null,
        tag: String? = null
    ) {
        supportFragmentManager.commit {
            setReorderingAllowed(true)
            replace<fragment>(
                id ?: binding.fragmentContainer.id,
                args = bundle,
                tag = tag
            )
        }
    }


    private fun attachObserver() {
        householdRegistrationViewModel.houseHoldRegistrationLiveData.observe(this@HouseholdActivity) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (householdRegistrationViewModel.householdId != -1L)
                        loadFragment(3)
                    else
                        loadFragment(2)
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }

        householdRegistrationViewModel.houseHoldUpdateLiveData.observe(this@HouseholdActivity) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    startBackgroundOfflineSync()
                    loadFragment(3)
                }

                ResourceState.ERROR -> {
                    hideLoading()
                }
            }
        }
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

    private fun phuMemberRegistration() {
        val isPhuWalkInsFlow=intent.getBooleanExtra(HouseholdDefinedParams.isPhuWalkInsFlow,false)
        if (isPhuWalkInsFlow) {
            householdRegistrationViewModel.isMemberRegistration =true
            val arguments = Bundle().apply {
                putBoolean(AnalyticsDefinedParams.AddNewMember, true)
                putBoolean(HouseholdDefinedParams.isPhuWalkInsFlow,true)
                putLong(MemberID,intent.getLongExtra(MemberID,-1L))
                putLong(FhirMemberID,intent.getLongExtra(FhirMemberID,-1L))
            }
            setTitle(getString(R.string.member_registration))
            replaceFragmentInId<MemberRegistrationFragment>(
                binding.fragmentContainer.id,
                bundle = arguments,
                tag = MemberRegistrationFragment::class.simpleName
            )
    }
    }
}