package com.medtroniclabs.opensource.ui.phuwalkins.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.medtroniclabs.opensource.R
import com.medtroniclabs.opensource.common.DefinedParams
import com.medtroniclabs.opensource.common.SecuredPreference
import com.medtroniclabs.opensource.common.SpiceLocationManager
import com.medtroniclabs.opensource.data.offlinesync.model.UnAssignedHouseholdMemberDetail
import com.medtroniclabs.opensource.databinding.ActivityPhuWalkInsBinding
import com.medtroniclabs.opensource.db.response.HouseHoldEntityWithMemberCount
import com.medtroniclabs.opensource.ui.BaseActivity
import com.medtroniclabs.opensource.ui.MenuConstants
import com.medtroniclabs.opensource.ui.followup.FollowUpMyPatientActivity
import com.medtroniclabs.opensource.ui.household.HouseholdDefinedParams
import com.medtroniclabs.opensource.ui.household.summary.HouseholdSummaryActivity
import com.medtroniclabs.opensource.ui.landing.LandingActivity
import com.medtroniclabs.opensource.ui.medicalreview.utils.MedicalReviewDefinedParams
import com.medtroniclabs.opensource.ui.member.MemberRegistrationFragment
import com.medtroniclabs.opensource.ui.phuwalkins.fragment.PhuLinkedHouseHoldListFragment
import com.medtroniclabs.opensource.ui.phuwalkins.fragment.PhuWalkInsListFragment
import com.medtroniclabs.opensource.ui.phuwalkins.listener.PhuLinkCallback
import com.medtroniclabs.opensource.ui.phuwalkins.viewmodel.PhuWalkInsViewModel

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
                tag = MemberRegistrationFragment::class.simpleName
            )
        } else {
            setTitle(getString(R.string.phu_walk_ins_title))
            val phuWalkInsListFragment =
                PhuWalkInsListFragment.newInstance()
            phuWalkInsListFragment.setDataCallback(this@PhuWalkInsActivity)
            addReplaceFragment(R.id.phuListFragment, phuWalkInsListFragment)
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
            }, callbackHome = {
                backNavigationToHome()
            }

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
                isNegativeButtonNeed = true
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

    private fun addReplaceFragment(containerId: Int, fragment: Fragment) {
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
            setTitle(getString(R.string.link_patient))
            viewModel.memberID = patientLinkedDetails.lMemberId.toLong()
            viewModel.fhirMemberID = patientLinkedDetails.memberId.toLong()
            val phuLinkedHouseHoldListFragment =
                PhuLinkedHouseHoldListFragment.newInstance(patientLinkedDetails)
            addReplaceFragment(R.id.phuListFragment, phuLinkedHouseHoldListFragment)
        } else if (patientLinkedDetails is HouseHoldEntityWithMemberCount) {
            val intent = Intent(this, HouseholdSummaryActivity::class.java)
            intent.putExtra(
                HouseholdDefinedParams.ID,
                patientLinkedDetails.id
            )
            intent.putExtra(
                DefinedParams.MemberID,
                viewModel.memberID
            )
            intent.putExtra(
                DefinedParams.FhirMemberID,
                viewModel.fhirMemberID
            )
            intent.putExtra(HouseholdDefinedParams.isFromHouseHoldRegistration, false)
            intent.putExtra(HouseholdDefinedParams.isPhuWalkInsFlow, true)
            startActivity(intent)
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
            DefinedParams.houseHoldLinkStartTiming,
            System.currentTimeMillis()
        )
        val dialIntent = Intent(Intent.ACTION_DIAL)
        dialIntent.data = Uri.parse("tel:${patientLinkedDetails.phoneNumber}")
        dialerLauncher.launch(dialIntent)
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
    private  fun homeScreenClick(){
        val intent = Intent(this, LandingActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        startActivity(intent)
        finish()
    }

}