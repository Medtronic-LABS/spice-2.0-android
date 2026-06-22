package org.medtroniclabs.uhis.ui.household

import android.content.Intent
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.activity.viewModels
import dagger.hilt.android.AndroidEntryPoint
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.model.UserDetail
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.appextensions.startBackgroundOfflineSync
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.DefinedParams.FhirMemberID
import org.medtroniclabs.uhis.common.DefinedParams.IS_MEMBER_REGISTRATION
import org.medtroniclabs.uhis.common.DefinedParams.VILLAGE_ID
import org.medtroniclabs.uhis.common.DefinedParams.isCreateHouseholdForPhu
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.databinding.ActivityHouseholdRegistrationBinding
import org.medtroniclabs.uhis.network.resource.ResourceState
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.household.fragment.HouseHoldRegistrationFragment
import org.medtroniclabs.uhis.ui.household.summary.HouseholdSummaryActivity
import org.medtroniclabs.uhis.ui.household.viewmodel.HouseRegistrationViewModel
import org.medtroniclabs.uhis.ui.landing.OnDialogDismissListener
import org.medtroniclabs.uhis.ui.member.MemberRegistrationFragment

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
                if (validateFormInputs()) {
                    backNavigation()
                } else {
                    logExitEventAnalytics()
                }
            },
        )

        intent.getStringExtra(DefinedParams.KeySignature)?.let {
            householdRegistrationViewModel.signatureFilename = it
        }

        intent.getStringExtra(DefinedParams.KEY_INITIAL)?.let {
            householdRegistrationViewModel.initialValue = it
        }

        UserDetail.eventName = AnalyticsDefinedParams.HouseholdCreation
        initializeView()
        attachObserver()
        // phuMemberRegistration()
    }

    override fun consumeImeInsets() = true

    private fun validateFormInputs(): Boolean {
        val fragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (fragment is HouseHoldRegistrationFragment) {
            return fragment.getHouseHoldEnteredInputs()
        } else if (fragment is MemberRegistrationFragment) {
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
                logExitEventAnalytics()
            }
        }
    }

    private fun logExitEventAnalytics() {
        householdRegistrationViewModel.setAnalyticsData(
            UserDetail.startDateTime,
            eventType = AnalyticsDefinedParams.HouseholdCreation,
            eventName = householdRegistrationViewModel.eventName,
            exitReason = AnalyticsDefinedParams.BackButtonClicked,
            isCompleted = false,
        )
        this@HouseholdActivity.finish()
    }

    private fun getCurrentLocation() {
        val locationManager = SpiceLocationManager(this)
        locationManager.getCurrentLocation {
            householdRegistrationViewModel.setCurrentLocation(it)
        }
    }

    private fun initializeView() {
        val householdId = intent.getLongExtra(DefinedParams.householdId, -1L)
        val isMemberRegistration = intent.getBooleanExtra(IS_MEMBER_REGISTRATION, false)
        val memberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1L)
        val memberFhirId = intent.getLongExtra(FhirMemberID, -1L)
        val isCreateHouseholdForPhu = intent.getBooleanExtra(isCreateHouseholdForPhu, false)
        val isPhuWalkInsFlow = intent.getBooleanExtra(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, false)

        householdRegistrationViewModel.isMemberRegistration = isMemberRegistration
        householdRegistrationViewModel.householdId = householdId
        householdRegistrationViewModel.memberID = memberId
        householdRegistrationViewModel.isCreateHouseholdForPhu = isCreateHouseholdForPhu
        householdRegistrationViewModel.isPhuWalkInsFlow = isPhuWalkInsFlow

        if (isCreateHouseholdForPhu) {
            val arguments = Bundle()
            val vId = intent.getLongExtra(VILLAGE_ID, -1L)
            if (vId != -1L) {
                arguments.putLong(VILLAGE_ID, vId)
            }
            launchHouseholdRegistration(arguments)
        } else if (isMemberRegistration || memberId != -1L) {
            val arguments = Bundle().apply {
                putBoolean(AnalyticsDefinedParams.AddNewMember, true)
                putBoolean(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, isPhuWalkInsFlow)
                putLong(DefinedParams.MEMBER_ID, memberId)
                putLong(FhirMemberID, memberFhirId)
                putString(
                    AnalyticsDefinedParams.StartDate,
                    UserDetail.startDateTime,
                )
            }
            launchMemberRegistrationFragment(arguments)
        } else {
            launchHouseholdRegistration(Bundle())
        }
    }

    private fun launchHouseholdRegistration(args: Bundle) {
        setTitle(getString(R.string.household_registration))
        replaceFragmentInId<HouseHoldRegistrationFragment>(
            binding.fragmentContainer.id,
            bundle = args,
            tag = HouseHoldRegistrationFragment::class.simpleName,
        )
    }

    private fun launchMemberRegistrationFragment(arg: Bundle) {
        setTitle(getString(R.string.member_registration))
        replaceFragmentInId<MemberRegistrationFragment>(
            binding.fragmentContainer.id,
            bundle = arg,
            tag = MemberRegistrationFragment::class.simpleName,
        )
    }

    private fun launchHouseholdSummaryPage() {
        val intent = Intent(this, HouseholdSummaryActivity::class.java)
        intent.putExtra(
            DefinedParams.householdId,
            householdRegistrationViewModel.householdId,
        )
        intent.putExtra(HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION, false)
        startActivity(intent)
        finish()
    }

    private fun attachObserver() {
        householdRegistrationViewModel.houseHoldRegistrationLiveData.observe(this@HouseholdActivity) { resource ->
            when (resource.state) {
                ResourceState.LOADING -> {
                    showLoading()
                }

                ResourceState.SUCCESS -> {
                    hideLoading()
                    if (householdRegistrationViewModel.householdId != -1L) {
                        launchHouseholdSummaryPage()
                    } else {
                        val memberId = intent.getLongExtra(DefinedParams.MEMBER_ID, -1L)
                        val memberFhirId = intent.getLongExtra(FhirMemberID, -1L)
                        val isPhuWalkInsFlow = intent.getBooleanExtra(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, false)
                        val arguments = Bundle().apply {
                            putBoolean(AnalyticsDefinedParams.AddNewMember, true)
                            putBoolean(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, isPhuWalkInsFlow)
                            putLong(DefinedParams.MEMBER_ID, memberId)
                            putLong(FhirMemberID, memberFhirId)
                            putString(
                                AnalyticsDefinedParams.StartDate,
                                UserDetail.startDateTime,
                            )
                        }
                        // Check if this is the first member (household head registration)
                        val isFirstMember = !householdRegistrationViewModel.isMemberRegistration &&
                            memberId == -1L &&
                            householdRegistrationViewModel.householdEntityDetail != null
                        if (isFirstMember) {
                            setTitle(getString(R.string.household_head_registration))
                        }
                        launchMemberRegistrationFragment(arguments)
                    }
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
                    launchHouseholdSummaryPage()
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

    override fun onResume() {
        super.onResume()
        getCurrentLocation()
    }
}
