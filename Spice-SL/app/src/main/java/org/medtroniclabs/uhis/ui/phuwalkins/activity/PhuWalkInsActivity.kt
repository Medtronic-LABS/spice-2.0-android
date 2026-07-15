package org.medtroniclabs.uhis.ui.phuwalkins.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import org.medtroniclabs.uhis.R
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.LINKPATIENT
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREEN
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREENCALLBUTTON
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREENHOUSEHOLDLISTLINK
import org.medtroniclabs.uhis.app.analytics.utils.AnalyticsDefinedParams.PHUWALKINSCREENLINKBUTTON
import org.medtroniclabs.uhis.common.DefinedParams
import org.medtroniclabs.uhis.common.SecuredPreference
import org.medtroniclabs.uhis.common.SpiceLocationManager
import org.medtroniclabs.uhis.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import org.medtroniclabs.uhis.databinding.ActivityPhuWalkInsBinding
import org.medtroniclabs.uhis.db.response.HouseHoldEntityWithLastActivity
import org.medtroniclabs.uhis.ui.BaseActivity
import org.medtroniclabs.uhis.ui.MenuConstants
import org.medtroniclabs.uhis.ui.followup.FollowUpMyPatientActivity
import org.medtroniclabs.uhis.ui.household.HouseholdDefinedParams
import org.medtroniclabs.uhis.ui.household.summary.HouseholdSummaryActivity
import org.medtroniclabs.uhis.ui.landing.LandingActivity
import org.medtroniclabs.uhis.ui.medicalreview.utils.MedicalReviewDefinedParams
import org.medtroniclabs.uhis.ui.member.MemberRegistrationFragment
import org.medtroniclabs.uhis.ui.phuwalkins.fragment.PhuLinkedHouseHoldListFragment
import org.medtroniclabs.uhis.ui.phuwalkins.fragment.PhuWalkInsListFragment
import org.medtroniclabs.uhis.ui.phuwalkins.listener.PhuLinkCallback
import org.medtroniclabs.uhis.ui.phuwalkins.viewmodel.PhuWalkInsViewModel

class PhuWalkInsActivity : BaseActivity(), View.OnClickListener, PhuLinkCallback {
    private lateinit var binding: ActivityPhuWalkInsBinding
    private val viewModel: PhuWalkInsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupBinding()
        setupMainContentView()
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        val isLinkBtnDialog = intent.getBooleanExtra("PhuFlow", false)
        if (isLinkBtnDialog) {
            val args = Bundle().apply {
                putBoolean(MedicalReviewDefinedParams.MEDICAL_REVIEW_ADD_MEMBER, true)
            }
            replaceFragmentInId<MemberRegistrationFragment>(
                binding.phuListFragment.id,
                bundle = args,
                tag = MemberRegistrationFragment::class.simpleName,
            )
        } else {
            setTitle(getString(R.string.phu_walk_ins_title))
            val phuWalkInsListFragment =
                PhuWalkInsListFragment.newInstance()
            phuWalkInsListFragment.setDataCallback(this@PhuWalkInsActivity)
            addReplaceFragment(R.id.phuListFragment, phuWalkInsListFragment)
            viewModel.setUserJourney(PHUWALKINSCREEN)
        }
    }

    private fun setupBinding() {
        binding = ActivityPhuWalkInsBinding.inflate(layoutInflater)
    }

    private fun setupMainContentView() {
        setMainContentView(
            binding.root,
            true,
            getString(R.string.patient_medical_review),
            homeAndBackVisibility = Pair(true, true),
            callback = {
                backNavigation()
            },
            callbackHome = {
                viewModel.setUserJourney(AnalyticsDefinedParams.HomeButtonClicked)
                backNavigationToHome()
            },
        )
    }

    private fun backNavigationToHome() {
        val phuFragments =
            getFragmentById(supportFragmentManager, (R.id.phuListFragment))
        if (phuFragments is PhuWalkInsListFragment) {
            homeScreenClick()
        } else {
            showErrorDialogue(
                getString(R.string.alert),
                getString(R.string.exit_reason),
                isNegativeButtonNeed = true,
            ) { isPositive ->
                if (isPositive) {
                    homeScreenClick()
                }
            }
        }
    }

    private fun getCurrentLocation() {
        SpiceLocationManager(this).getCurrentLocation {
//            viewModel.lastLocation = it
        }
    }

    override fun onClick(v: View?) {
    }

    private fun addReplaceFragment(
        containerId: Int,
        fragment: Fragment,
    ) {
        val existingFragment = getFragmentById(supportFragmentManager, containerId)
        supportFragmentManager.commit {
            if (existingFragment == null) {
                add(containerId, fragment)
            } else {
                replace(containerId, fragment)
            }
        }
    }

    override fun onLinkClicked(patientLinkedDetails: Any) {
        if (patientLinkedDetails is UnAssignedHouseholdMemberDetail) {
            viewModel.setUserJourney(LINKPATIENT)
            setTitle(getString(R.string.link_patient))
            viewModel.memberID = patientLinkedDetails.lMemberId.toLong()
            viewModel.fhirMemberID = patientLinkedDetails.memberId.toLong()
            viewModel.setUserJourney(PHUWALKINSCREENHOUSEHOLDLISTLINK)
            val phuLinkedHouseHoldListFragment =
                PhuLinkedHouseHoldListFragment.newInstance(patientLinkedDetails)
            addReplaceFragment(R.id.phuListFragment, phuLinkedHouseHoldListFragment)
        } else if (patientLinkedDetails is HouseHoldEntityWithLastActivity) {
            val intent = Intent(this, HouseholdSummaryActivity::class.java)
            intent.putExtra(
                DefinedParams.householdId,
                patientLinkedDetails.id,
            )
            intent.putExtra(
                DefinedParams.MEMBER_ID,
                viewModel.memberID,
            )
            intent.putExtra(
                DefinedParams.FhirMemberID,
                viewModel.fhirMemberID,
            )
            intent.putExtra(HouseholdDefinedParams.IS_FROM_HOUSEHOLD_REGISTRATION, false)
            intent.putExtra(HouseholdDefinedParams.IS_PHU_WALK_INS_FLOW, true)
            startActivity(intent)
            viewModel.setUserJourney(PHUWALKINSCREENLINKBUTTON)
        }
    }

    private val dialerLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK || result.resultCode == RESULT_CANCELED) {
                viewModel.saveCallHistory()
            }
        }

    override fun onCallClicked(patientLinkedDetails: UnAssignedHouseholdMemberDetail) {
        viewModel.memberID = patientLinkedDetails.lMemberId.toLong()
        SecuredPreference.putLong(
            DefinedParams.HOUSE_HOLD_LINK_START_TIMING,
            System.currentTimeMillis(),
        )
        val dialIntent = Intent(Intent.ACTION_DIAL)
        dialIntent.data = Uri.parse("tel:${patientLinkedDetails.phoneNumber}")
        dialerLauncher.launch(dialIntent)
        viewModel.setUserJourney(PHUWALKINSCREENCALLBUTTON)
    }

    fun backNavigation() {
        onBackPress()
    }

    private val onBackPressedCallback: OnBackPressedCallback =
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPress()
            }
        }

    private fun onBackPress() {
        val phuFragments =
            getFragmentById(supportFragmentManager, (R.id.phuListFragment))
        if (phuFragments is PhuWalkInsListFragment) {
            setTitle(getString(R.string.phu_walk_ins_title))
            val intent = Intent(this, FollowUpMyPatientActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            intent.putExtra(MenuConstants.MY_PATIENTS_MENU_ID, MenuConstants.MY_PATIENTS_MENU_ID)
            startActivity(intent)
            finish()
        } else if (phuFragments is PhuLinkedHouseHoldListFragment) {
            val phuWalkInsListFragment =
                PhuWalkInsListFragment.newInstance()
            phuWalkInsListFragment.setDataCallback(this@PhuWalkInsActivity)
            addReplaceFragment(R.id.phuListFragment, phuWalkInsListFragment)
        }
    }

    private fun homeScreenClick() {
        val intent = Intent(this, LandingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

    override fun onResume() {
        super.onResume()
        hideLoading()
    }
}
